package org.xyplugin.xysoulspace.data;

import org.bukkit.inventory.ItemStack;

public final class SoulItemRecord {
    private final ItemStack item;
    private long amount;

    public SoulItemRecord(ItemStack item, long amount) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        this.item = copy;
        this.amount = Math.max(0L, amount);
    }

    public ItemStack getItem() {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return copy;
    }

    public long getAmount() {
        return amount;
    }

    public void add(long delta) {
        if (delta <= 0L) return;
        amount = Long.MAX_VALUE - amount < delta ? Long.MAX_VALUE : amount + delta;
    }

    public long remove(long delta) {
        long removed = Math.min(amount, Math.max(0L, delta));
        amount -= removed;
        return removed;
    }
}
