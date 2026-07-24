package org.xyplugin.xysoulspace.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {
    }

    public static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            if (lore != null && lore.length > 0) {
                List<String> colored = new ArrayList<>();
                for (String line : Arrays.asList(lore)) colored.add(Text.color(line));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        Material material = Material.getMaterial(name.trim().toUpperCase());
        return material == null ? fallback : material;
    }

    public static String plainName(ItemStack item) {
        return ChatColor.stripColor(Text.itemName(item));
    }
}
