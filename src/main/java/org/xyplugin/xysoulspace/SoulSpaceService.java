package org.xyplugin.xysoulspace;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.api.XySoulSpaceApi;
import org.xyplugin.xysoulspace.api.XySoulSpaceItemDepositEvent;
import org.xyplugin.xysoulspace.api.SoulSpaceWithdrawal;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.data.YamlSoulStorageRepository;
import org.xyplugin.xysoulspace.gui.SoulSpaceGui;
import org.xyplugin.xysoulspace.integration.XyCoreBridge;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulSpaceService implements XySoulSpaceApi {
    private final XySoulSpacePlugin plugin;
    private final YamlSoulStorageRepository repository;
    private final XyCoreBridge core;
    private final Map<UUID, SoulStorage> cache = new ConcurrentHashMap<>();
    private SoulSpaceGui gui;

    public SoulSpaceService(XySoulSpacePlugin plugin, XyCoreBridge core) {
        this.plugin = plugin;
        this.core = core;
        this.repository = new YamlSoulStorageRepository(plugin);
    }

    public void setGui(SoulSpaceGui gui) {
        this.gui = gui;
    }

    @Override
    public SoulStorage getStorage(UUID playerId) {
        return cache.computeIfAbsent(playerId, repository::load);
    }

    @Override
    public boolean deposit(Player player, ItemStack item) {
        boolean result = deposit(player.getUniqueId(), item);
        if (result) {
            Bukkit.getPluginManager().callEvent(new XySoulSpaceItemDepositEvent(player, item, "plugin"));
            if (gui != null) gui.refreshIfOpen(player);
        }
        return result;
    }

    public boolean deposit(Player player, ItemStack item, String source) {
        boolean result = deposit(player.getUniqueId(), item);
        if (result) {
            Bukkit.getPluginManager().callEvent(new XySoulSpaceItemDepositEvent(player, item, source));
            if (gui != null) gui.refreshIfOpen(player);
        }
        return result;
    }

    @Override
    public boolean deposit(UUID playerId, ItemStack item) {
        if (item == null || item.getAmount() <= 0) return false;
        SoulStorage storage = getStorage(playerId);
        storage.deposit(item);
        if (plugin.getConfig().getBoolean("storage.save-on-deposit", false)) save(playerId);
        return true;
    }

    @Override
    public long removeByCostKey(UUID playerId, String costKey, long amount) {
        return getStorage(playerId).removeByCostKey(costKey, amount);
    }

    @Override
    public long getAmountByCostKey(UUID playerId, String costKey) {
        return getStorage(playerId).getAmountByCostKey(costKey);
    }

    @Override
    public long getAmountByItemId(UUID playerId, String namespacedItemId) {
        return getStorage(playerId).getAmountByItemId(namespacedItemId, core::matches);
    }

    @Override
    public boolean hasItems(UUID playerId, Map<String, Long> requirements) {
        return getStorage(playerId).hasItems(requirements, core::matches);
    }

    @Override
    public Optional<SoulSpaceWithdrawal> withdrawItems(UUID playerId, Map<String, Long> requirements) {
        return getStorage(playerId).withdrawItems(requirements, core::matches);
    }

    @Override
    public long refund(UUID playerId, SoulSpaceWithdrawal withdrawal, int percent) {
        if (withdrawal == null || percent <= 0) return 0L;
        int safePercent = Math.min(100, percent);
        SoulStorage storage = getStorage(playerId);
        long restored = 0L;
        for (SoulSpaceWithdrawal.Entry entry : withdrawal.getEntries()) {
            long amount = percentage(entry.getAmount(), safePercent);
            if (amount <= 0L) continue;
            storage.depositAmount(entry.getItem(), amount);
            restored = Long.MAX_VALUE - restored < amount ? Long.MAX_VALUE : restored + amount;
        }
        return restored;
    }

    private long percentage(long amount, int percent) {
        if (amount <= 0L || percent <= 0) return 0L;
        return (amount / 100L) * percent + ((amount % 100L) * percent) / 100L;
    }

    @Override
    public void open(Player player) {
        if (gui != null) gui.open(player, player, 0, false);
    }

    public void save(UUID playerId) {
        SoulStorage storage = cache.get(playerId);
        if (storage != null) repository.save(playerId, storage);
    }

    public void saveDirty() {
        for (Map.Entry<UUID, SoulStorage> entry : new ArrayList<>(cache.entrySet())) {
            if (entry.getValue().isDirty()) repository.save(entry.getKey(), entry.getValue());
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, SoulStorage> entry : new ArrayList<>(cache.entrySet())) {
            repository.save(entry.getKey(), entry.getValue());
        }
    }

    public void unload(UUID playerId) {
        save(playerId);
        cache.remove(playerId);
    }
}
