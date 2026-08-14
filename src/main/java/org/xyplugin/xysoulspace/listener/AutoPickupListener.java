package org.xyplugin.xysoulspace.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;
    private final Map<UUID, LinkedHashMap<String, PendingNotification>> pendingNotifications = new HashMap<>();
    private final Set<UUID> pendingGuiRefreshes = new HashSet<>();
    private int taskId = -1;

    public AutoPickupListener(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void startTask() {
        restartTask();
    }

    public void restartTask() {
        stopTask();
        long interval = Math.max(1L, plugin.getConfig().getLong("pickup.scan-interval-ticks", 10L));
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        pendingNotifications.clear();
        pendingGuiRefreshes.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!canAutoPickup(player)) return;
        Item item = event.getItem();
        if (depositGroundItem(player, item)) {
            event.setCancelled(true);
        }
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

    private void tick() {
        scanOnlinePlayers();
        flushGuiRefreshes();
        flushReadyNotifications(System.currentTimeMillis());
    }

    private void scanOnlinePlayers() {
        if (!isGloballyEnabled()) return;
        double range = Math.max(0.5D, Math.min(64.0D,
                plugin.getConfig().getDouble("pickup.range", 6.0D)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canAutoPickup(player)) continue;
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (!(entity instanceof Item)) continue;
                Item item = (Item) entity;
                if (item.isDead() || !item.isValid() || item.getPickupDelay() > 0) continue;
                depositGroundItem(player, item);
            }
        }
    }

    private boolean depositGroundItem(Player player, Item item) {
        if (!canAutoPickup(player) || item == null || item.isDead() || !item.isValid()) return false;
        ItemStack stack = item.getItemStack();
        if (!validStack(stack)) return false;
        ItemStack stored = stack.clone();
        if (!service.deposit(player, stored, "pickup", false)) return false;
        item.remove();
        afterSuccessfulDeposit(player, stored);
        return true;
    }

    private void afterSuccessfulDeposit(Player player, ItemStack stack) {
        pendingGuiRefreshes.add(player.getUniqueId());
        queueNotification(player, stack);
    }

    private void queueNotification(Player player, ItemStack stack) {
        if (!plugin.getConfig().getBoolean("pickup.notification-enabled", true)) return;
        String itemName = safeChatValue(plugin.itemFriendlyName(stack));
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
