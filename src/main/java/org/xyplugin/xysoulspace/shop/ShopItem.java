package org.xyplugin.xysoulspace.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.xyplugin.xysoulspace.util.Items;
import org.xyplugin.xysoulspace.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopItem {
    private final String id;
    private final ItemStack result;
    private final Map<String, Long> costs;
    private final int slot;

    public ShopItem(String id, ConfigurationSection section) {
        this.id = id;
        Material material = Items.material(section.getString("material", "STONE"), Material.STONE);
        int amount = Math.max(1, section.getInt("amount", 1));
        this.result = new ItemStack(material, amount);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(section.getString("name", id)));
            ArrayList<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) lore.add(Text.color(line));
            meta.setLore(lore);
            result.setItemMeta(meta);
        }
        this.costs = new LinkedHashMap<>();
        ConfigurationSection costSection = section.getConfigurationSection("costs");
        if (costSection != null) {
            for (String key : costSection.getKeys(false)) {
                costs.put(key, costSection.getLong(key, 0L));
            }
        }
        this.slot = section.getInt("slot", -1);
    }

    public String getId() {
        return id;
    }

    public ItemStack getResult() {
        return result.clone();
    }

    public Map<String, Long> getCosts() {
        return costs;
    }

    public int getSlot() {
        return slot;
    }
}
