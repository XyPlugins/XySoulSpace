package org.xyplugin.xysoulspace.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.xyplugin.xysoulspace.util.Text;

public final class ShopListener implements Listener {
    private final SoulShop shop;

    public ShopListener(SoulShop shop) {
        this.shop = shop;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = Text.stripColor(event.getView().getTitle());
        if (!title.startsWith("灵魂商店[")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        String shopName = title.substring("灵魂商店[".length(), title.length() - 1);
        int amount = event.isRightClick() ? 64 : 1;
        shop.buy((Player) event.getWhoClicked(), shopName, event.getRawSlot(), amount);
    }
}
