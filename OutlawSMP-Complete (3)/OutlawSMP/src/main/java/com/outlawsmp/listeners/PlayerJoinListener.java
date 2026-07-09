package com.outlawsmp.listeners;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.managers.WishManager;
import com.outlawsmp.player.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final OutlawSMP plugin;

    public PlayerJoinListener(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        WishManager wishManager = plugin.getWishManager();
        plugin.getPlayerManager().loadPlayer(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                (PlayerData data) -> {
                    if (!event.getPlayer().isOnline()) {
                        // They disconnected again before load finished; save was already queued.
                        return;
                    }
                    wishManager.applyActiveBlessings(event.getPlayer(), data);
                    plugin.getHunterManager().giveCompassIfMissing(event.getPlayer());
                }
        );
    }
}
