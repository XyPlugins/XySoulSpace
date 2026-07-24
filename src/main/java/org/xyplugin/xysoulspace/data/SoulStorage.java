package org.xyplugin.xysoulspace.data;

import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.util.ItemKeys;
import org.xyplugin.xysoulspace.util.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SoulStorage {
    private final Map<String, SoulItemRecord> items = new LinkedHashMap<>();
    private boolean pickupEnabled = true;
    private boolean dirty;

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
        dirty = true;
    }

    public synchronized long withdraw(String key, long amount) {
        SoulItemRecord record = items.get(key);
        if (record == null) return 0L;
        long removed = record.remove(amount);
        if (record.getAmount() <= 0L) items.remove(key);
        if (removed > 0L) dirty = true;
        return removed;
    }

    public synchronized long getAmount(String key) {
        SoulItemRecord record = items.get(key);
        return record == null ? 0L : record.getAmount();
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
        if (removed > 0L) dirty = true;
        return removed;
    }

    public synchronized Collection<Map.Entry<String, SoulItemRecord>> entriesSnapshot() {
        LinkedHashMap<String, SoulItemRecord> copy = new LinkedHashMap<>();
        for (Map.Entry<String, SoulItemRecord> entry : items.entrySet()) {
            copy.put(entry.getKey(), new SoulItemRecord(entry.getValue().getItem(), entry.getValue().getAmount()));
        }
        return copy.entrySet();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized void clear() {
        items.clear();
        dirty = true;
    }

    public synchronized boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public synchronized void setPickupEnabled(boolean pickupEnabled) {
        this.pickupEnabled = pickupEnabled;
        dirty = true;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void markClean() {
        dirty = false;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    private boolean matchesCost(ItemStack item, String costKey) {
        if (costKey == null || costKey.trim().isEmpty()) return false;
        String normalized = Text.stripColor(costKey).trim();
        return item.getType().name().equalsIgnoreCase(normalized)
                || Text.costKey(item).equalsIgnoreCase(normalized);
    }
}
