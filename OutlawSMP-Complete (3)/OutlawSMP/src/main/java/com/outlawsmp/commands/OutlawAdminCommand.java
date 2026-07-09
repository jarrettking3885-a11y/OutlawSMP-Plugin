package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Lightweight admin command surface. Only covers what's needed to operate
 * the Hunter Event by hand for now (reload, event start/stop); more
 * subcommands (givewish, givecoins, backups, etc.) land in a later pass.
 */
public class OutlawAdminCommand implements CommandExecutor {

    private final OutlawSMP plugin;

    public OutlawAdminCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("outlawsmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix() + ChatColor.GRAY + "Usage: /outlawadmin <reload|event start [player]|event stop>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(prefix() + ChatColor.GREEN + "Config reloaded.");
            }
            case "event" -> handleEvent(sender, args);
            default -> sender.sendMessage(prefix() + ChatColor.RED + "Unknown subcommand. Usage: /outlawadmin <reload|event start [player]|event stop>");
        }
        return true;
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix() + ChatColor.RED + "Usage: /outlawadmin event <start [player]|stop>");
            return;
        }

        if (args[1].equalsIgnoreCase("start")) {
            if (plugin.getHunterManager().isActive()) {
                sender.sendMessage(prefix() + ChatColor.RED + "A Hunter Event is already active.");
                return;
            }
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(prefix() + ChatColor.RED + "That player could not be found (must be online).");
                    return;
                }
            } else {
                plugin.getHunterManager().tryStartEvent();
                if (!plugin.getHunterManager().isActive()) {
                    sender.sendMessage(prefix() + ChatColor.RED + "Could not start an event - not enough online players with Wishes.");
                }
                return;
            }
            plugin.getHunterManager().forceStart(target);
            sender.sendMessage(prefix() + ChatColor.GREEN + "Started a Hunter Event targeting " + target.getName() + ".");
        } else if (args[1].equalsIgnoreCase("stop")) {
            if (!plugin.getHunterManager().isActive()) {
                sender.sendMessage(prefix() + ChatColor.RED + "No Hunter Event is active.");
                return;
            }
            plugin.getHunterManager().forceStop("stopped by an admin.");
            sender.sendMessage(prefix() + ChatColor.GREEN + "Hunter Event stopped.");
        } else {
            sender.sendMessage(prefix() + ChatColor.RED + "Usage: /outlawadmin event <start [player]|stop>");
        }
    }
}
