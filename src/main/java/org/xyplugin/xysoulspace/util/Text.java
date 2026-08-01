package org.xyplugin.xysoulspace.util;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

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
        String message = config.getString("messages." + key, key);
        sendRaw(sender, config, message, replacements);
    }

    public static void sendRaw(CommandSender sender, FileConfiguration config, String message, String... replacements) {
        if (sender == null) return;
        sender.sendMessage(color(prefix(config) + replace(message, replacements)));
    }

    public static String prefix(FileConfiguration config) {
        String fallback = config == null ? "" : config.getString("messages.prefix", "");
        String corePrefix = xyCorePrefix();
        return corePrefix == null ? fallback : corePrefix;
    }

    private static String xyCorePrefix() {
        Plugin core = Bukkit.getPluginManager().getPlugin("XyCore");
        if (core == null || !core.isEnabled()) return null;
        try {
            ClassLoader loader = core.getClass().getClassLoader();
            Class<?> entry = Class.forName("org.xyplugin.xycore.api.XyCore", true, loader);
            Object api = entry.getMethod("get").invoke(null);
            Object prefix = api.getClass().getMethod("getMessagePrefix").invoke(api);
            return prefix == null ? "" : String.valueOf(prefix);
        } catch (Exception ignored) {
            return null;
        }
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
