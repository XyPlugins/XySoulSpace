package org.xyplugin.xysoulspace.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.data.SoulStorage;

import java.util.UUID;

public interface XySoulSpaceApi {
    SoulStorage getStorage(UUID playerId);

    boolean deposit(Player player, ItemStack item);

    boolean deposit(UUID playerId, ItemStack item);

    long removeByCostKey(UUID playerId, String costKey, long amount);

    long getAmountByCostKey(UUID playerId, String costKey);

    void open(Player player);
}
