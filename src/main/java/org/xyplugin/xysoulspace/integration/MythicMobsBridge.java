package org.xyplugin.xysoulspace.integration;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.util.Text;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class MythicMobsBridge {
    private final XySoulSpacePlugin plugin;
    private final Random random = new Random();
    private boolean registered;

    public MythicMobsBridge(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void registerIfAvailable() {
        if (registered || !plugin.getConfig().getBoolean("integrations.mythicmobs.enabled", true)) return;
        try {
            Class<?> rawEventClass = Class.forName("io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent");
            Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            Listener listener = new Listener() {};
            EventExecutor executor = (ignored, event) -> handleDeath(event);
            Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, executor, plugin);
            registered = true;
            plugin.getLogger().info("已注册 MythicMobs 灵魂掉落桥接。");
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("未检测到兼容的 MythicMobs 死亡事件，跳过 ssdrops 桥接。");
        } catch (Exception failure) {
            plugin.getLogger().warning("注册 MythicMobs 桥接失败: " + failure.getMessage());
        }
    }

    private void handleDeath(Event event) {
        Player killer = readKiller(event);
        if (killer == null || !killer.hasPermission("xysoulspace.use")) return;
        String mobId = readMobId(event);
        if (mobId.isEmpty()) return;
        for (DropRule rule : loadRules(mobId)) {
            if (random.nextDouble() > rule.chance) continue;
            ItemStack item = plugin.getItemLibrary().get(rule.itemId);
            if (item == null) item = generateMythicItem(rule.itemId, rule.amount);
            if (item == null) continue;
            item.setAmount(rule.amount);
            plugin.getService().deposit(killer, item, "mythicmobs");
            sendMessage(killer, item);
        }
    }

    private Player readKiller(Event event) {
        Object value = invoke(event, "getKiller");
        if (value instanceof Player) return (Player) value;
        Object entity = invoke(event, "getEntity");
        value = invoke(entity, "getKiller");
        return value instanceof Player ? (Player) value : null;
    }

    private String readMobId(Event event) {
        Object mob = invoke(event, "getMob");
        Object type = invoke(mob, "getType");
        Object id = invoke(type, "getInternalName");
        if (id == null) id = invoke(type, "getId");
        if (id == null) id = invoke(mob, "getInternalName");
        return id == null ? "" : String.valueOf(id);
    }

    private List<DropRule> loadRules(String mobId) {
        List<DropRule> rules = new ArrayList<>();
        File mobs = new File("plugins/MythicMobs/Mobs");
        collectRules(mobs, mobId, rules);
        return rules;
    }

    private void collectRules(File file, String mobId, List<DropRule> rules) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File child : files) collectRules(child, mobId, rules);
            return;
        }
        if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".yml")) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection(mobId);
        if (section == null) return;
        for (String path : new String[]{"ssdrops", "SsDrops", "soulspace-drops", "SoulSpaceDrops"}) {
            for (String line : section.getStringList(path)) {
                DropRule rule = DropRule.parse(line);
                if (rule != null) rules.add(rule);
            }
        }
    }

    private ItemStack generateMythicItem(String itemId, int amount) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object instance = mythicMobsClass.getMethod("inst").invoke(null);
            Object manager = instance.getClass().getMethod("getItemManager").invoke(instance);
            Object itemResult = manager.getClass().getMethod("getItem", String.class).invoke(manager, itemId);
            Object mythicItem = unwrapOptional(itemResult);
            if (mythicItem == null) return null;
            Object generated = mythicItem.getClass().getMethod("generateItemStack", int.class).invoke(mythicItem, amount);
            if (generated instanceof ItemStack) return (ItemStack) generated;
            Class<?> adapter = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            for (Method method : adapter.getMethods()) {
                if (!"adapt".equals(method.getName()) || method.getParameterTypes().length != 1) continue;
                try {
                    Object adapted = method.invoke(null, generated);
                    if (adapted instanceof ItemStack) return (ItemStack) adapted;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Object unwrapOptional(Object value) throws Exception {
        if (value == null) return null;
        if ("java.util.Optional".equals(value.getClass().getName())) {
            Boolean present = (Boolean) value.getClass().getMethod("isPresent").invoke(value);
            return present ? value.getClass().getMethod("get").invoke(value) : null;
        }
        return value;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sendMessage(Player player, ItemStack item) {
        String template = plugin.getConfig().getString("integrations.mythicmobs.pickup-message", "");
        if (template == null || template.isEmpty()) return;
        Text.sendRaw(player, plugin.getConfig(), template,
                "%amount%", String.valueOf(item.getAmount()),
                "%item%", Text.itemName(item));
    }

    private static final class DropRule {
        private final String itemId;
        private final int amount;
        private final double chance;

        private DropRule(String itemId, int amount, double chance) {
            this.itemId = itemId;
            this.amount = Math.max(1, amount);
            this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        }

        private static DropRule parse(String line) {
            if (line == null || line.trim().isEmpty()) return null;
            String[] parts = line.trim().split("[\\s,;]+");
            String id = parts[0];
            int amount = parts.length > 1 ? parseInt(parts[1], 1) : 1;
            double chance = parts.length > 2 ? parseChance(parts[2]) : 1.0D;
            return new DropRule(id, amount, chance);
        }

        private static int parseInt(String value, int fallback) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static double parseChance(String value) {
            try {
                String normalized = value.endsWith("%") ? value.substring(0, value.length() - 1) : value;
                double parsed = Double.parseDouble(normalized);
                return value.endsWith("%") ? parsed / 100.0D : parsed;
            } catch (NumberFormatException ignored) {
                return 1.0D;
            }
        }
    }
}
