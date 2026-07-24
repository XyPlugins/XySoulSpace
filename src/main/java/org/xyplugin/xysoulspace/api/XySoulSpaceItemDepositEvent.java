package org.xyplugin.xysoulspace.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class XySoulSpaceItemDepositEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack item;
    private final String source;

    public XySoulSpaceItemDepositEvent(Player player, ItemStack item, String source) {
        this.player = player;
        this.item = item == null ? null : item.clone();
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public String getSource() {
        return source;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
