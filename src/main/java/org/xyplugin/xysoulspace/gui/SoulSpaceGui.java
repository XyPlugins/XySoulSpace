package org.xyplugin.xysoulspace.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.data.SoulItemRecord;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.util.Inventorys;
import org.xyplugin.xysoulspace.util.Items;
import org.xyplugin.xysoulspace.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SoulSpaceGui implements Listener {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, UUID> viewing = new HashMap<>();

    public SoulSpaceGui(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer, Player owner, int page, boolean admin) {
        pages.put(viewer.getUniqueId(), Math.max(0, page));
        viewing.put(viewer.getUniqueId(), owner.getUniqueId());
        String title = admin
                ? Text.color(plugin.getConfig().getString("gui.admin-title", "&6[管理] %player% 的灵魂空间")
                .replace("%player%", owner.getName()))
                : Text.color(plugin.getConfig().getString("gui.title", "&a灵魂空间"));
        Inventory inventory = Bukkit.createInventory(null, 54, title + " - " + (Math.max(0, page) + 1));
        populate(inventory, viewer, owner.getUniqueId(), Math.max(0, page));
        viewer.openInventory(inventory);
    }

    public void refreshIfOpen(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null || !isSoulTitle(top.getTitle())) return;
        UUID owner = viewing.getOrDefault(player.getUniqueId(), player.getUniqueId());
        populate(top, player, owner, pages.getOrDefault(player.getUniqueId(), 0));
    }

    private void populate(Inventory inventory, Player viewer, UUID owner, int page) {
        inventory.clear();
        SoulStorage storage = service.getStorage(owner);
        Collection<Map.Entry<String, SoulItemRecord>> entries = storage.entriesSnapshot();
        ArrayList<Map.Entry<String, SoulItemRecord>> list = new ArrayList<>(entries);
        int perPage = Math.max(9, Math.min(45, plugin.getConfig().getInt("gui.items-per-page", 45)));
        int start = page * perPage;
        int slot = 0;
        for (int i = start; i < Math.min(list.size(), start + perPage); i++) {
            Map.Entry<String, SoulItemRecord> entry = list.get(i);
            ItemStack display = entry.getValue().getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                java.util.List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Text.color("&7灵魂数量: &f" + entry.getValue().getAmount()));
                lore.add(Text.color("&8Key: " + entry.getKey()));
                lore.add(Text.color("&7左键取 1，右键取 64，Shift 左键取全部"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        if (page > 0) inventory.setItem(45, Items.named(navMaterial("previous-page-material", Material.FEATHER), "&e上一页"));
        inventory.setItem(49, Items.named(navMaterial("store-button-material", Material.EMERALD), "&2一键存入", "&7将背包普通物品存入灵魂空间"));
        inventory.setItem(50, Items.named(navMaterial("decompose-button-material", Material.BLAZE_POWDER), "&6快捷分解", "&7根据配置匹配 Lore 并执行命令"));
        inventory.setItem(48, Items.named(navMaterial("close-button-material", Material.BARRIER), "&c关闭"));
        if (start + perPage < list.size()) inventory.setItem(53, Items.named(navMaterial("next-page-material", Material.FEATHER), "&e下一页"));
    }

    private Material navMaterial(String path, Material fallback) {
        return Items.material(plugin.getConfig().getString("gui." + path), fallback);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null || !isSoulTitle(top.getTitle())) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != top) return;
        Player player = (Player) event.getWhoClicked();
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
        if (slot == 49) {
            quickStore(player);
            return;
        }
        if (slot == 50) {
            plugin.getDecomposeService().decompose(player);
            refreshIfOpen(player);
            return;
        }
        withdraw(player, clicked, event.getClick());
    }

    private void changePage(Player player, int delta) {
        int next = Math.max(0, pages.getOrDefault(player.getUniqueId(), 0) + delta);
        UUID owner = viewing.getOrDefault(player.getUniqueId(), player.getUniqueId());
        Player ownerPlayer = Bukkit.getPlayer(owner);
        open(player, ownerPlayer == null ? player : ownerPlayer, next, !owner.equals(player.getUniqueId()));
    }

    private void withdraw(Player player, ItemStack clicked, ClickType click) {
        String key = readKey(clicked);
        if (key.isEmpty()) return;
        UUID owner = viewing.getOrDefault(player.getUniqueId(), player.getUniqueId());
        SoulStorage storage = service.getStorage(owner);
        long available = storage.getAmount(key);
        if (available <= 0L) return;
        long requested = click == ClickType.SHIFT_LEFT ? available : click.isRightClick() ? 64L : 1L;
        long accepted = Inventorys.addItems(player.getInventory(), clicked, Math.min(available, requested));
        if (accepted <= 0L) {
            Text.send(player, plugin.getConfig(), "inventory-full");
            return;
        }
        storage.withdraw(key, accepted);
        service.save(owner);
        Text.send(player, plugin.getConfig(), "withdrawn", "%amount%", String.valueOf(accepted), "%item%", Text.itemName(clicked));
        refreshIfOpen(player);
    }

    private void quickStore(Player player) {
        SoulStorage storage = service.getStorage(player.getUniqueId());
        int stored = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            storage.deposit(item);
            stored += item.getAmount();
            contents[i] = null;
        }
        player.getInventory().setContents(contents);
        service.save(player.getUniqueId());
        Text.send(player, plugin.getConfig(), "stored", "%amount%", String.valueOf(stored), "%item%", "物品");
        refreshIfOpen(player);
    }

    private String readKey(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return "";
        for (String line : item.getItemMeta().getLore()) {
            String plain = Text.stripColor(line);
            if (plain.startsWith("Key: ")) return plain.substring("Key: ".length()).trim();
        }
        return "";
    }

    private boolean isSoulTitle(String title) {
        String plain = Text.stripColor(title);
        return plain.startsWith("灵魂空间") || plain.startsWith("[管理]");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!isSoulTitle(event.getInventory().getTitle())) return;
        Player player = (Player) event.getPlayer();
        pages.remove(player.getUniqueId());
        viewing.remove(player.getUniqueId());
    }
}
