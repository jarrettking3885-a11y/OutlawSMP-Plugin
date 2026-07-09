package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.database.DatabaseManager;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BountyCommand implements CommandExecutor {

    private final OutlawSMP plugin;

    public BountyCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            showLeaderboard(sender);
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /bounty <player>");
                return true;
            }
            PlayerData data = plugin.getPlayerManager().getCached(player);
            if (data == null) {
                player.sendMessage(prefix() + ChatColor.RED + "Your data is still loading, try again in a moment.");
                return true;
            }
            player.sendMessage(prefix() + ChatColor.YELLOW + "Your bounty: " + ChatColor.RED
                    + data.getBounty().getAmount() + " coins");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix() + ChatColor.RED + "That player could not be found.");
            return true;
        }

        PlayerData data = plugin.getPlayerManager().getCached(target.getUniqueId());
        if (data == null) {
            data = plugin.getDatabaseManager().loadPlayer(target.getUniqueId(), target.getName() != null ? target.getName() : args[0]);
        }
        if (data == null) {
            sender.sendMessage(prefix() + ChatColor.RED + "No data found for that player yet.");
            return true;
        }

        sender.sendMessage(prefix() + ChatColor.YELLOW + data.getName() + "'s bounty: " + ChatColor.RED
                + data.getBounty().getAmount() + " coins");
        return true;
    }

    private void showLeaderboard(CommandSender sender) {
        List<DatabaseManager.BountyRow> rows = plugin.getBountyManager().getLeaderboard(10);
        sender.sendMessage(prefix() + ChatColor.GOLD + "Top Bounties:");
        if (rows.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  Nobody has an active bounty right now.");
            return;
        }
        int rank = 1;
        for (DatabaseManager.BountyRow row : rows) {
            sender.sendMessage(ChatColor.GRAY + "  #" + rank + " " + ChatColor.YELLOW + row.name()
                    + ChatColor.GRAY + " — " + ChatColor.RED + row.amount() + " coins");
            rank++;
        }
    }
}
