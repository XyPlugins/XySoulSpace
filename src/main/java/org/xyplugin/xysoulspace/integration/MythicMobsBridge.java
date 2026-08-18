package org.xyplugin.xysoulspace.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public final class MythicMobsBridge {
    private final XySoulSpacePlugin plugin;
    private final Random random = new Random();
    private final Set<String> warnedMissingItems = new HashSet<>();
    private volatile Map<String, List<DropRule>> rulesByMob = Collections.emptyMap();
    private boolean registered;

    public MythicMobsBridge(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void registerIfAvailable() {
        if (registered || !plugin.getConfig().getBoolean("integrations.mythicmobs.enabled", true)) return;
        reloadRules();
        registerListener();
    }

    public void reload() {
        reloadRules();
        if (!registered && plugin.getConfig().getBoolean("integrations.mythicmobs.enabled", true)) {
            registerListener();
        }
    }

    @SuppressWarnings("unchecked")
    private void registerListener() {
        if (registered) return;
        try {
            Class<?> rawEventClass = Class.forName("io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent");
            Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            Listener listener = new Listener() {};
            EventExecutor executor = (ignored, event) -> handleDeath(event);
            Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.HIGHEST, executor, plugin);
            registered = true;
            plugin.getLogger().info("已注册 MythicMobs 灵魂掉落桥接。");
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("未检测到兼容的 MythicMobs 死亡事件，跳过 ssdrops 桥接。");
        } catch (Exception failure) {
            plugin.getLogger().warning("注册 MythicMobs 桥接失败: " + failure.getMessage());
        }
    }

    private void handleDeath(Event event) {
        if (!plugin.getConfig().getBoolean("integrations.mythicmobs.enabled", true)) return;
        Player killer = readKiller(event);
        if (killer == null) return;
        Location deathLocation = readDeathLocation(event);
        captureEventDrops(event, killer, deathLocation);

        String mobId = readMobId(event);
        if (mobId.isEmpty()) return;
        List<DropRule> rules = rulesByMob.get(mobId.toLowerCase(Locale.ENGLISH));
        if (rules == null || rules.isEmpty()) return;
        for (DropRule rule : rules) {
            if (random.nextDouble() >= rule.chance) continue;
            ItemStack item = createDrop(rule);
            if (item == null) {
                warnMissingItem(rule.itemId);
                continue;
            }
            if (plugin.getAutoPickup() == null
                    || !plugin.getAutoPickup().spawnOwnedDrop(killer, deathLocation, item)) {
                dropAtDeath(event, killer, item);
            }
        }
    }

    private void captureEventDrops(Event event, Player killer, Location deathLocation) {
        if (plugin.getAutoPickup() == null || !plugin.getAutoPickup().canAutoPickup(killer)
                || deathLocation == null || deathLocation.getWorld() == null) {
            return;
        }
        List<ItemStack> drops = readEventDrops(event);
        if (drops.isEmpty() || !writeEventDrops(event, Collections.emptyList())) return;

        List<ItemStack> retained = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) continue;
            if (!plugin.getAutoPickup().spawnOwnedDrop(killer, deathLocation, drop)) {
                retained.add(drop);
            }
        }
        if (!retained.isEmpty() && !writeEventDrops(event, retained)) {
            for (ItemStack drop : retained) dropAtDeath(event, killer, drop);
        }
    }

    private List<ItemStack> readEventDrops(Event event) {
        Object value = invoke(event, "getDrops");
        if (!(value instanceof List)) return Collections.emptyList();
        List<?> rawDrops = (List<?>) value;
        List<ItemStack> drops = new ArrayList<>(rawDrops.size());
        for (Object rawDrop : rawDrops) {
            if (rawDrop instanceof ItemStack) drops.add((ItemStack) rawDrop);
        }
        return drops;
    }

    @SuppressWarnings("unchecked")
    private boolean writeEventDrops(Event event, List<ItemStack> drops) {
        try {
            event.getClass().getMethod("setDrops", List.class).invoke(event, new ArrayList<>(drops));
            return true;
        } catch (Exception ignored) {
            Object value = invoke(event, "getDrops");
            if (!(value instanceof List)) return false;
            try {
                List<Object> current = (List<Object>) value;
                current.clear();
                current.addAll(drops);
                return true;
            } catch (RuntimeException failure) {
                return false;
            }
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
        if (type == null) type = invoke(event, "getMobType");
        Object id = invoke(type, "getInternalName");
        if (id == null) id = invoke(type, "getId");
        if (id == null) id = invoke(mob, "getInternalName");
        return id == null ? "" : String.valueOf(id);
    }

    private void reloadRules() {
        warnedMissingItems.clear();
        if (!plugin.getConfig().getBoolean("integrations.mythicmobs.enabled", true)) {
            rulesByMob = Collections.emptyMap();
            return;
        }
        Map<String, List<DropRule>> loaded = new LinkedHashMap<>();
        collectRules(mythicMobsDirectory(), loaded);
        Map<String, List<DropRule>> snapshot = new LinkedHashMap<>();
        int ruleCount = 0;
        for (Map.Entry<String, List<DropRule>> entry : loaded.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            List<DropRule> rules = Collections.unmodifiableList(new ArrayList<>(entry.getValue()));
            snapshot.put(entry.getKey(), rules);
            ruleCount += rules.size();
        }
        rulesByMob = Collections.unmodifiableMap(snapshot);
        plugin.getLogger().info("已缓存 MythicMobs ssdrops 规则: " + snapshot.size()
                + " 个怪物，" + ruleCount + " 条掉落。");
    }

    private File mythicMobsDirectory() {
        Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
        File dataFolder = mythicMobs == null ? new File("plugins/MythicMobs") : mythicMobs.getDataFolder();
        return new File(dataFolder, "Mobs");
    }

    private void collectRules(File file, Map<String, List<DropRule>> rulesByMob) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File child : files) collectRules(child, rulesByMob);
            return;
        }
        if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".yml")) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String mobId : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(mobId);
            if (section == null) continue;
            String key = mobId.toLowerCase(Locale.ENGLISH);
            List<DropRule> rules = rulesByMob.get(key);
            for (String path : new String[]{"ssdrops", "SsDrops", "soulspace-drops", "SoulSpaceDrops"}) {
                for (String line : section.getStringList(path)) {
                    DropRule rule = DropRule.parse(line);
                    if (rule == null) {
                        plugin.getLogger().warning("忽略无效 MythicMobs ssdrops 规则 "
                                + file.getPath() + " -> " + mobId + ": " + line);
                        continue;
                    }
                    if (rules == null) {
                        rules = new ArrayList<>();
                        rulesByMob.put(key, rules);
                    }
                    rules.add(rule);
                }
            }
        }
    }

    private ItemStack createDrop(DropRule rule) {
        String itemId = rule.itemId.trim();
        int separator = itemId.indexOf(':');
        ItemStack item = null;
        if (separator > 0 && separator < itemId.length() - 1) {
            item = plugin.getXyCoreBridge().createItem(itemId, rule.amount);
            if (item == null) item = plugin.getItemLibrary().get(itemId);
            if (item == null && "mythicmobs".equalsIgnoreCase(itemId.substring(0, separator))) {
                item = generateMythicItem(itemId.substring(separator + 1), rule.amount);
            } else if (item == null && "minecraft".equalsIgnoreCase(itemId.substring(0, separator))) {
                Material material = Material.matchMaterial(itemId.substring(separator + 1));
                if (material != null && material != Material.AIR) item = new ItemStack(material, rule.amount);
            }
        } else {
            item = plugin.getItemLibrary().get(itemId);
            if (item == null) item = generateMythicItem(itemId, rule.amount);
        }
        if (item != null) item.setAmount(rule.amount);
        return item;
    }

    private void warnMissingItem(String itemId) {
        String normalized = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ENGLISH);
        if (!warnedMissingItems.add(normalized)) return;
        plugin.getLogger().warning("无法生成 MythicMobs ssdrops 物品: " + itemId
                + "；请检查完整物品 ID 及对应物品库是否已加载。");
    }

    @SuppressWarnings("unchecked")
    private void dropAtDeath(Event event, Player killer, ItemStack item) {
        Location location = readDeathLocation(event);
        if (location != null && location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, item.clone());
            return;
        }
        Object drops = invoke(event, "getDrops");
        if (drops instanceof List) {
            try {
                ((List<Object>) drops).add(item.clone());
                return;
            } catch (RuntimeException ignored) {
            }
        }
        if (killer.getWorld() != null) killer.getWorld().dropItemNaturally(killer.getLocation(), item.clone());
    }

    private Location readDeathLocation(Event event) {
        Object entityValue = invoke(event, "getEntity");
        if (!(entityValue instanceof Entity)) entityValue = invoke(entityValue, "getBukkitEntity");
        if (entityValue instanceof Entity) return ((Entity) entityValue).getLocation();
        Object location = invoke(entityValue, "getLocation");
        return location instanceof Location ? (Location) location : null;
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
            if (parts.length > 3) return null;
            String id = parts[0];
            Integer amount = parts.length > 1 ? parseInt(parts[1]) : Integer.valueOf(1);
            Double chance = parts.length > 2 ? parseChance(parts[2]) : Double.valueOf(1.0D);
            if (amount == null || chance == null) return null;
            return new DropRule(id, amount, chance);
        }

        private static Integer parseInt(String value) {
            try {
                int parsed = Integer.parseInt(value);
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static Double parseChance(String value) {
            try {
                boolean percent = value.endsWith("%");
                String normalized = percent ? value.substring(0, value.length() - 1) : value;
                double parsed = Double.parseDouble(normalized);
                if (Double.isNaN(parsed) || Double.isInfinite(parsed)) return null;
                if (percent) return parsed >= 0.0D && parsed <= 100.0D ? parsed / 100.0D : null;
                return parsed >= 0.0D && parsed <= 1.0D ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
