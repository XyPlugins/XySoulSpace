package org.xyplugin.xysoulspace.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public final class Text {
    private Text() {
    }

    public static String color(String value) {
        return value == null ? "" : ChatColor.translateAlternateColorCodes('&', value);
    }

    public static String stripColor(String value) {
        return value == null ? "" : ChatColor.stripColor(color(value));
    }

    public static String itemName(ItemStack item) {
        if (item == null) return "";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    public static String costKey(ItemStack item) {
        if (item == null) return "";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String plain = stripColor(item.getItemMeta().getDisplayName()).trim();
            if (!plain.isEmpty()) return plain;
        }
        return item.getType().name();
    }

    public static void send(CommandSender sender, FileConfiguration config, String key, String... replacements) {
        String prefix = color(config.getString("messages.prefix", ""));
        String message = config.getString("messages." + key, key);
        message = replace(message, replacements);
        sender.sendMessage(prefix + color(message));
    }

    public static String replace(String value, String... replacements) {
        String result = value == null ? "" : value;
        if (replacements == null) return result;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return result;
    }
}
