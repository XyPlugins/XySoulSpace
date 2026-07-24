package org.xyplugin.xysoulspace.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class Inventorys {
    private Inventorys() {
    }

    public static long addItems(Inventory inventory, ItemStack template, long amount) {
        long accepted = 0L;
        long remaining = amount;
        while (remaining > 0L) {
            ItemStack stack = template.clone();
            int give = (int) Math.min(Math.min(remaining, stack.getMaxStackSize()), 64L);
            stack.setAmount(give);
            java.util.Map<Integer, ItemStack> leftover = inventory.addItem(stack);
            int notAccepted = 0;
            for (ItemStack item : leftover.values()) notAccepted += item.getAmount();
            accepted += give - notAccepted;
            if (notAccepted > 0) break;
            remaining -= give;
        }
        return accepted;
    }

    public static boolean hasFreeSlot(Inventory inventory) {
        return inventory.firstEmpty() >= 0;
    }
}
