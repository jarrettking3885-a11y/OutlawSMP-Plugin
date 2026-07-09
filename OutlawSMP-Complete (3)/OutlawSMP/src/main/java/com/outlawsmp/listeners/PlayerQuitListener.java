package com.outlawsmp.listeners;

import com.outlawsmp.OutlawSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final OutlawSMP plugin;

    public PlayerQuitListener(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(plugin.getHunterManager().getHuntedId())) {
            plugin.getHunterManager().cancelDueToDisconnect();
        }
        plugin.getPlayerManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
