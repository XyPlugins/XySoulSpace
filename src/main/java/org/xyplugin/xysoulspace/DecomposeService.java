package org.xyplugin.xysoulspace;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.data.SoulItemRecord;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DecomposeService {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;

    public DecomposeService(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void decompose(Player player) {
        if (!plugin.getConfig().getBoolean("quick-decompose.enabled", true)) return;
        SoulStorage storage = service.getStorage(player.getUniqueId());
        int count = 0;
        for (Map.Entry<String, SoulItemRecord> entry : new ArrayList<>(storage.entriesSnapshot())) {
            List<String> commands = matchCommands(entry.getValue().getItem());
            if (commands.isEmpty()) continue;
            long removed = storage.withdraw(entry.getKey(), entry.getValue().getAmount());
            if (removed <= 0L) continue;
            count += removed;
            for (long i = 0; i < removed; i++) {
                for (String command : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                }
            }
        }
        service.save(player.getUniqueId());
        Text.sendRaw(player, plugin.getConfig(), "&a已分解 " + count + " 个物品。");
    }

    private List<String> matchCommands(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return result;
        List<String> lore = item.getItemMeta().getLore();
        List<Map<?, ?>> rules = plugin.getConfig().getMapList("quick-decompose.items");
        for (Map<?, ?> rule : rules) {
            Object loreRule = rule.get("lore");
            if (!matches(lore, loreRule)) continue;
            Object commands = rule.containsKey("commands") ? rule.get("commands") : rule.get("command");
            if (commands instanceof List) {
                for (Object command : (List<?>) commands) result.add(String.valueOf(command));
            } else if (commands != null) {
                result.add(String.valueOf(commands));
            }
            break;
        }
        return result;
    }

    private boolean matches(List<String> lore, Object rule) {
        if (rule instanceof List) {
            for (Object value : (List<?>) rule) {
                if (matches(lore, value)) return true;
            }
            return false;
        }
        String needle = Text.stripColor(String.valueOf(rule));
        for (String line : lore) {
            if (Text.stripColor(line).contains(needle)) return true;
        }
        return false;
    }
}
