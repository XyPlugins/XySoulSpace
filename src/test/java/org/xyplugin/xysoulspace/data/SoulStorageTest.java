package org.xyplugin.xysoulspace.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Field;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.xyplugin.xysoulspace.api.SoulSpaceWithdrawal;

public class SoulStorageTest {
    @Test
    public void batchWithdrawalIsAllOrNothing() throws Exception {
        SoulStorage storage = new SoulStorage();
        Field itemsField = SoulStorage.class.getDeclaredField("items");
        itemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, SoulItemRecord> stored = (Map<String, SoulItemRecord>) itemsField.get(storage);
        stored.put("iron", new SoulItemRecord(new ItemStack(Material.IRON_INGOT), 16L));
        stored.put("diamond", new SoulItemRecord(new ItemStack(Material.DIAMOND), 2L));
        SoulStorage.ItemMatcher matcher = (id, item) -> id.equalsIgnoreCase("minecraft:" + item.getType().name());

        Map<String, Long> insufficient = new LinkedHashMap<String, Long>();
        insufficient.put("minecraft:IRON_INGOT", 16L);
        insufficient.put("minecraft:DIAMOND", 3L);
        assertFalse(storage.withdrawItems(insufficient, matcher).isPresent());
        assertEquals(16L, storage.getAmountByItemId("minecraft:IRON_INGOT", matcher));
        assertEquals(2L, storage.getAmountByItemId("minecraft:DIAMOND", matcher));

        Map<String, Long> exact = new LinkedHashMap<String, Long>();
        exact.put("minecraft:IRON_INGOT", 10L);
        exact.put("minecraft:DIAMOND", 2L);
        Optional<SoulSpaceWithdrawal> receipt = storage.withdrawItems(exact, matcher);
        assertTrue(receipt.isPresent());
        assertEquals(12L, receipt.get().getTotalAmount());
        assertEquals(6L, storage.getAmountByItemId("minecraft:IRON_INGOT", matcher));
        assertEquals(0L, storage.getAmountByItemId("minecraft:DIAMOND", matcher));
    }
}
