package com.outlawsmp.listeners;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final OutlawSMP plugin;

    public PlayerDeathListener(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        PlayerData victimData = plugin.getPlayerManager().getCached(victim);
        if (victimData == null) {
            return;
        }

        if (killer == null || killer.equals(victim)) {
            // Non-PvP death: no wish steal, no bounty payout, bounty is
            // preserved - but any death still ends a kill streak.
            victimData.getBounty().resetStreak();
            victimData.markDirty();
            return;
        }

        PlayerData killerData = plugin.getPlayerManager().getCached(killer);
        if (killerData == null) {
            return;
        }

        // Bounty payout + growth (also updates kill streaks).
        int payout = plugin.getBountyManager().handleKill(killerData, victimData);
        if (payout > 0) {
            killer.sendMessage(prefix() + ChatColor.GREEN + "You collected " + ChatColor.GOLD + payout + " coins"
                    + ChatColor.GREEN + " for killing " + ChatColor.YELLOW + victim.getName() + ChatColor.GREEN + "'s bounty!");
        }

        // Any unredeemed Wish Tokens the victim was already carrying transfer
        // straight to the killer instead of scattering on the ground.
        int stolenTokens = plugin.getWishManager().transferCarriedTokens(event, killer);
        if (stolenTokens > 0) {
            killer.sendMessage(prefix() + ChatColor.LIGHT_PURPLE + "You seized " + stolenTokens
                    + " unredeemed Wish Token(s) " + victim.getName() + " was carrying!");
            victim.sendMessage(prefix() + ChatColor.RED + killer.getName() + " took " + stolenTokens
                    + " of your unredeemed Wish Token(s)!");
        }

        // Wish stealing: the victim loses exactly one Wish immediately; the
        // killer only gets an unredeemed token for it - they have to make it
        // back to the Wish Shrine alive to actually gain the blessing.
        Wish taken = plugin.getWishManager().takeWishFromVictim(victimData, victim);
        if (taken != null) {
            plugin.getWishManager().giveToken(killer, taken);
            killer.sendMessage(prefix() + ChatColor.LIGHT_PURPLE + "You received an unredeemed Wish ("
                    + taken.getBlessing().getDisplayName() + ChatColor.LIGHT_PURPLE + ") from " + ChatColor.YELLOW
                    + victim.getName() + ChatColor.LIGHT_PURPLE + "! Get to the Wish Shrine before someone takes it from you.");
            victim.sendMessage(prefix() + ChatColor.RED + killer.getName() + " stole one of your Wishes ("
                    + taken.getBlessing().getDisplayName() + ")!");

            if (victimData.getWishCount() == 0) {
                victim.sendMessage(prefix() + ChatColor.RED + "You have no Wishes left. All blessings lost until you steal one back.");
            }
        }

        // Hunter Event: if the victim was The Hunted, this kill earns an extra bonus
        // on top of the normal bounty payout and Wish steal handled above.
        boolean victimWasHunted = victim.getUniqueId().equals(plugin.getHunterManager().getHuntedId());
        if (victimWasHunted) {
            plugin.getHunterManager().handleHuntedKilled(killer, killerData);
        }

        killerData.markDirty();
        victimData.markDirty();
    }
}
