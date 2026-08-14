package org.xyplugin.xysoulspace.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

import java.util.Collection;
import java.lang.reflect.Method;
import java.util.Optional;

public final class XyCoreBridge {
    private final XySoulSpacePlugin plugin;
    private boolean available;
    private Object itemLibrary;
    private Method createMethod;
    private Method matchesMethod;
    private Method getProvidersMethod;
    private Collection<?> providers;

    public XyCoreBridge(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        if (!plugin.getConfig().getBoolean("integrations.xcore.enabled", true)) {
            available = false;
            itemLibrary = null;
            createMethod = null;
            matchesMethod = null;
            getProvidersMethod = null;
            providers = null;
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
                createMethod = itemLibrary.getClass().getMethod("create", String.class, int.class);
                matchesMethod = itemLibrary.getClass().getMethod("matches", String.class, ItemStack.class);
                getProvidersMethod = itemLibrary.getClass().getMethod("getProviders");
                Object providerValue = getProvidersMethod.invoke(itemLibrary);
                providers = providerValue instanceof Collection ? (Collection<?>) providerValue : null;
            } catch (Exception failure) {
                available = false;
                itemLibrary = null;
                createMethod = null;
                matchesMethod = null;
                getProvidersMethod = null;
                providers = null;
                plugin.getLogger().warning("XyCore 物品库 API 不可用: " + failure.getMessage());
            }
        }
        if (available) {
            plugin.getLogger().info("已连接 XyCore 完整物品 ID 匹配服务。");
        } else {
            itemLibrary = null;
            createMethod = null;
            matchesMethod = null;
            getProvidersMethod = null;
            providers = null;
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

    public ItemStack createItem(String namespacedItemId, int amount) {
        if (!available || itemLibrary == null || createMethod == null
                || namespacedItemId == null || amount <= 0) return null;
        try {
            Object value = createMethod.invoke(itemLibrary, namespacedItemId, amount);
            Object item = unwrapOptional(value);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Exception failure) {
            return null;
        }
    }

    public String displayId(ItemStack item) {
        String custom = customProviderId(item);
        if (custom != null && !custom.isEmpty()) return custom;
        if (item == null || item.getType() == null) return "";
        return "minecraft:" + item.getType().name();
    }

    private String customProviderId(ItemStack item) {
        if (!available || providers == null || item == null) return "";
        for (Object provider : providers) {
            if (provider == null) continue;
            try {
                Method availableMethod = provider.getClass().getMethod("isAvailable");
                if (!Boolean.TRUE.equals(availableMethod.invoke(provider))) continue;
                String providerId = String.valueOf(provider.getClass().getMethod("getId").invoke(provider));
                if ("minecraft".equalsIgnoreCase(providerId)) continue;
                Method identify = provider.getClass().getMethod("identify", ItemStack.class);
                Object value = identify.invoke(provider, item);
                String itemId = optionalValue(value);
                if (itemId != null && !itemId.trim().isEmpty()) return providerId + ":" + itemId.trim();
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String optionalValue(Object value) {
        if (value == null) return "";
        if (value instanceof Optional) {
            Optional<?> optional = (Optional<?>) value;
            return optional.isPresent() ? String.valueOf(optional.get()) : "";
        }
        return String.valueOf(value);
    }

    private Object unwrapOptional(Object value) {
        if (!(value instanceof Optional)) return value;
        Optional<?> optional = (Optional<?>) value;
        return optional.isPresent() ? optional.get() : null;
    }
}
