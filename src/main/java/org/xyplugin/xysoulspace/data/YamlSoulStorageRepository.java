package org.xyplugin.xysoulspace.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class YamlSoulStorageRepository {
    private final XySoulSpacePlugin plugin;
    private final File folder;

    public YamlSoulStorageRepository(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "soulspace");
    }

    public synchronized SoulStorage load(UUID uuid) {
        File file = fileOf(uuid);
        SoulStorage storage = new SoulStorage();
        storage.setPickupEnabled(plugin.getConfig().getBoolean("pickup.default-player-enabled", true));
        storage.markClean();
        if (!file.exists()) return storage;

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            storage.setPickupEnabled(yaml.getBoolean("settings.pickup-enabled",
                    plugin.getConfig().getBoolean("pickup.default-player-enabled", true)));
            ConfigurationSection section = yaml.getConfigurationSection("items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ItemStack item = section.getItemStack(key + ".item");
                    long amount = section.getLong(key + ".amount", 0L);
                    if (item != null && amount > 0L) {
                        item.setAmount(1);
                        storage.depositAmount(item, amount);
                    }
                }
            }
        } catch (Exception failure) {
            plugin.getLogger().warning("读取灵魂空间数据失败 " + uuid + ": " + failure.getMessage());
        }
        storage.markClean();
        return storage;
    }

    public synchronized void save(UUID uuid, SoulStorage storage) {
        if (!folder.exists()) folder.mkdirs();
        File file = fileOf(uuid);
        YamlConfiguration yaml = new YamlConfiguration();
        SoulStorage.Snapshot snapshot = storage.snapshot();
        yaml.set("settings.pickup-enabled", snapshot.isPickupEnabled());
        for (Map.Entry<String, SoulItemRecord> entry : snapshot.entries()) {
            String path = "items." + entry.getKey();
            yaml.set(path + ".amount", entry.getValue().getAmount());
            yaml.set(path + ".item", entry.getValue().getItem());
        }
        try {
            yaml.save(file);
            storage.markClean(snapshot.getRevision());
        } catch (IOException failure) {
            plugin.getLogger().warning("保存灵魂空间数据失败 " + uuid + ": " + failure.getMessage());
        }
    }

    private File fileOf(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }
}
