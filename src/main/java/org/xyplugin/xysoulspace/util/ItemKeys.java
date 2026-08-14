package org.xyplugin.xysoulspace.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ItemKeys {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ItemKeys() {
    }

    public static String keyOf(ItemStack item) {
        if (item == null) return "";
        ItemStack normalized = cleanInternalLore(item);
        normalized.setAmount(1);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", normalized);
        return sha256(yaml.saveToString());
    }

    public static ItemStack cleanInternalLore(ItemStack item) {
        if (item == null) return null;
        ItemStack copy = item.clone();
        ItemMeta meta;
        try {
            if (!copy.hasItemMeta()) return copy;
            meta = copy.getItemMeta();
        } catch (RuntimeException failure) {
            return copy;
        }
        if (meta == null || !meta.hasLore()) return copy;
        List<String> cleaned = new ArrayList<String>();
        boolean changed = false;
        for (String line : meta.getLore()) {
            if (isInternalGuiLore(line)) {
                changed = true;
                continue;
            }
            cleaned.add(line);
        }
        if (!changed) return copy;
        meta.setLore(cleaned.isEmpty() ? null : cleaned);
        copy.setItemMeta(meta);
        return copy;
    }

    private static boolean isInternalGuiLore(String line) {
        String plain = Text.stripColor(line).trim();
        return plain.startsWith("灵魂数量:")
                || plain.startsWith("Key: ")
                || (plain.contains("左键取") && plain.contains("右键取"));
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(HEX[(b >>> 4) & 0x0F]);
                builder.append(HEX[b & 0x0F]);
            }
            return builder.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
