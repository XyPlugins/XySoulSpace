package org.xyplugin.xysoulspace.listener;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

final class DropOwnershipTracker<T> {
    private static final long DEFAULT_MATCH_WINDOW_NANOS = 2_000_000_000L;
    private static final double DEFAULT_MATCH_DISTANCE_SQUARED = 1.0D;
    private static final int DEFAULT_MAX_DEATH_BATCHES = 2048;

    private final long matchWindowNanos;
    private final double matchDistanceSquared;
    private final int maxDeathBatches;
    private final StackMatcher stackMatcher;
    private final Deque<DeathBatch> deathBatches = new ArrayDeque<>();
    private final Map<UUID, OwnedDrop<T>> ownedByItem = new HashMap<>();
    private final PriorityQueue<OwnedDrop<T>> dueDrops = new PriorityQueue<>();
    private long sequence;

    DropOwnershipTracker() {
        this(DEFAULT_MATCH_WINDOW_NANOS, DEFAULT_MATCH_DISTANCE_SQUARED,
                DEFAULT_MAX_DEATH_BATCHES, ItemStack::isSimilar);
    }

    DropOwnershipTracker(long matchWindowNanos, double matchDistanceSquared, int maxDeathBatches) {
        this(matchWindowNanos, matchDistanceSquared, maxDeathBatches, ItemStack::isSimilar);
    }

    DropOwnershipTracker(long matchWindowNanos, double matchDistanceSquared,
                         int maxDeathBatches, StackMatcher stackMatcher) {
        this.matchWindowNanos = Math.max(1L, matchWindowNanos);
        this.matchDistanceSquared = Math.max(0.0D, matchDistanceSquared);
        this.maxDeathBatches = Math.max(1, maxDeathBatches);
        this.stackMatcher = stackMatcher;
    }

    void recordDeath(UUID worldId, double x, double y, double z, UUID ownerId,
                     Iterable<ItemStack> drops, long nowNanos) {
        if (worldId == null || ownerId == null || drops == null) return;
        removeExpiredDeaths(nowNanos);
        List<ExpectedDrop> expected = aggregate(drops);
        if (expected.isEmpty()) return;
        while (deathBatches.size() >= maxDeathBatches) deathBatches.removeFirst();
        deathBatches.addLast(new DeathBatch(worldId, x, y, z, ownerId, nowNanos, expected));
    }

    OwnedDrop<T> matchSpawn(UUID itemId, UUID worldId, double x, double y, double z,
                            ItemStack stack, T item, long nowNanos, long dueTick, long expiresTick) {
        if (itemId == null || worldId == null || item == null || !valid(stack)) return null;
        removeExpiredDeaths(nowNanos);
        Iterator<DeathBatch> batches = deathBatches.descendingIterator();
        while (batches.hasNext()) {
            DeathBatch batch = batches.next();
            if (!batch.worldId.equals(worldId) || batch.distanceSquared(x, y, z) > matchDistanceSquared) {
                continue;
            }
            if (!batch.claim(stack, stackMatcher)) continue;
            if (batch.isEmpty()) batches.remove();
            return register(itemId, batch.ownerId, item, dueTick, expiresTick);
        }
        return null;
    }

    OwnedDrop<T> register(UUID itemId, UUID ownerId, T item, long dueTick, long expiresTick) {
        if (itemId == null || ownerId == null || item == null) return null;
        OwnedDrop<T> owned = new OwnedDrop<>(itemId, ownerId, item,
                Math.max(0L, dueTick), Math.max(dueTick, expiresTick), sequence++);
        ownedByItem.put(itemId, owned);
        dueDrops.add(owned);
        return owned;
    }

    OwnedDrop<T> get(UUID itemId) {
        return itemId == null ? null : ownedByItem.get(itemId);
    }

    boolean isOwned(UUID itemId) {
        return get(itemId) != null;
    }

    OwnedDrop<T> pollDue(long currentTick) {
        while (!dueDrops.isEmpty()) {
            OwnedDrop<T> next = dueDrops.peek();
            if (next.dueTick > currentTick) return null;
            dueDrops.poll();
            if (ownedByItem.get(next.itemId) == next) return next;
        }
        return null;
    }

