package org.xyplugin.xysoulspace.listener;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DropOwnershipTrackerTest {
    @Test
    public void exactDeathDropIsBoundToKillerAndBecomesDueOnce() {
        DropOwnershipTracker<String> tracker = tracker();
        UUID world = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        ItemStack stack = new ItemStack(Material.IRON_INGOT, 16);

        tracker.recordDeath(world, 1.0D, 2.0D, 3.0D, owner,
                Collections.singletonList(stack), 100L);
        DropOwnershipTracker.OwnedDrop<String> owned = tracker.matchSpawn(item, world,
                1.2D, 2.1D, 3.1D, stack, "entity", 101L, 10L, 200L);

        assertEquals(owner, owned.getOwnerId());
        assertEquals("entity", owned.getItem());
        assertTrue(tracker.isOwned(item));
        assertNull(tracker.pollDue(9L));
        assertSame(owned, tracker.pollDue(10L));
        assertNull(tracker.pollDue(10L));
    }

    @Test
    public void identicalDeathStacksCanBeCombinedIntoOneSpawn() {
        DropOwnershipTracker<String> tracker = tracker();
        UUID world = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        ItemStack first = new ItemStack(Material.DIAMOND, 32);
        ItemStack second = new ItemStack(Material.DIAMOND, 32);

        tracker.recordDeath(world, 0.0D, 0.0D, 0.0D, owner,
                Arrays.asList(first, second), 1L);
        DropOwnershipTracker.OwnedDrop<String> combined = tracker.matchSpawn(UUID.randomUUID(), world,
                0.0D, 0.0D, 0.0D, new ItemStack(Material.DIAMOND, 64),
                "combined", 2L, 10L, 200L);

        assertEquals(owner, combined.getOwnerId());
        assertNull(tracker.matchSpawn(UUID.randomUUID(), world,
                0.0D, 0.0D, 0.0D, new ItemStack(Material.DIAMOND, 1),
                "extra", 3L, 10L, 200L));
    }

    @Test
    public void oversizedDistantWrongWorldAndExpiredSpawnsAreRejected() {
        DropOwnershipTracker<String> tracker = new DropOwnershipTracker<>(
                100L, 1.0D, 8, DropOwnershipTrackerTest::similarWithoutServer);
        UUID world = UUID.randomUUID();
        UUID otherWorld = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        ItemStack expected = new ItemStack(Material.EMERALD, 8);

        tracker.recordDeath(world, 0.0D, 0.0D, 0.0D, owner,
                Collections.singletonList(expected), 100L);
        assertNull(tracker.matchSpawn(UUID.randomUUID(), world, 0.0D, 0.0D, 0.0D,
                new ItemStack(Material.EMERALD, 9), "large", 110L, 10L, 200L));
        assertNull(tracker.matchSpawn(UUID.randomUUID(), otherWorld, 0.0D, 0.0D, 0.0D,
                expected, "world", 120L, 10L, 200L));
        assertNull(tracker.matchSpawn(UUID.randomUUID(), world, 2.0D, 0.0D, 0.0D,
                expected, "far", 130L, 10L, 200L));
        assertNull(tracker.matchSpawn(UUID.randomUUID(), world, 0.0D, 0.0D, 0.0D,
                expected, "expired", 201L, 10L, 200L));
    }

    @Test
    public void newestMatchingDeathWinsAtTheSameLocation() {
        DropOwnershipTracker<String> tracker = tracker();
        UUID world = UUID.randomUUID();
        UUID olderOwner = UUID.randomUUID();
        UUID newerOwner = UUID.randomUUID();
        ItemStack stack = new ItemStack(Material.GOLD_INGOT, 1);

        tracker.recordDeath(world, 0.0D, 0.0D, 0.0D, olderOwner,
                Collections.singletonList(stack), 1L);
        tracker.recordDeath(world, 0.0D, 0.0D, 0.0D, newerOwner,
                Collections.singletonList(stack), 2L);

        DropOwnershipTracker.OwnedDrop<String> newest = tracker.matchSpawn(UUID.randomUUID(), world,
                0.0D, 0.0D, 0.0D, stack, "new", 3L, 10L, 200L);
        DropOwnershipTracker.OwnedDrop<String> older = tracker.matchSpawn(UUID.randomUUID(), world,
                0.0D, 0.0D, 0.0D, stack, "old", 4L, 10L, 200L);

        assertEquals(newerOwner, newest.getOwnerId());
        assertEquals(olderOwner, older.getOwnerId());
    }

    @Test
    public void replacementAndRescheduleLeaveNoDuplicateDueEntries() {
        DropOwnershipTracker<String> tracker = tracker();
        UUID item = UUID.randomUUID();
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();

        tracker.register(item, firstOwner, "first", 10L, 200L);
        DropOwnershipTracker.OwnedDrop<String> replacement =
                tracker.register(item, secondOwner, "second", 20L, 200L);

        assertNull(tracker.pollDue(10L));
        assertSame(replacement, tracker.pollDue(20L));
        tracker.reschedule(replacement, 21L);
        assertNull(tracker.pollDue(20L));
        assertSame(replacement, tracker.pollDue(21L));
        tracker.remove(item);
        assertFalse(tracker.isOwned(item));
    }

    @Test
    public void queueBacklogDoesNotExpireReadyDrops() {
        assertFalse(AutoPickupListener.shouldReleaseForExtendedPickupDelay(10_000L, 200L, 0));
        assertTrue(AutoPickupListener.shouldReleaseForExtendedPickupDelay(201L, 200L, 1));
    }

    private static DropOwnershipTracker<String> tracker() {
        return new DropOwnershipTracker<>(2_000_000_000L, 1.0D, 2048,
                DropOwnershipTrackerTest::similarWithoutServer);
    }

    private static boolean similarWithoutServer(ItemStack left, ItemStack right) {
        return left.getType() == right.getType()
                && left.getDurability() == right.getDurability();
    }
}
