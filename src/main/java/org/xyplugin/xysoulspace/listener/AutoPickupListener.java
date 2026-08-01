package org.xyplugin.xysoulspace.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.util.Text;

public final class AutoPickupListener implements Listener {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;

    public AutoPickupListener(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void startTask() {
        long interval = Math.max(1L, plugin.getConfig().getLong("pickup.scan-interval-ticks", 10L));
        Bukkit.getScheduler().runTaskTimer(plugin, this::scanOnlinePlayers, interval, interval);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!canPickup(player)) return;
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        if (stack == null || stack.getType() == Material.AIR) return;
        if (service.deposit(player, stack, "pickup")) {
            event.setCancelled(true);
            item.remove();
            sendPickupMessage(player, stack);
        }
    }

    private void scanOnlinePlayers() {
        if (!plugin.getConfig().getBoolean("pickup.global-enabled", true)) return;
        double range = Math.max(0.5D, plugin.getConfig().getDouble("pickup.range", 6.0D));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canPickup(player)) continue;
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (!(entity instanceof Item)) continue;
                Item item = (Item) entity;
                if (item.isDead() || !item.isValid() || item.getPickupDelay() > 0) continue;
                ItemStack stack = item.getItemStack();
                if (stack == null || stack.getType() == Material.AIR) continue;
                if (service.deposit(player, stack, "pickup")) {
                    item.remove();
                    sendPickupMessage(player, stack);
                }
            }
        }
    }

    private boolean canPickup(Player player) {
        return player.hasPermission("xysoulspace.use")
                && plugin.getConfig().getBoolean("pickup.global-enabled", true)
                && service.getStorage(player.getUniqueId()).isPickupEnabled();
    }

    private void sendPickupMessage(Player player, ItemStack stack) {
        if (!plugin.getConfig().getBoolean("pickup.message-enabled", true)) return;
        String message = plugin.getConfig().getString("pickup.message", "");
        Text.sendRaw(player, plugin.getConfig(), message,
                "%amount%", String.valueOf(stack.getAmount()),
                "%item%", Text.itemName(stack));
    }
}
