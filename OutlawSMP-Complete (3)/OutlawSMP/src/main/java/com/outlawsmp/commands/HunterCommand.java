package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.managers.HunterManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HunterCommand implements CommandExecutor {

    private final OutlawSMP plugin;

    public HunterCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        HunterManager hunterManager = plugin.getHunterManager();
        if (!hunterManager.isActive()) {
            sender.sendMessage(prefix() + ChatColor.GRAY + "No Hunter Event is active right now.");
            return true;
        }

        long remaining = hunterManager.getRemainingSeconds();
        long minutes = remaining / 60;
        long seconds = remaining % 60;

        sender.sendMessage(prefix() + ChatColor.RED + "The Hunted: " + ChatColor.YELLOW + hunterManager.getHuntedName());
        sender.sendMessage(prefix() + ChatColor.GRAY + "Time remaining: " + ChatColor.GOLD
                + String.format("%d:%02d", minutes, seconds));
        return true;
    }
}
