package org.xyplugin.xysoulspace.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

import java.lang.reflect.Method;

public final class XyCoreBridge {
    private final XySoulSpacePlugin plugin;
    private boolean available;
    private Object itemLibrary;
    private Method matchesMethod;

    public XyCoreBridge(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        if (!plugin.getConfig().getBoolean("integrations.xcore.enabled", true)) {
            available = false;
            itemLibrary = null;
            matchesMethod = null;
            return;
        }
        Plugin core = Bukkit.getPluginManager().getPlugin("XyCore");
        available = core != null && core.isEnabled();
        if (available) {
            try {
                ClassLoader loader = core.getClass().getClassLoader();
                Class<?> entry = Class.forName("org.xyplugin.xycore.api.XyCore", true, loader);
                Object api = entry.getMethod("get").invoke(null);
                itemLibrary = api.getClass().getMethod("getItems").invoke(api);
                matchesMethod = itemLibrary.getClass().getMethod("matches", String.class, ItemStack.class);
            } catch (Exception failure) {
                available = false;
                itemLibrary = null;
                matchesMethod = null;
                plugin.getLogger().warning("XyCore 物品库 API 不可用: " + failure.getMessage());
            }
        }
        if (available) {
            plugin.getLogger().info("已连接 XyCore 完整物品 ID 匹配服务。");
        } else {
            itemLibrary = null;
            matchesMethod = null;
            plugin.getLogger().info("未检测到 XyCore，当前使用本地 YML 存储。");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean matches(String namespacedItemId, ItemStack item) {
        if (!available || itemLibrary == null || matchesMethod == null) return false;
        try {
            return Boolean.TRUE.equals(matchesMethod.invoke(itemLibrary, namespacedItemId, item));
        } catch (Exception failure) {
            return false;
        }
    }
}
