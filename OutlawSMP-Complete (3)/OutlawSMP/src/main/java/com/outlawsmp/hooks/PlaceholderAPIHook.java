package com.outlawsmp.hooks;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI integration for OutlawSMP.
 * Placeholders: %outlawsmp_wishes%, %outlawsmp_coins%, etc.
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final OutlawSMP plugin;

    public PlaceholderAPIHook(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "outlawsmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "OutlawSMP Team";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerManager().getCached(player);
        if (data == null) return "Loading...";

        return switch (params.toLowerCase()) {
            case "wishes" -> String.valueOf(data.getWishCount());
            case "coins" -> String.valueOf(data.getCoins());
            case "bounty" -> String.valueOf(data.getBounty().getAmount());
            case "streak" -> String.valueOf(data.getBounty().getKillStreak());
            case "threat" -> String.format("%.1f", calculateThreat(data));
            default -> "Invalid";
        };
    }

    private double calculateThreat(PlayerData data) {
        return (data.getWishCount() * 5.0) + (data.getBounty().getAmount() * 1.0) + (data.getBounty().getKillStreak() * 3.0);
    }
}
