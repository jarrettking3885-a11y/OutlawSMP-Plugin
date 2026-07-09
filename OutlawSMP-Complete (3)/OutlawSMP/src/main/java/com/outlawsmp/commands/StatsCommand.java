package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final OutlawSMP plugin;

    public StatsCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerData data;
        String targetName;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /stats <player>");
                return true;
            }
            data = plugin.getPlayerManager().getCached(player);
            targetName = player.getName();
            if (data == null) {
                player.sendMessage(prefix() + ChatColor.RED + "Your data is still loading, try again in a moment.");
                return true;
            }
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(prefix() + ChatColor.RED + "That player could not be found.");
                return true;
            }
            targetName = target.getName() != null ? target.getName() : args[0];
            data = plugin.getPlayerManager().getCached(target.getUniqueId());
            if (data == null) {
                data = plugin.getDatabaseManager().loadPlayer(target.getUniqueId(), targetName);
            }
            if (data == null) {
                sender.sendMessage(prefix() + ChatColor.RED + "No data found for that player yet.");
                return true;
            }
        }

        sender.sendMessage(prefix() + ChatColor.GOLD + targetName + "'s Stats");
        sender.sendMessage(ChatColor.GRAY + "  Wishes: " + ChatColor.LIGHT_PURPLE + data.getWishCount()
                + ChatColor.GRAY + " (" + data.getActiveWishes().size() + "/" + plugin.getWishManager().maxActiveBlessings() + " active)");
        sender.sendMessage(ChatColor.GRAY + "  Coins: " + ChatColor.GOLD + data.getCoins());
        sender.sendMessage(ChatColor.GRAY + "  Bounty: " + ChatColor.RED + data.getBounty().getAmount());
        sender.sendMessage(ChatColor.GRAY + "  Kills: " + ChatColor.GREEN + data.getBounty().getKills()
                + ChatColor.GRAY + "  Deaths: " + ChatColor.RED + data.getBounty().getDeaths());
        sender.sendMessage(ChatColor.GRAY + "  Kill Streak: " + ChatColor.GOLD + data.getBounty().getKillStreak());
        sender.sendMessage(ChatColor.GRAY + "  Total bounty earned: " + ChatColor.GOLD + data.getBounty().getTotalEarned());
        return true;
    }
}
