package org.xyplugin.xysoulspace;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.xyplugin.xysoulspace.api.XySoulSpaceApi;
import org.xyplugin.xysoulspace.command.XySoulSpaceCommand;
import org.xyplugin.xysoulspace.command.XySoulSpaceTabCompleter;
import org.xyplugin.xysoulspace.gui.SoulSpaceGui;
import org.xyplugin.xysoulspace.integration.XyCoreBridge;
import org.xyplugin.xysoulspace.integration.MythicMobsBridge;
import org.xyplugin.xysoulspace.item.ItemLibrary;
import org.xyplugin.xysoulspace.listener.AutoPickupListener;
import org.xyplugin.xysoulspace.listener.PlayerDataListener;
import org.xyplugin.xysoulspace.shop.ShopListener;
import org.xyplugin.xysoulspace.shop.SoulShop;
import org.xyplugin.xysoulspace.util.Text;
import org.xyplugin.xysoulspace.util.VanillaMaterialNames;

import java.io.File;

public final class XySoulSpacePlugin extends JavaPlugin {
    private static XySoulSpacePlugin instance;

    private SoulSpaceService service;
    private SoulSpaceGui gui;
    private SoulShop soulShop;
    private ItemLibrary itemLibrary;
    private DecomposeService decomposeService;
    private XyCoreBridge xyCoreBridge;
    private MythicMobsBridge mythicMobsBridge;
    private AutoPickupListener autoPickup;
    private int autosaveTask = -1;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveShopConfig();

        xyCoreBridge = new XyCoreBridge(this);
        xyCoreBridge.refresh();
        service = new SoulSpaceService(this, xyCoreBridge);
        autoPickup = new AutoPickupListener(this, service);
        gui = new SoulSpaceGui(this, service);
        service.setGui(gui);
        decomposeService = new DecomposeService(this, service);
        itemLibrary = new ItemLibrary(this);
        soulShop = new SoulShop(this, service);
        mythicMobsBridge = new MythicMobsBridge(this);
        mythicMobsBridge.registerIfAvailable();

        Bukkit.getPluginManager().registerEvents(autoPickup, this);
        Bukkit.getPluginManager().registerEvents(new PlayerDataListener(service), this);
        Bukkit.getPluginManager().registerEvents(new ShopListener(soulShop), this);
        autoPickup.startTask();

        if (getCommand("xyss") != null) {
            getCommand("xyss").setExecutor(new XySoulSpaceCommand(this, service));
            getCommand("xyss").setTabCompleter(new XySoulSpaceTabCompleter(this));
        }
        startAutosave();
        logStartupSummary();
    }

    @Override
    public void onDisable() {
        if (autoPickup != null) autoPickup.stopTask();
        if (autosaveTask != -1) Bukkit.getScheduler().cancelTask(autosaveTask);
        autosaveTask = -1;
        if (service != null) service.saveAll();
        instance = null;
    }

    public void reloadXySoulSpace() {
        reloadConfig();
        saveShopConfig();
        itemLibrary.reload();
        soulShop.reload();
        xyCoreBridge.refresh();
        if (mythicMobsBridge != null) mythicMobsBridge.reload();
        if (autoPickup != null) autoPickup.restartTask();
        restartAutosave();
    }

    private void startAutosave() {
        long interval = Math.max(200L, getConfig().getLong("storage.autosave-interval-ticks", 1200L));
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> service.saveDirty(), interval, interval).getTaskId();
    }

    private void restartAutosave() {
        if (autosaveTask != -1) Bukkit.getScheduler().cancelTask(autosaveTask);
        autosaveTask = -1;
        startAutosave();
    }

    private void saveShopConfig() {
        File file = new File(getDataFolder(), "shop.yml");
        if (!file.exists()) saveResource("shop.yml", false);
    }

    private void logStartupSummary() {
        getLogger().info("============================================================");
        getLogger().info("XySoulSpace " + getDescription().getVersion() + " 已启用");
        getLogger().info("存储模式: " + getConfig().getString("storage.type", "yaml")
                + (xyCoreBridge.isAvailable() ? "（已检测到 XyCore）" : "（本地 YML）"));
        getLogger().info("命令入口: /xyss");
        getLogger().info("灵魂商店数量: " + soulShop.getShopNames().size());
        getLogger().info("============================================================");
    }

    public static XySoulSpacePlugin getInstance() {
        return instance;
    }

    public XySoulSpaceApi getApi() {
        return service;
    }

    public SoulSpaceGui getGui() {
        return gui;
    }

    public SoulShop getSoulShop() {
        return soulShop;
    }

    public ItemLibrary getItemLibrary() {
        return itemLibrary;
    }

    public DecomposeService getDecomposeService() {
        return decomposeService;
    }

    public SoulSpaceService getService() {
        return service;
    }

    public AutoPickupListener getAutoPickup() {
        return autoPickup;
    }

    public XyCoreBridge getXyCoreBridge() {
        return xyCoreBridge;
    }

    public String itemDisplayName(ItemStack item) {
        if (item == null) return "";
        String mode = getConfig().getString("messages.item-name-mode", "id").trim().toLowerCase();
        String itemId = itemId(item);
        String customName = "raw-id".equals(mode) ? "" : customDisplayName(item);
        String vanillaName = customName.isEmpty() && itemId.regionMatches(true, 0,
                "minecraft:", 0, "minecraft:".length()) ? vanillaDisplayName(item) : "";
        return selectMessageItemName(mode, itemId, customName, vanillaName);
    }

    static String selectMessageItemName(String mode, String itemId, String customName, String vanillaName) {
        String safeId = itemId == null ? "" : itemId;
        if ("raw-id".equalsIgnoreCase(mode)) return safeId;
        if (customName != null && !customName.isEmpty()) return customName;
        if (safeId.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())
                && vanillaName != null && !vanillaName.isEmpty()) return vanillaName;
        return safeId;
    }

    public String itemId(ItemStack item) {
        if (xyCoreBridge != null && xyCoreBridge.isAvailable()) {
            String id = xyCoreBridge.displayId(item);
            if (id != null && !id.trim().isEmpty()) return id;
        }
        return "minecraft:" + item.getType().name();
    }

    public String itemFriendlyName(ItemStack item) {
        if (item == null) return "";
        String customName = customDisplayName(item);
        if (!customName.isEmpty()) return customName;
        String itemId = itemId(item);
        if (!itemId.toLowerCase().startsWith("minecraft:")) return itemId;
        String vanillaName = vanillaDisplayName(item);
        if (!vanillaName.isEmpty()) return vanillaName;
        return itemId;
    }

    private String customDisplayName(ItemStack item) {
        try {
            return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? Text.itemName(item) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String vanillaDisplayName(ItemStack item) {
        if (item == null || item.getType() == null) return "";
        String material = item.getType().name();
        String configured = getConfig().getString("messages.vanilla-names." + material, "");
        if (configured != null && !configured.trim().isEmpty()) return Text.color(configured);
        return VanillaMaterialNames.get(material);
    }
}
