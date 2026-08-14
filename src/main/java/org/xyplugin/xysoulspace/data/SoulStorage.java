package org.xyplugin.xysoulspace.data;

import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.util.ItemKeys;
import org.xyplugin.xysoulspace.util.Text;
import org.xyplugin.xysoulspace.api.SoulSpaceWithdrawal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public final class SoulStorage {
    @FunctionalInterface
    public interface ItemMatcher {
        boolean matches(String namespacedItemId, ItemStack item);
    }

    private final Map<String, SoulItemRecord> items = new LinkedHashMap<>();
    private boolean pickupEnabled = true;
    private boolean dirty;
    private long revision;

    public synchronized void deposit(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return;
        depositAmount(item, item.getAmount());
    }

    public synchronized void depositAmount(ItemStack item, long amount) {
        if (item == null || item.getType().name().equals("AIR") || amount <= 0L) return;
        String key = ItemKeys.keyOf(item);
        SoulItemRecord record = items.get(key);
        if (record == null) {
            record = new SoulItemRecord(item, amount);
            items.put(key, record);
        } else {
            record.add(amount);
        }
        markChanged();
    }

    public synchronized long withdraw(String key, long amount) {
        SoulItemRecord record = items.get(key);
        if (record == null) return 0L;
        long removed = record.remove(amount);
        if (record.getAmount() <= 0L) items.remove(key);
        if (removed > 0L) markChanged();
        return removed;
    }

    public synchronized long getAmount(String key) {
        SoulItemRecord record = items.get(key);
        return record == null ? 0L : record.getAmount();
    }

    public synchronized ItemStack getItem(String key) {
        SoulItemRecord record = items.get(key);
        return record == null ? null : record.getItem();
    }

    public synchronized long getAmountByCostKey(String costKey) {
        long amount = 0L;
        for (SoulItemRecord record : items.values()) {
            if (matchesCost(record.getItem(), costKey)) amount += record.getAmount();
        }
        return amount;
    }

    public synchronized long removeByCostKey(String costKey, long amount) {
        long remaining = amount;
        ArrayList<String> keys = new ArrayList<>(items.keySet());
        for (String key : keys) {
            if (remaining <= 0L) break;
            SoulItemRecord record = items.get(key);
            if (record == null || !matchesCost(record.getItem(), costKey)) continue;
            long removed = record.remove(remaining);
            remaining -= removed;
            if (record.getAmount() <= 0L) items.remove(key);
        }
        long removed = amount - remaining;
        if (removed > 0L) markChanged();
        return removed;
    }

    public synchronized long getAmountByItemId(String namespacedItemId, ItemMatcher matcher) {
        if (!validItemId(namespacedItemId) || matcher == null) return 0L;
        long total = 0L;
        for (SoulItemRecord record : items.values()) {
            if (matcher.matches(namespacedItemId, record.getItem())) total += record.getAmount();
        }
        return total;
    }

    public synchronized boolean hasItems(Map<String, Long> requirements, ItemMatcher matcher) {
        return planWithdrawal(requirements, matcher).isPresent();
    }

    /** Builds the entire plan before mutating storage, preventing partial material loss. */
    public synchronized Optional<SoulSpaceWithdrawal> withdrawItems(Map<String, Long> requirements,
                                                                     ItemMatcher matcher) {
        Optional<WithdrawalPlan> planned = planWithdrawal(requirements, matcher);
        if (!planned.isPresent()) return Optional.empty();
        for (PlannedEntry entry : planned.get().entries) {
            SoulItemRecord record = items.get(entry.storageKey);
            if (record == null || record.remove(entry.amount) != entry.amount) {
                throw new IllegalStateException("SoulSpace atomic withdrawal plan became inconsistent");
            }
            if (record.getAmount() <= 0L) items.remove(entry.storageKey);
        }
        if (!planned.get().entries.isEmpty()) markChanged();
        return Optional.of(planned.get().receipt);
    }

    private Optional<WithdrawalPlan> planWithdrawal(Map<String, Long> requirements, ItemMatcher matcher) {
        if (requirements == null || requirements.isEmpty() || matcher == null) return Optional.empty();

        Map<String, Long> available = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, SoulItemRecord> entry : items.entrySet()) {
            available.put(entry.getKey(), entry.getValue().getAmount());
        }

        List<PlannedEntry> plan = new ArrayList<PlannedEntry>();
        List<SoulSpaceWithdrawal.Entry> receipt = new ArrayList<SoulSpaceWithdrawal.Entry>();
        for (Map.Entry<String, Long> requirement : requirements.entrySet()) {
            String itemId = requirement.getKey() == null ? "" : requirement.getKey().trim();
            long remaining = requirement.getValue() == null ? 0L : requirement.getValue();
            if (!validItemId(itemId) || remaining <= 0L) return Optional.empty();

            for (Map.Entry<String, SoulItemRecord> stored : items.entrySet()) {
                if (remaining <= 0L) break;
                long recordAvailable = available.get(stored.getKey());
                if (recordAvailable <= 0L || !matcher.matches(itemId, stored.getValue().getItem())) continue;
                long used = Math.min(remaining, recordAvailable);
                plan.add(new PlannedEntry(stored.getKey(), used));
                receipt.add(new SoulSpaceWithdrawal.Entry(itemId, stored.getValue().getItem(), used));
                available.put(stored.getKey(), recordAvailable - used);
                remaining -= used;
            }
            if (remaining > 0L) return Optional.empty();
        }
        return Optional.of(new WithdrawalPlan(plan, new SoulSpaceWithdrawal(receipt)));
    }

    private boolean validItemId(String itemId) {
        if (itemId == null) return false;
        int separator = itemId.indexOf(':');
        return separator > 0 && separator < itemId.length() - 1;
    }

    private static final class PlannedEntry {
        private final String storageKey;
        private final long amount;

        private PlannedEntry(String storageKey, long amount) {
            this.storageKey = storageKey;
            this.amount = amount;
        }
    }

    private static final class WithdrawalPlan {
        private final List<PlannedEntry> entries;
        private final SoulSpaceWithdrawal receipt;

        private WithdrawalPlan(List<PlannedEntry> entries, SoulSpaceWithdrawal receipt) {
            this.entries = entries;
            this.receipt = receipt;
        }
    }

    public synchronized Collection<Map.Entry<String, SoulItemRecord>> entriesSnapshot() {
        LinkedHashMap<String, SoulItemRecord> copy = new LinkedHashMap<>();
        for (Map.Entry<String, SoulItemRecord> entry : items.entrySet()) {
            copy.put(entry.getKey(), new SoulItemRecord(entry.getValue().getItem(), entry.getValue().getAmount()));
        }
        return copy.entrySet();
    }

    public synchronized Snapshot snapshot() {
        LinkedHashMap<String, SoulItemRecord> copy = new LinkedHashMap<>();
        for (Map.Entry<String, SoulItemRecord> entry : items.entrySet()) {
            copy.put(entry.getKey(), new SoulItemRecord(entry.getValue().getItem(), entry.getValue().getAmount()));
        }
        return new Snapshot(pickupEnabled, copy, revision);
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized void clear() {
        items.clear();
        markChanged();
    }

    public synchronized boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public synchronized void setPickupEnabled(boolean pickupEnabled) {
        this.pickupEnabled = pickupEnabled;
        markChanged();
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void markClean() {
        dirty = false;
    }

    public synchronized void markClean(long savedRevision) {
        if (revision == savedRevision) dirty = false;
    }

    public synchronized void markDirty() {
        markChanged();
    }

    private void markChanged() {
        dirty = true;
        revision++;
    }

    public static final class Snapshot {
        private final boolean pickupEnabled;
        private final Map<String, SoulItemRecord> items;
        private final long revision;

        private Snapshot(boolean pickupEnabled, Map<String, SoulItemRecord> items, long revision) {
            this.pickupEnabled = pickupEnabled;
            this.items = items;
            this.revision = revision;
        }

        public boolean isPickupEnabled() {
            return pickupEnabled;
        }

        public Collection<Map.Entry<String, SoulItemRecord>> entries() {
            return items.entrySet();
        }

        public long getRevision() {
            return revision;
        }
    }

    private boolean matchesCost(ItemStack item, String costKey) {
        if (costKey == null || costKey.trim().isEmpty()) return false;
        String normalized = Text.stripColor(costKey).trim();
        return item.getType().name().equalsIgnoreCase(normalized)
                || Text.costKey(item).equalsIgnoreCase(normalized);
    }
}
