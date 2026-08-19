package org.xyplugin.xysoulspace.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.util.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public final class AutoPickupListener implements Listener {
    private static final int MAX_PENDING_ITEM_TYPES = 32;
    private static final long MAX_OWNER_PROTECTION_TICKS = 200L;
    private static final long CONSUMED_DROP_RETENTION_TICKS = 40L;
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;
    private final DropOwnershipTracker<Item> dropOwnership = new DropOwnershipTracker<>();
    private final Map<UUID, LinkedHashMap<String, PendingNotification>> pendingNotifications = new HashMap<>();
    private final Set<UUID> pendingGuiRefreshes = new HashSet<>();
    /**
     * Paper 1.12.2 may dispatch both pickup event variants for one player pickup.
     * Keep a short-lived claim so the second event cannot deliver the same entity again.
     */
    private final Map<UUID, Long> consumedDrops = new HashMap<>();
    private int ownedDropTaskId = -1;
    private long pickupClockTick;
    private long mobDropDelayTicks = 10L;
    private int maxOwnedPickupsPerTick = 32;

    public AutoPickupListener(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void startTask() {
        restartTask();
    }

    public void restartTask() {
        cancelTasks();
        mobDropDelayTicks = Math.max(1L, Math.min(200L,
                plugin.getConfig().getLong("pickup.mob-drop-delay-ticks", 10L)));
        maxOwnedPickupsPerTick = Math.max(1, Math.min(512,
                plugin.getConfig().getInt("pickup.max-owned-pickups-per-tick", 32)));
        ownedDropTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickOwnedDrops,
                1L, 1L).getTaskId();
    }

    public void stopTask() {
        cancelTasks();
        for (DropOwnershipTracker.OwnedDrop<Item> owned : dropOwnership.clear()) {
            Item item = owned.getItem();
            if (item != null && !item.isDead() && item.isValid()) item.setPickupDelay(0);
        }
        pendingNotifications.clear();
        pendingGuiRefreshes.clear();
        consumedDrops.clear();
    }

    private void cancelTasks() {
        if (ownedDropTaskId != -1) Bukkit.getScheduler().cancelTask(ownedDropTaskId);
        ownedDropTaskId = -1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        DropOwnershipTracker.OwnedDrop<Item> source = dropOwnership.get(event.getEntity().getUniqueId());
        DropOwnershipTracker.OwnedDrop<Item> target = dropOwnership.get(event.getTarget().getUniqueId());
        if (source == null && target == null) return;
        if (source != null && target != null && source.getOwnerId().equals(target.getOwnerId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (dropOwnership.isOwned(event.getItem().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            if (handlePlayerPickup((Player) event.getEntity(), event.getItem())) {
                event.setCancelled(true);
            }
            return;
        }
        if (dropOwnership.isOwned(event.getItem().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        dropOwnership.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (handlePlayerPickup(event.getPlayer(), event.getItem())) event.setCancelled(true);
    }

    private boolean handlePlayerPickup(Player player, Item item) {
        if (player == null || item == null) return false;
        UUID itemId = item.getUniqueId();
        if (consumedDrops.containsKey(itemId)) return true;

        DropOwnershipTracker.OwnedDrop<Item> owned = dropOwnership.get(itemId);
        if (owned == null) return false;
        if (!owned.getOwnerId().equals(player.getUniqueId())) {
            Player owner = Bukkit.getPlayer(owned.getOwnerId());
            if (owner != null && canAutoPickup(owner)) return true;
            dropOwnership.remove(itemId);
            return false;
        }
        if (!canAutoPickup(player)) return false;
        return depositGroundItem(player, item);
    }

    public boolean isGloballyEnabled() {
        return plugin.getConfig().getBoolean("pickup.global-enabled", true);
    }

    public boolean isPlayerEnabled(Player player) {
        return player != null && service.getStorage(player.getUniqueId()).isPickupEnabled();
    }

    public boolean canAutoPickup(Player player) {
        return player != null
                && hasUsePermission(player)
                && isGloballyEnabled()
                && isPlayerEnabled(player);
    }

    public boolean toggle(Player player) {
        if (player == null || !hasUsePermission(player)) return false;
        if (!isGloballyEnabled()) {
            Text.send(player, plugin.getConfig(), "pickup-global-disabled");
            return false;
        }
        setPlayerEnabled(player, !isPlayerEnabled(player));
        return true;
    }

    private boolean hasUsePermission(Player player) {
        return player.isOp() || player.hasPermission("xysoulspace.use");
    }

    public void setPlayerEnabled(Player player, boolean enabled) {
        if (player == null) return;
        SoulStorage storage = service.getStorage(player.getUniqueId());
        storage.setPickupEnabled(enabled);
        service.save(player.getUniqueId());
        Text.send(player, plugin.getConfig(), enabled ? "pickup-on" : "pickup-off");
    }

    public boolean depositDirect(Player player, ItemStack stack) {
        if (!canAutoPickup(player) || !validStack(stack)) return false;
        ItemStack stored = stack.clone();
        if (!service.deposit(player, stored, "pickup", false)) return false;
        afterSuccessfulDeposit(player, stored);
        return true;
    }

    public boolean spawnOwnedDrop(Player owner, Location location, ItemStack stack) {
        if (!canAutoPickup(owner) || location == null || location.getWorld() == null || !validStack(stack)) {
            return false;
        }
        Item item;
        try {
            item = location.getWorld().dropItemNaturally(location, stack.clone());
        } catch (RuntimeException failure) {
            return false;
        }
        // The spawn event may be cancelled or consumed by another drop plugin. The attempt is
        // still handled; retrying would duplicate drops for plugins that merge during spawn.
        if (item == null || item.isDead() || !item.isValid()) return true;
        long delay = mobDropDelayTicks();
        item.setPickupDelay((int) delay);
        dropOwnership.register(item.getUniqueId(), owner.getUniqueId(), item,
                pickupClockTick + delay,
                pickupClockTick + Math.max(delay, MAX_OWNER_PROTECTION_TICKS));
        return true;
    }

    private void tickOwnedDrops() {
        pickupClockTick++;
        pruneConsumedDrops();
        DropOwnershipTracker.OwnedDrop<Item> owned;
        int remainingBudget = maxOwnedPickupsPerTick();
        while (remainingBudget-- > 0
                && (owned = dropOwnership.pollDue(pickupClockTick)) != null) {
            Item item = owned.getItem();
            if (item == null || item.isDead() || !item.isValid() || !validStack(item.getItemStack())) {
                dropOwnership.remove(owned.getItemId());
                continue;
            }
            Player owner = Bukkit.getPlayer(owned.getOwnerId());
            if (owner == null || !canAutoPickup(owner)) {
                dropOwnership.remove(owned.getItemId());
                continue;
            }
            if (item.getPickupDelay() > 0) {
                if (shouldReleaseForExtendedPickupDelay(
                        pickupClockTick, owned.getExpiresTick(), item.getPickupDelay())) {
                    dropOwnership.remove(owned.getItemId());
                    continue;
                }
                dropOwnership.reschedule(owned, pickupClockTick + 1L);
                continue;
            }
            if (!depositGroundItem(owner, item)) {
                dropOwnership.remove(owned.getItemId());
            }
        }
        flushGuiRefreshes();
        flushReadyNotifications(System.currentTimeMillis());
    }

    private boolean depositGroundItem(Player player, Item item) {
        if (!canAutoPickup(player) || item == null || item.isDead() || !item.isValid()) return false;
        UUID itemId = item.getUniqueId();
        if (consumedDrops.containsKey(itemId)) return false;
        DropOwnershipTracker.OwnedDrop<Item> owned = dropOwnership.get(itemId);
        if (owned != null && !owned.getOwnerId().equals(player.getUniqueId())) return false;
        ItemStack stack = item.getItemStack();
        if (!validStack(stack)) return false;
        ItemStack stored = stack.clone();
        if (!service.deposit(player, stored, "pickup", false)) return false;
        consumedDrops.put(itemId, pickupClockTick);
        dropOwnership.remove(itemId);
        item.remove();
        afterSuccessfulDeposit(player, stored);
        return true;
    }

    private void pruneConsumedDrops() {
        if (consumedDrops.isEmpty()) return;
        long expireBefore = pickupClockTick - CONSUMED_DROP_RETENTION_TICKS;
        Iterator<Map.Entry<UUID, Long>> entries = consumedDrops.entrySet().iterator();
        while (entries.hasNext()) {
            Long consumedAt = entries.next().getValue();
            if (consumedAt == null || consumedAt < expireBefore) entries.remove();
        }
    }

    private void afterSuccessfulDeposit(Player player, ItemStack stack) {
        pendingGuiRefreshes.add(player.getUniqueId());
        queueNotification(player, stack);
    }

    private void queueNotification(Player player, ItemStack stack) {
        if (!plugin.getConfig().getBoolean("pickup.notification-enabled", true)) return;
        String itemName = safeChatValue(plugin.itemDisplayName(stack));
        String itemId = safeChatValue(plugin.itemId(stack));
        long mergeTicks = Math.max(0L, Math.min(200L,
                plugin.getConfig().getLong("pickup.notification-merge-ticks", 10L)));
        if (mergeTicks == 0L) {
            sendNotification(player, new PendingNotification(itemName, itemId, stack.getAmount(), 0L));
            return;
        }

        UUID playerId = player.getUniqueId();
        LinkedHashMap<String, PendingNotification> playerPending = pendingNotifications.get(playerId);
        if (playerPending == null) {
            playerPending = new LinkedHashMap<>();
            pendingNotifications.put(playerId, playerPending);
        }
        String key = itemId + '\u0000' + itemName;
        PendingNotification pending = playerPending.get(key);
        if (pending == null) {
            if (playerPending.size() >= MAX_PENDING_ITEM_TYPES) {
                flushPlayerNotifications(playerId, playerPending);
                playerPending.clear();
            }
            long deadline = System.currentTimeMillis() + mergeTicks * 50L;
            playerPending.put(key, new PendingNotification(itemName, itemId, stack.getAmount(), deadline));
        } else {
            pending.amount = safeAdd(pending.amount, stack.getAmount());
        }
    }

    private void flushReadyNotifications(long now) {
        Iterator<Map.Entry<UUID, LinkedHashMap<String, PendingNotification>>> players =
                pendingNotifications.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, LinkedHashMap<String, PendingNotification>> playerEntry = players.next();
            Player player = Bukkit.getPlayer(playerEntry.getKey());
            if (player == null || !player.isOnline()) {
                players.remove();
                continue;
            }
            Iterator<PendingNotification> notifications = playerEntry.getValue().values().iterator();
            while (notifications.hasNext()) {
                PendingNotification pending = notifications.next();
                if (pending.deadlineMillis > now) continue;
                sendNotification(player, pending);
                notifications.remove();
            }
            if (playerEntry.getValue().isEmpty()) players.remove();
        }
    }

    private void flushPlayerNotifications(UUID playerId,
                                          LinkedHashMap<String, PendingNotification> notifications) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        for (PendingNotification pending : notifications.values()) sendNotification(player, pending);
    }

    private void sendNotification(Player player, PendingNotification pending) {
        String message = plugin.getConfig().getString("pickup.notification-message",
                "&a已自动拾取 &r%item% &7x%amount% &a进入灵魂仓库");
        Text.sendRaw(player, plugin.getConfig(), message,
                "%item%", pending.itemName,
                "%item_name%", pending.itemName,
                "%item_id%", pending.itemId,
                "%amount%", String.valueOf(pending.amount));
    }

    private void flushGuiRefreshes() {
        if (pendingGuiRefreshes.isEmpty()) return;
        for (UUID playerId : new HashSet<>(pendingGuiRefreshes)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && plugin.getGui() != null) {
                plugin.getGui().refreshIfOpen(player);
            }
        }
        pendingGuiRefreshes.clear();
    }

    private boolean validStack(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0;
    }

    private long mobDropDelayTicks() {
        return mobDropDelayTicks;
    }

    private int maxOwnedPickupsPerTick() {
        return maxOwnedPickupsPerTick;
    }

    static boolean shouldReleaseForExtendedPickupDelay(long currentTick,
                                                        long expiresTick,
                                                        int pickupDelay) {
        return pickupDelay > 0 && currentTick > expiresTick;
    }

    private long safeAdd(long left, long right) {
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private String safeChatValue(String value) {
        if (value == null || value.isEmpty()) return "未知物品";
        StringBuilder safe = new StringBuilder(Math.min(80, value.length()));
        for (int index = 0; index < value.length() && safe.length() < 80; index++) {
            char current = value.charAt(index);
            if (current == '\r' || current == '\n' || Character.isISOControl(current)) continue;
            safe.append(current);
        }
        return safe.length() == 0 ? "未知物品" : safe.toString();
    }

    private static final class PendingNotification {
        private final String itemName;
        private final String itemId;
        private long amount;
        private final long deadlineMillis;

        private PendingNotification(String itemName, String itemId, long amount, long deadlineMillis) {
            this.itemName = itemName;
            this.itemId = itemId;
            this.amount = Math.max(0L, amount);
            this.deadlineMillis = deadlineMillis;
        }
    }
}
