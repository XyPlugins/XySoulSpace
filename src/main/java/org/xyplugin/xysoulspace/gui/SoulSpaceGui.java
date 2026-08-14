package org.xyplugin.xysoulspace.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.data.SoulItemRecord;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.listener.AutoPickupListener;
import org.xyplugin.xysoulspace.util.Inventorys;
import org.xyplugin.xysoulspace.util.ItemKeys;
import org.xyplugin.xysoulspace.util.Items;
import org.xyplugin.xysoulspace.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SoulSpaceGui implements Listener {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;
    private final Set<UUID> pendingInventoryStores = new HashSet<>();

    public SoulSpaceGui(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer, Player owner, int page, boolean admin) {
        int safePage = Math.max(0, page);
        String title = admin
                ? Text.color(plugin.getConfig().getString("gui.admin-title", "&6[管理] %player% 的灵魂空间")
                .replace("%player%", owner.getName()))
                : Text.color(plugin.getConfig().getString("gui.title", "&a灵魂空间"));
        SoulSpaceHolder holder = new SoulSpaceHolder(owner.getUniqueId(), safePage, admin);
        Inventory inventory = Bukkit.createInventory(holder, 54, title + " - " + (safePage + 1));
        holder.attach(inventory);
        populate(inventory, viewer, holder);
        viewer.openInventory(inventory);
    }

    public void refreshIfOpen(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        SoulSpaceHolder holder = soulHolder(top);
        if (holder == null) return;
        populate(top, player, holder);
    }

    private void populate(Inventory inventory, Player viewer, SoulSpaceHolder holder) {
        inventory.clear();
        UUID owner = holder.owner;
        int page = holder.page;
        SoulStorage storage = service.getStorage(owner);
        Collection<Map.Entry<String, SoulItemRecord>> entries = storage.entriesSnapshot();
        ArrayList<Map.Entry<String, SoulItemRecord>> list = new ArrayList<>(entries);
        int perPage = Math.max(9, Math.min(45, plugin.getConfig().getInt("gui.items-per-page", 45)));
        int start = page * perPage;
        int slot = 0;
        Map<Integer, String> slotKeys = new HashMap<>();
        for (int i = start; i < Math.min(list.size(), start + perPage); i++) {
            Map.Entry<String, SoulItemRecord> entry = list.get(i);
            ItemStack display = entry.getValue().getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                java.util.List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                if (plugin.getConfig().getBoolean("gui.show-amount-lore", true)) {
                    lore.add(Text.color("&7灵魂数量: &f" + entry.getValue().getAmount()));
                }
                if (plugin.getConfig().getBoolean("gui.show-key-lore", false)) {
                    lore.add(Text.color("&8Key: " + entry.getKey()));
                }
                if (plugin.getConfig().getBoolean("gui.show-action-lore", false)) {
                    lore.add(Text.color("&7左键取 1，右键取 64，Shift 左键取全部"));
                }
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
            slotKeys.put(slot, entry.getKey());
            slot++;
        }
        holder.visibleKeys = slotKeys;

        if (page > 0) inventory.setItem(45, Items.named(navMaterial("previous-page-material", Material.FEATHER), "&e上一页"));
        inventory.setItem(49, Items.named(navMaterial("store-button-material", Material.EMERALD), "&2一键存入", "&7只存入主背包和快捷栏物品", "&8不会处理装备栏、副手或其它槽位"));
        inventory.setItem(50, Items.named(navMaterial("decompose-button-material", Material.BLAZE_POWDER), "&6快捷分解", "&7根据配置匹配 Lore 并执行命令"));
        inventory.setItem(51, autoPickupButton(viewer, owner));
        inventory.setItem(48, Items.named(navMaterial("close-button-material", Material.BARRIER), "&c关闭"));
        if (start + perPage < list.size()) inventory.setItem(53, Items.named(navMaterial("next-page-material", Material.FEATHER), "&e下一页"));
    }

    private ItemStack autoPickupButton(Player viewer, UUID owner) {
        AutoPickupListener autoPickup = plugin.getAutoPickup();
        boolean readonly = !owner.equals(viewer.getUniqueId());
        if (autoPickup == null || !autoPickup.isGloballyEnabled()) {
            return Items.named(navMaterial("pickup-global-disabled-button-material", Material.BARRIER),
                    "&8自动拾取：全局停用",
                    "&7管理员已关闭全局自动拾取",
                    readonly ? "&8管理员查看时无法切换" : "&8当前无法切换");
        }

        boolean enabled = service.getStorage(owner).isPickupEnabled();
        Material material = enabled
                ? navMaterial("pickup-enabled-button-material", Material.EMERALD)
                : navMaterial("pickup-disabled-button-material", Material.REDSTONE);
        String name = enabled ? "&a自动拾取：已开启" : "&c自动拾取：已关闭";
        String action = readonly
                ? "&8管理员查看时无法切换"
                : enabled ? "&7点击关闭自动拾取" : "&7点击开启自动拾取";
        return Items.named(material, name, action);
    }

    private Material navMaterial(String path, Material fallback) {
        return Items.material(plugin.getConfig().getString("gui." + path), fallback);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        SoulSpaceHolder holder = soulHolder(top);
        if (holder == null) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != top) {
            storeFromPlayerInventory(player, event);
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        if (slot == 48) {
            player.closeInventory();
            return;
        }
        if (slot == 45 && clicked.getType() == navMaterial("previous-page-material", Material.FEATHER)) {
            changePage(player, -1);
            return;
        }
        if (slot == 53 && clicked.getType() == navMaterial("next-page-material", Material.FEATHER)) {
            changePage(player, 1);
            return;
        }
        if (!holder.owner.equals(player.getUniqueId())) return;
        if (slot == 49) {
            quickStore(player);
            return;
        }
        if (slot == 50) {
            plugin.getDecomposeService().decompose(player);
            refreshIfOpen(player);
            return;
        }
        if (slot == 51) {
            toggleAutoPickup(player);
            return;
        }
        String key = holder.visibleKeys.get(slot);
        if (key != null && !hasCursor(event)) withdraw(player, key, event.getClick());
    }

    private void changePage(Player player, int delta) {
        SoulSpaceHolder holder = soulHolder(player.getOpenInventory().getTopInventory());
        if (holder == null) return;
        int next = Math.max(0, holder.page + delta);
        Player ownerPlayer = Bukkit.getPlayer(holder.owner);
        if (ownerPlayer == null) {
            if (!holder.owner.equals(player.getUniqueId())) {
                Text.sendLocal(player, plugin.getConfig(), "player-not-found");
                player.closeInventory();
                return;
            }
            ownerPlayer = player;
        }
        open(player, ownerPlayer, next, holder.admin);
    }

    private void withdraw(Player player, String key, ClickType click) {
        if (key == null || key.isEmpty()) return;
        SoulSpaceHolder holder = soulHolder(player.getOpenInventory().getTopInventory());
        if (holder == null) return;
        UUID owner = holder.owner;
        SoulStorage storage = service.getStorage(owner);
        long available = storage.getAmount(key);
        if (available <= 0L) return;
        ItemStack template = storage.getItem(key);
        if (template == null || template.getType() == Material.AIR) return;
        long requested = withdrawAmount(available, click);
        if (requested <= 0L) return;
        long accepted = Inventorys.addItems(player.getInventory(), template, Math.min(available, requested));
        if (accepted <= 0L) {
            Text.send(player, plugin.getConfig(), "inventory-full");
            return;
        }
        storage.withdraw(key, accepted);
        service.save(owner);
        Text.send(player, plugin.getConfig(), "withdrawn", "%amount%", String.valueOf(accepted), "%item%", plugin.itemDisplayName(template));
        refreshIfOpen(player);
    }

    private void quickStore(Player player) {
        SoulStorage storage = service.getStorage(player.getUniqueId());
        int stored = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            storage.deposit(item);
            stored += item.getAmount();
            player.getInventory().setItem(i, null);
        }
        service.save(player.getUniqueId());
        Text.send(player, plugin.getConfig(), "stored", "%amount%", String.valueOf(stored), "%item%", "物品");
        refreshIfOpen(player);
    }

    private void storeFromPlayerInventory(Player player, InventoryClickEvent event) {
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getBottomInventory()) return;
        SoulSpaceHolder holder = soulHolder(event.getView().getTopInventory());
        if (holder == null || !holder.owner.equals(player.getUniqueId())) return;
        if (hasCursor(event)) return;
        int slot = event.getSlot();
        if (slot < 0 || slot >= 36) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return;
        int amount = storeAmount(item, event.getClick());
        if (amount <= 0) return;

        final UUID playerId = player.getUniqueId();
        final int clickedSlot = slot;
        final ClickType click = event.getClick();
        final ItemStack clickedSnapshot = item.clone();
        if (!pendingInventoryStores.add(playerId)) return;

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    completeStoreFromPlayerInventory(playerId, clickedSlot, click, clickedSnapshot);
                } finally {
                    pendingInventoryStores.remove(playerId);
                }
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isSoulInventory(event.getView().getTopInventory())) event.setCancelled(true);
    }

    private void toggleAutoPickup(Player player) {
        SoulSpaceHolder holder = soulHolder(player.getOpenInventory().getTopInventory());
        if (holder == null || !holder.owner.equals(player.getUniqueId())) return;
        AutoPickupListener autoPickup = plugin.getAutoPickup();
        if (autoPickup == null) return;
        autoPickup.toggle(player);
        refreshIfOpen(player);
    }

    private void completeStoreFromPlayerInventory(UUID playerId, int slot, ClickType click, ItemStack clickedSnapshot) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline() || !isViewingOwnSoulSpace(player)) return;

        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getType() == Material.AIR || current.getAmount() <= 0) return;
        if (clickedSnapshot == null || !current.isSimilar(clickedSnapshot)) return;

        ItemStack storedItem = current.clone();
        SoulStorage storage = service.getStorage(playerId);
        long amount;
        if (click == ClickType.SHIFT_LEFT) {
            amount = storeAllMatching(player, clickedSnapshot, storage);
        } else {
            int stackAmount = Math.min(storeAmount(current, click), current.getAmount());
            if (stackAmount <= 0) return;
            amount = stackAmount;
            storedItem.setAmount(stackAmount);
            storage.depositAmount(storedItem, stackAmount);

            int remaining = current.getAmount() - stackAmount;
            if (remaining <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                ItemStack remainingItem = current.clone();
                remainingItem.setAmount(remaining);
                player.getInventory().setItem(slot, remainingItem);
            }
        }
        if (amount <= 0L) return;

        service.save(playerId);
        Text.send(player, plugin.getConfig(), "stored",
                "%amount%", String.valueOf(amount),
                "%item%", plugin.itemDisplayName(storedItem));
        refreshIfOpen(player);
        player.updateInventory();
    }

    private long storeAllMatching(Player player, ItemStack template, SoulStorage storage) {
        String templateKey = ItemKeys.keyOf(template);
        if (templateKey.isEmpty()) return 0L;

        ArrayList<Integer> matchingSlots = new ArrayList<>();
        long total = 0L;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType() != template.getType() || item.getDurability() != template.getDurability()) continue;
            if (!templateKey.equals(ItemKeys.keyOf(item))) continue;
            matchingSlots.add(slot);
            total += item.getAmount();
        }
        if (total <= 0L) return 0L;

        storage.depositAmount(template, total);
        for (Integer slot : matchingSlots) {
            player.getInventory().setItem(slot, null);
        }
        return total;
    }

    private int storeAmount(ItemStack item, ClickType click) {
        if (click == ClickType.SHIFT_LEFT) return item.getAmount();
        if (click == ClickType.RIGHT) return Math.min(64, item.getAmount());
        if (click == ClickType.LEFT) return Math.min(1, item.getAmount());
        return 0;
    }

    private long withdrawAmount(long available, ClickType click) {
        if (click == ClickType.SHIFT_LEFT) return available;
        if (click == ClickType.RIGHT) return Math.min(64L, available);
        if (click == ClickType.LEFT) return Math.min(1L, available);
        return 0L;
    }

    private boolean hasCursor(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        return cursor != null && cursor.getType() != Material.AIR && cursor.getAmount() > 0;
    }

    private boolean isSoulInventory(Inventory inventory) {
        return soulHolder(inventory) != null;
    }

    private SoulSpaceHolder soulHolder(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof SoulSpaceHolder
                ? (SoulSpaceHolder) inventory.getHolder()
                : null;
    }

    private boolean isViewingOwnSoulSpace(Player player) {
        SoulSpaceHolder holder = soulHolder(player.getOpenInventory().getTopInventory());
        return holder != null && holder.owner.equals(player.getUniqueId());
    }

    private static final class SoulSpaceHolder implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private final boolean admin;
        private Map<Integer, String> visibleKeys = new HashMap<>();
        private Inventory inventory;

        private SoulSpaceHolder(UUID owner, int page, boolean admin) {
            this.owner = owner;
            this.page = page;
            this.admin = admin;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
