package com.outlawsmp.managers;

import com.outlawsmp.database.DatabaseManager;
import com.outlawsmp.player.PlayerData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Handles bounty growth and payout.
 *
 * Every PvP kill increases the killer's bounty (making high-kill-streak
 * players bigger targets). When a player with a bounty is killed, the killer
 * is paid coins equal to that bounty and it resets to zero.
 */
public class BountyManager {

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final DatabaseManager databaseManager;

    public BountyManager(JavaPlugin plugin, EconomyManager economyManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.databaseManager = databaseManager;
    }

    private int killBountyIncrease() {
        return plugin.getConfig().getInt("kill-bounty-increase", 5);
    }

    private int minimumKillPayout() {
        return plugin.getConfig().getInt("minimum-kill-payout", 0);
    }

    /**
     * Handles the bounty side-effects of a PvP kill: pays the killer the
     * victim's bounty, resets the victim's bounty, then grows the killer's
     * own bounty. Returns the amount of coins paid out to the killer.
     */
    public int handleKill(PlayerData killer, PlayerData victim) {
        int payout = Math.max(victim.getBounty().payout(), minimumKillPayout());
        victim.getBounty().addDeath();
        victim.getBounty().resetStreak();
        victim.markDirty();

        if (payout > 0) {
            economyManager.deposit(killer, payout);
        }

        killer.getBounty().increase(killBountyIncrease());
        killer.getBounty().addKill();
        killer.getBounty().incrementStreak();
        killer.markDirty();

        return payout;
    }

    /** Loads the current top bounties directly from the database (includes offline players). */
    public List<DatabaseManager.BountyRow> getLeaderboard(int limit) {
        return databaseManager.loadTopBounties(limit);
    }
}
