package org.xyplugin.xysoulspace.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

public final class XyCoreBridge {
    private final XySoulSpacePlugin plugin;
    private boolean available;

    public XyCoreBridge(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        if (!plugin.getConfig().getBoolean("integrations.xcore.enabled", true)) {
            available = false;
            return;
        }
        Plugin core = Bukkit.getPluginManager().getPlugin("XyCore");
        available = core != null && core.isEnabled();
        if (available) {
            plugin.getLogger().info("已检测到 XyCore，XySoulSpace API 将作为 XY 生态扩展运行。");
        } else {
            plugin.getLogger().info("未检测到 XyCore，当前使用本地 YML 存储。");
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
