package org.xyplugin.xysoulspace.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.data.SoulStorage;

import java.util.UUID;
import java.util.Map;
import java.util.Optional;

public interface XySoulSpaceApi {
    SoulStorage getStorage(UUID playerId);

    boolean deposit(Player player, ItemStack item);

    boolean deposit(UUID playerId, ItemStack item);

    long removeByCostKey(UUID playerId, String costKey, long amount);

    long getAmountByCostKey(UUID playerId, String costKey);

    /** Counts stacks by their complete XyCore item-library id, such as xyitems:forge_crystal. */
    long getAmountByItemId(UUID playerId, String namespacedItemId);

    /** Checks all requested complete item ids against one consistent storage snapshot. */
    boolean hasItems(UUID playerId, Map<String, Long> requirements);

    /** Removes all requested materials or removes nothing, and returns an exact refund receipt. */
    Optional<SoulSpaceWithdrawal> withdrawItems(UUID playerId, Map<String, Long> requirements);

    /** Refunds a percentage of a trusted withdrawal receipt; returns the amount restored. */
    long refund(UUID playerId, SoulSpaceWithdrawal withdrawal, int percent);

    void open(Player player);
}
