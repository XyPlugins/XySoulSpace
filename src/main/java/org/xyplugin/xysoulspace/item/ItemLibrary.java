package org.xyplugin.xysoulspace.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.util.Text;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ItemLibrary {
    private final XySoulSpacePlugin plugin;
    private final File file;
    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public ItemLibrary(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "itemlibrary/items.yml");
        reload();
    }

    public void reload() {
        items.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ItemStack item = yaml.getItemStack(key);
            if (item != null) items.put(key.toLowerCase(), item);
        }
    }

    public void saveItem(Player player, String id) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getAmount() <= 0 || hand.getType().name().equals("AIR")) {
            Text.send(player, plugin.getConfig(), "item-library-missing", "%id%", id);
            return;
        }
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        yaml.set(id, hand.clone());
        try {
            yaml.save(file);
            items.put(id.toLowerCase(), hand.clone());
            Text.send(player, plugin.getConfig(), "item-library-saved", "%id%", id);
        } catch (IOException failure) {
            player.sendMessage(Text.color("&c保存物品库失败: " + failure.getMessage()));
        }
    }

    public ItemStack get(String id) {
        ItemStack item = items.get(id == null ? "" : id.toLowerCase());
        return item == null ? null : item.clone();
    }

    public Set<String> names() {
        return items.keySet();
    }
}
