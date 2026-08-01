package org.xyplugin.xysoulspace.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.xyplugin.xysoulspace.SoulSpaceService;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;
import org.xyplugin.xysoulspace.data.SoulStorage;
import org.xyplugin.xysoulspace.util.Text;

public final class XySoulSpaceCommand implements CommandExecutor {
    private final XySoulSpacePlugin plugin;
    private final SoulSpaceService service;

    public XySoulSpaceCommand(XySoulSpacePlugin plugin, SoulSpaceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("reload".equals(sub)) return reload(sender);
        if ("reloadshop".equals(sub)) return reloadShop(sender);
        if ("globalpickup".equals(sub)) return globalPickup(sender, args);
        if ("giveitem".equals(sub)) return giveItem(sender, args);
        if (!(sender instanceof Player)) {
            Text.send(sender, plugin.getConfig(), "only-player");
            return true;
        }
        Player player = (Player) sender;
        switch (sub) {
            case "open":
            case "gui":
            case "list":
                if (!has(player, "xysoulspace.use")) return true;
                plugin.getGui().open(player, player, 0, false);
                return true;
            case "store":
                if (!has(player, "xysoulspace.use")) return true;
                storeHand(player);
                return true;
            case "pickup":
                if (!has(player, "xysoulspace.use")) return true;
                pickup(player, args);
                return true;
            case "shop":
                if (!has(player, "xysoulspace.shop.use")) return true;
                plugin.getSoulShop().open(player, args.length > 1 ? args[1] : "默认");
                return true;
            case "saveitem":
                if (!has(player, "xysoulspace.admin")) return true;
                if (args.length < 2) {
                    Text.sendRaw(player, plugin.getConfig(), "&c用法: /xyss saveitem <id>");
                    return true;
                }
                plugin.getItemLibrary().saveItem(player, args[1]);
                return true;
            case "admin":
                if (!has(player, "xysoulspace.admin")) return true;
                adminOpen(player, args);
                return true;
            case "clear":
                if (!has(player, "xysoulspace.admin")) return true;
                clear(player, args);
                return true;
            default:
                help(sender);
                return true;
        }
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("xysoulspace.reload")) {
            Text.send(sender, plugin.getConfig(), "no-permission");
            return true;
        }
        plugin.reloadXySoulSpace();
        Text.send(sender, plugin.getConfig(), "reload");
        return true;
    }

    private boolean giveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xysoulspace.admin")) {
            Text.send(sender, plugin.getConfig(), "no-permission");
            return true;
        }
        if (args.length < 4) {
            Text.sendRaw(sender, plugin.getConfig(), "&c用法: /xyss giveitem <id> <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            Text.send(sender, plugin.getConfig(), "player-not-found");
            return true;
        }
        ItemStack item = plugin.getItemLibrary().get(args[1]);
        if (item == null) {
            Text.send(sender, plugin.getConfig(), "item-library-missing", "%id%", args[1]);
            return true;
        }
        int amount = parseInt(args[3], 1);
        item.setAmount(Math.max(1, amount));
        service.deposit(target, item, "library");
        service.save(target.getUniqueId());
        Text.send(sender, plugin.getConfig(), "item-library-given",
                "%id%", args[1],
                "%player%", target.getName(),
                "%amount%", String.valueOf(amount));
        return true;
    }

    private void storeHand(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getAmount() <= 0 || hand.getType().name().equals("AIR")) return;
        service.deposit(player, hand, "manual");
        player.setItemInHand(null);
        service.save(player.getUniqueId());
        Text.send(player, plugin.getConfig(), "stored",
                "%amount%", String.valueOf(hand.getAmount()),
                "%item%", Text.itemName(hand));
    }

    private void pickup(Player player, String[] args) {
        SoulStorage storage = service.getStorage(player.getUniqueId());
        if (args.length < 2) {
            Text.sendRaw(player, plugin.getConfig(), "&7个人自动拾取: " + (storage.isPickupEnabled() ? "&a开启" : "&c关闭"));
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        storage.setPickupEnabled(enabled);
        service.save(player.getUniqueId());
        Text.send(player, plugin.getConfig(), enabled ? "pickup-on" : "pickup-off");
    }

    private void adminOpen(Player player, String[] args) {
        if (args.length < 2) {
            Text.sendRaw(player, plugin.getConfig(), "&c用法: /xyss admin <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Text.send(player, plugin.getConfig(), "player-not-found");
            return;
        }
        plugin.getGui().open(player, target, 0, true);
    }

    private void clear(Player player, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
        if (target == null) {
            Text.send(player, plugin.getConfig(), "player-not-found");
            return;
        }
        service.getStorage(target.getUniqueId()).clear();
        service.save(target.getUniqueId());
        Text.send(player, plugin.getConfig(), "storage-cleared", "%player%", target.getName());
    }

    private boolean has(Player player, String permission) {
        if (player.isOp() || player.hasPermission(permission)) return true;
        Text.send(player, plugin.getConfig(), "no-permission");
        return false;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void help(CommandSender sender) {
        Text.sendRaw(sender, plugin.getConfig(), "&6=== XySoulSpace " + plugin.getDescription().getVersion() + " ===");
        Text.sendRaw(sender, plugin.getConfig(), "&e/xyss open &7打开灵魂空间");
        Text.sendRaw(sender, plugin.getConfig(), "&e/xyss store &7存入手中物品");
        Text.sendRaw(sender, plugin.getConfig(), "&e/xyss pickup <on|off> &7个人自动拾取");
        Text.sendRaw(sender, plugin.getConfig(), "&e/xyss shop [商店名] &7打开灵魂商店");
        if (sender.hasPermission("xysoulspace.admin")) {
            Text.sendRaw(sender, plugin.getConfig(), "&e/xyss admin <玩家> &7查看玩家灵魂空间");
            Text.sendRaw(sender, plugin.getConfig(), "&e/xyss clear [玩家] &7清空灵魂空间");
            Text.sendRaw(sender, plugin.getConfig(), "&e/xyss saveitem <id> &7保存手中物品到物品库");
            Text.sendRaw(sender, plugin.getConfig(), "&e/xyss giveitem <id> <玩家> <数量> &7发放物品到灵魂空间");
            Text.sendRaw(sender, plugin.getConfig(), "&e/xyss globalpickup <on|off|status> &7全局自动拾取");
        }
    }

    private boolean reloadShop(CommandSender sender) {
        if (!sender.hasPermission("xysoulspace.shop.admin")) {
            Text.send(sender, plugin.getConfig(), "no-permission");
            return true;
        }
        plugin.getSoulShop().reload();
        Text.sendRaw(sender, plugin.getConfig(), "&a灵魂商店配置已重载。");
        return true;
    }

    private boolean globalPickup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xysoulspace.admin")) {
            Text.send(sender, plugin.getConfig(), "no-permission");
            return true;
        }
        if (args.length < 2 || "status".equalsIgnoreCase(args[1])) {
            Text.sendRaw(sender, plugin.getConfig(), "&7全局自动拾取: "
                    + (plugin.getConfig().getBoolean("pickup.global-enabled", true) ? "&a开启" : "&c关闭"));
            return true;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getConfig().set("pickup.global-enabled", enabled);
        plugin.saveConfig();
        Text.send(sender, plugin.getConfig(), enabled ? "global-pickup-on" : "global-pickup-off");
        return true;
    }
}
