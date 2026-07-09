package com.outlawsmp.listeners;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * Lets a player redeem their unredeemed Wish Tokens by right-clicking while
 * standing near the Wish Shrine, as an alternative to typing /wish redeem.
 */
public class PlayerInteractListener implements Listener {

    private final OutlawSMP plugin;

    public PlayerInteractListener(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        // Only the main-hand swing needs to trigger this, or every right
        // click fires twice (once per hand) and we'd redeem/spam-check twice.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getWishManager().isNearShrine(player.getLocation())) {
            return;
        }

        PlayerData data = plugin.getPlayerManager().getCached(player);
        if (data == null) {
            return;
        }

        List<Wish> redeemed = plugin.getWishManager().redeemAll(player, data);
        if (redeemed.isEmpty()) {
            return;
        }

        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
        player.sendMessage(prefix + ChatColor.GREEN + "Redeemed " + redeemed.size() + " Wish(es) at the Shrine:");
        for (Wish wish : redeemed) {
            player.sendMessage(ChatColor.GRAY + "  - " + wish.getBlessing().getDisplayName());
        }
        player.sendMessage(ChatColor.GRAY + "Use /wish activate <#> near spawn to turn one on.");
    }
}
