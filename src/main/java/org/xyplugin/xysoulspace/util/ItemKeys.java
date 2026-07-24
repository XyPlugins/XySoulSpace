package org.xyplugin.xysoulspace.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ItemKeys {
    private ItemKeys() {
    }

    public static String keyOf(ItemStack item) {
        if (item == null) return "";
        ItemStack normalized = item.clone();
        normalized.setAmount(1);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", normalized);
        return sha256(yaml.saveToString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
