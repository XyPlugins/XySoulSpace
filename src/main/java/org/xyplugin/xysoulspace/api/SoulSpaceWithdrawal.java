package org.xyplugin.xysoulspace.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/** Immutable receipt containing the exact stacks removed by one atomic withdrawal. */
public final class SoulSpaceWithdrawal {
    private final List<Entry> entries;

    public SoulSpaceWithdrawal(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public long getTotalAmount() {
        long total = 0L;
        for (Entry entry : entries) total += entry.getAmount();
        return total;
    }

    public static final class Entry {
        private final String itemId;
        private final ItemStack item;
        private final long amount;

        public Entry(String itemId, ItemStack item, long amount) {
            this.itemId = itemId;
            ItemStack copy = item.clone();
            copy.setAmount(1);
            this.item = copy;
            this.amount = amount;
        }

        public String getItemId() {
            return itemId;
        }

        public ItemStack getItem() {
            return item.clone();
        }

        public long getAmount() {
            return amount;
        }
    }
}