    void reschedule(OwnedDrop<T> owned, long dueTick) {
        if (owned == null || ownedByItem.get(owned.itemId) != owned) return;
        owned.dueTick = Math.max(0L, dueTick);
        dueDrops.add(owned);
    }

    OwnedDrop<T> remove(UUID itemId) {
        return itemId == null ? null : ownedByItem.remove(itemId);
    }

    List<OwnedDrop<T>> clear() {
        List<OwnedDrop<T>> owned = new ArrayList<>(ownedByItem.values());
        deathBatches.clear();
        ownedByItem.clear();
        dueDrops.clear();
        return owned;
    }

    private void removeExpiredDeaths(long nowNanos) {
        while (!deathBatches.isEmpty()) {
            DeathBatch first = deathBatches.peekFirst();
            long elapsed = nowNanos - first.createdNanos;
            if (elapsed >= 0L && elapsed <= matchWindowNanos) break;
            deathBatches.removeFirst();
        }
    }

    private List<ExpectedDrop> aggregate(Iterable<ItemStack> drops) {
        List<ExpectedDrop> expected = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (!valid(stack)) continue;
            ExpectedDrop match = null;
            for (ExpectedDrop candidate : expected) {
                if (stackMatcher.isSimilar(candidate.template, stack)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                ItemStack template = stack.clone();
                template.setAmount(1);
                expected.add(new ExpectedDrop(template, stack.getAmount()));
            } else {
                match.remaining = safeAdd(match.remaining, stack.getAmount());
            }
        }
        return expected;
    }

    private static boolean valid(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0;
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    interface StackMatcher {
        boolean isSimilar(ItemStack left, ItemStack right);
    }

    static final class OwnedDrop<T> implements Comparable<OwnedDrop<T>> {
        private final UUID itemId;
        private final UUID ownerId;
        private final T item;
        private final long expiresTick;
        private final long sequence;
        private long dueTick;

        private OwnedDrop(UUID itemId, UUID ownerId, T item,
                          long dueTick, long expiresTick, long sequence) {
            this.itemId = itemId;
            this.ownerId = ownerId;
            this.item = item;
            this.dueTick = dueTick;
            this.expiresTick = expiresTick;
            this.sequence = sequence;
        }

        UUID getItemId() {
            return itemId;
        }

        UUID getOwnerId() {
            return ownerId;
        }

        T getItem() {
            return item;
        }

        long getExpiresTick() {
            return expiresTick;
        }

        @Override
        public int compareTo(OwnedDrop<T> other) {
            int dueComparison = Long.compare(dueTick, other.dueTick);
            return dueComparison != 0 ? dueComparison : Long.compare(sequence, other.sequence);
        }
    }

    private static final class DeathBatch {
        private final UUID worldId;
        private final double x;
        private final double y;
        private final double z;
        private final UUID ownerId;
        private final long createdNanos;
        private final List<ExpectedDrop> expected;

        private DeathBatch(UUID worldId, double x, double y, double z, UUID ownerId,
                           long createdNanos, List<ExpectedDrop> expected) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.ownerId = ownerId;
            this.createdNanos = createdNanos;
            this.expected = expected;
        }

        private double distanceSquared(double otherX, double otherY, double otherZ) {
            double deltaX = x - otherX;
            double deltaY = y - otherY;
            double deltaZ = z - otherZ;
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }

        private boolean claim(ItemStack stack, StackMatcher stackMatcher) {
            int amount = stack.getAmount();
            Iterator<ExpectedDrop> drops = expected.iterator();
            while (drops.hasNext()) {
                ExpectedDrop candidate = drops.next();
                if (candidate.remaining < amount
                        || !stackMatcher.isSimilar(candidate.template, stack)) continue;
                candidate.remaining -= amount;
                if (candidate.remaining == 0L) drops.remove();
                return true;
            }
            return false;
        }

        private boolean isEmpty() {
            return expected.isEmpty();
        }
    }

    private static final class ExpectedDrop {
        private final ItemStack template;
        private long remaining;

        private ExpectedDrop(ItemStack template, long remaining) {
            this.template = template;
            this.remaining = Math.max(0L, remaining);
        }
    }
}
