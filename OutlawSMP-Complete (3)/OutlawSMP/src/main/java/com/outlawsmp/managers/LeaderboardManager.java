package com.outlawsmp.managers;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Leaderboards for Hunter Events and overall stats.
 */
public class LeaderboardManager {

    private final OutlawSMP plugin;

    public LeaderboardManager(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    /**
     * Hunter Event specific: Top survivors
     */
    public List<LeaderboardEntry> getTopHunterSurvivals(int limit) {
        List<PlayerData> all = new ArrayList<>(
                plugin.getPlayerManager().getAllCached()
        );

        return all.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getHunterSurvivals(),
                        a.getHunterSurvivals()
                ))
                .limit(limit)
                .map(p -> new LeaderboardEntry(
                        p.getName(),
                        p.getHunterSurvivals(),
                        "survivals"
                ))
                .collect(Collectors.toList());
    }

    public void showHunterLeaderboard(Player player, String type) {
        player.sendMessage(ChatColor.GOLD + "=== Hunter Event Leaderboard ===");

        List<LeaderboardEntry> leaderboard = getTopHunterSurvivals(10);

        if (leaderboard.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No hunter data yet.");
            return;
        }

        int place = 1;
        for (LeaderboardEntry entry : leaderboard) {
            player.sendMessage(
                    ChatColor.YELLOW + "#" + place +
                    " " + ChatColor.WHITE + entry.name() +
                    ChatColor.GRAY + " - " +
                    ChatColor.GREEN + entry.value()
            );
            place++;
        }
    }

    public record LeaderboardEntry(String name, int value, String category) {}
}
