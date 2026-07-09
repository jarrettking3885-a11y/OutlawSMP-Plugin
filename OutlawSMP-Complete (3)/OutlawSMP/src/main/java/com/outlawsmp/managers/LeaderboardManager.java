package com.outlawsmp.managers;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Leaderboards for Hunter Events and overall stats.
 */
public class LeaderboardManager {

    private final OutlawSMP plugin;

    public LeaderboardManager(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    /** Hunter Event specific: Top survivors & killers */
    public List<LeaderboardEntry> getTopHunterSurvivals(int limit) {
        // In full version, would persist hunter stats; for now, use cached + mock
        List<PlayerData> all = new ArrayList<>(plugin.getPlayerManager().getAllCached()); // assume helper
        return all.stream()
                .sorted((a, b) -> Integer.compare(b.getHunterSurvivals(), a.getHunterSurvivals())) // placeholder
                .limit(limit)
                .map(p -> new LeaderboardEntry(p.getName(), p.getHunterSurvivals(), "survivals"))
                .collect(Collectors.toList());
    }

    public void showHunterLeaderboard(Player player, String type) {
        player.sendMessage(ChatColor.GOLD + "=== Hunter Event Leaderboard ===");
        // Expand with real stats persistence later
        player.sendMessage(ChatColor.YELLOW + "Top Survivors coming soon!");
    }

    public record LeaderboardEntry(String name, int value, String category) {}
}
