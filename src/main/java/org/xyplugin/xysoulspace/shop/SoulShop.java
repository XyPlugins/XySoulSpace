package org.xyplugin.xysoulspace.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.util.Inventorys;
import org.xyplugin.xysoulspace.util.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SoulShop {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;
    private final Map<String, java.util.List<ShopItem>> shops = new LinkedHashMap<>();
    private final Map<String, Integer> sizes = new LinkedHashMap<>();

    public SoulShop(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
        reload();
    }

    public void reload() {
        shops.clear();
        sizes.clear();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) plugin.saveResource("shop.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("shops");
        if (root == null) return;
        for (String shopName : root.getKeys(false)) {
            ConfigurationSection shop = root.getConfigurationSection(shopName);
            if (shop == null) continue;
            sizes.put(shopName, normalizeSize(shop.getInt("size", 54)));
            ArrayList<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemSection = shop.getConfigurationSection("items");
            if (itemSection != null) {
                for (String id : itemSection.getKeys(false)) {
                    items.add(new ShopItem(id, itemSection.getConfigurationSection(id)));
                }
            }
            shops.put(shopName, items);
        }
    }

    public java.util.Set<String> getShopNames() {
        return shops.keySet();
    }

    public void open(Player player, String shopName) {
        if (!shops.containsKey(shopName)) shopName = shops.keySet().isEmpty() ? "默认" : shops.keySet().iterator().next();
        Inventory inventory = Bukkit.createInventory(null, sizes.getOrDefault(shopName, 54), Text.color("&a灵魂商店[" + shopName + "]"));
        for (ShopItem item : shops.getOrDefault(shopName, new ArrayList<>())) {
            int slot = item.getSlot();
            if (slot < 0 || slot >= inventory.getSize()) slot = inventory.firstEmpty();
            if (slot < 0) continue;
            inventory.setItem(slot, display(player, item));
        }
        player.openInventory(inventory);
    }

    public boolean buy(Player player, String shopName, int slot, int multiplier) {
        java.util.List<ShopItem> items = shops.get(shopName);
        if (items == null) return false;
        ShopItem selected = null;
        for (ShopItem item : items) {
            if (item.getSlot() == slot) {
                selected = item;
                break;
            }
        }
        if (selected == null && slot >= 0 && slot < items.size()) selected = items.get(slot);
        if (selected == null) return false;

        for (Map.Entry<String, Long> cost : selected.getCosts().entrySet()) {
            long needed = cost.getValue() * multiplier;
            if (service.getAmountByCostKey(player.getUniqueId(), cost.getKey()) < needed) {
                Text.sendRaw(player, plugin.getConfig(), "&c灵魂空间材料不足: " + cost.getKey() + " x" + needed);
                return false;
            }
        }
        for (Map.Entry<String, Long> cost : selected.getCosts().entrySet()) {
            service.removeByCostKey(player.getUniqueId(), cost.getKey(), cost.getValue() * multiplier);
        }
        ItemStack result = selected.getResult();
        long accepted = Inventorys.addItems(player.getInventory(), result, (long) result.getAmount() * multiplier);
        service.save(player.getUniqueId());
        Text.sendRaw(player, plugin.getConfig(), "&a购买成功，获得 " + accepted + " 个 " + Text.itemName(result));
        return true;
    }

    private ItemStack display(Player player, ShopItem item) {
        ItemStack display = item.getResult();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            java.util.List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(Text.color("&7消耗:"));
            for (Map.Entry<String, Long> cost : item.getCosts().entrySet()) {
                long owned = service.getAmountByCostKey(player.getUniqueId(), cost.getKey());
                lore.add(Text.color("&8- &f" + cost.getKey() + " &7x" + cost.getValue() + " &8(拥有 " + owned + ")"));
            }
            lore.add(Text.color("&e左键购买 1 次，右键购买 64 次"));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        return normalized % 9 == 0 ? normalized : ((normalized / 9) + 1) * 9;
    }
}
