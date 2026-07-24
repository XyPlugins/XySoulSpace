package org.xyplugin.xysoulspace;

import org.bukkit.Bukkit;
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
    private int autosaveTask = -1;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveShopConfig();

        service = new SoulSpaceService(this);
        gui = new SoulSpaceGui(this, service);
        service.setGui(gui);
        decomposeService = new DecomposeService(this, service);
        itemLibrary = new ItemLibrary(this);
        soulShop = new SoulShop(this, service);
        xyCoreBridge = new XyCoreBridge(this);
        xyCoreBridge.refresh();
        mythicMobsBridge = new MythicMobsBridge(this);
        mythicMobsBridge.registerIfAvailable();

        AutoPickupListener autoPickup = new AutoPickupListener(this, service);
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
        if (autosaveTask != -1) Bukkit.getScheduler().cancelTask(autosaveTask);
        if (service != null) service.saveAll();
        instance = null;
    }

    public void reloadXySoulSpace() {
        reloadConfig();
        saveShopConfig();
        itemLibrary.reload();
        soulShop.reload();
        xyCoreBridge.refresh();
    }

    private void startAutosave() {
        long interval = Math.max(200L, getConfig().getLong("storage.autosave-interval-ticks", 1200L));
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> service.saveDirty(), interval, interval).getTaskId();
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
}
