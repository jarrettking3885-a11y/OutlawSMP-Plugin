package com.outlawsmp.managers;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;

/**
 * Live Sidebar Scoreboard - Updates in real-time.
 */
public class ScoreboardManager {

    private final OutlawSMP plugin;
    private BukkitTask updateTask;

    public ScoreboardManager(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }

    public void start() {
        if (!enabled() || updateTask != null) return;
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 
            plugin.getConfig().getLong("scoreboard.update-interval-ticks", 20L));
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateForPlayer(p);
        }
    }

    public void updateForPlayer(Player player) {
        if (!enabled()) return;

        PlayerData data = plugin.getPlayerManager().getCached(player);
        if (data == null) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("outlawsmp", "dummy", ChatColor.GOLD + "⚔ OutlawSMP");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        setLine(obj, line--, ChatColor.WHITE + "   " + player.getName());
        setLine(obj, line--, "");
        setLine(obj, line--, ChatColor.LIGHT_PURPLE + "⭐ Wishes: " + ChatColor.WHITE + data.getWishCount());
        setLine(obj, line--, ChatColor.GOLD + "💰 Coins: " + ChatColor.WHITE + data.getCoins());
        setLine(obj, line--, ChatColor.RED + "🎯 Bounty: " + ChatColor.WHITE + data.getBounty().getAmount());
        setLine(obj, line--, ChatColor.GREEN + "🔥 Streak: " + ChatColor.WHITE + data.getBounty().getKillStreak());

        double threat = calculateThreat(data);
        setLine(obj, line--, ChatColor.YELLOW + "⚠ Threat: " + ChatColor.WHITE + String.format("%.1f", threat));

        if (plugin.getHunterManager().isActive()) {
            setLine(obj, line--, "");
            setLine(obj, line--, ChatColor.RED + "🎯 Hunted: " + ChatColor.WHITE + plugin.getHunterManager().getHuntedName());
            setLine(obj, line--, ChatColor.GOLD + "⏳ Time: " + ChatColor.WHITE + plugin.getHunterManager().getRemainingSeconds() + "s");
        }

        player.setScoreboard(board);
    }

    private double calculateThreat(PlayerData data) {
        return (data.getWishCount() * 5.0) + (data.getBounty().getAmount() * 1.0) + (data.getBounty().getKillStreak() * 3.0);
    }

    private void setLine(Objective obj, int score, String text) {
        obj.getScore(text).setScore(score);
    }

    public void onJoin(Player player) {
        updateForPlayer(player);
    }
}
