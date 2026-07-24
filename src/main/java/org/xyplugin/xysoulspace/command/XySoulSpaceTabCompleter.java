package org.xyplugin.xysoulspace.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.xyplugin.xysoulspace.XySoulSpacePlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class XySoulSpaceTabCompleter implements TabCompleter {
    private final XySoulSpacePlugin plugin;

    public XySoulSpaceTabCompleter(XySoulSpacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("open", "store", "pickup", "globalpickup", "shop", "reloadshop", "saveitem", "giveitem", "admin", "clear", "reload", "help"), args[0]);
        }
        if (args.length == 2 && "shop".equalsIgnoreCase(args[0])) {
            return filter(new ArrayList<>(plugin.getSoulShop().getShopNames()), args[1]);
        }
        if (args.length == 2 && "pickup".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("on", "off"), args[1]);
        }
        if (args.length == 2 && "globalpickup".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("on", "off", "status"), args[1]);
        }
        if (args.length == 2 && "giveitem".equalsIgnoreCase(args[0])) {
            return filter(new ArrayList<>(plugin.getItemLibrary().names()), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(lower)) result.add(value);
        }
        return result;
    }
}
