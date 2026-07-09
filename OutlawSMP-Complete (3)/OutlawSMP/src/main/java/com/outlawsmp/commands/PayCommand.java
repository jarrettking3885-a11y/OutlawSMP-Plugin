package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final OutlawSMP plugin;

    public PayCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        PlayerData senderData = plugin.getPlayerManager().getCached(player);
        if (senderData == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Your data is still loading.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Player not found or offline.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(prefix() + ChatColor.RED + "You can't pay yourself.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix() + ChatColor.RED + "Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(prefix() + ChatColor.RED + "Amount must be positive.");
            return true;
        }

        if (!plugin.getEconomyManager().withdraw(senderData, amount)) {
            player.sendMessage(prefix() + ChatColor.RED + "You don't have enough coins.");
            return true;
        }

        PlayerData targetData = plugin.getPlayerManager().getCached(target);
        if (targetData != null) {
            plugin.getEconomyManager().deposit(targetData, amount);
        } // else offline pay? For now, assume online

        player.sendMessage(prefix() + ChatColor.GREEN + "Paid " + ChatColor.GOLD + amount + " coins to " + target.getName());
        target.sendMessage(prefix() + ChatColor.GREEN + player.getName() + " paid you " + ChatColor.GOLD + amount + " coins");

        return true;
    }
}
