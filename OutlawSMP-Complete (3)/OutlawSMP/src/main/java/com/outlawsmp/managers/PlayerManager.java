package com.outlawsmp.managers;

import com.outlawsmp.database.DatabaseManager;
import com.outlawsmp.models.Bounty;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Owns the in-memory cache of {@link PlayerData} and coordinates loading
 * from / saving to the {@link DatabaseManager}. All database I/O happens off
 * the main thread; callers get results back on the main thread via the
 * supplied callback.
 */
public class PlayerManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final WishManager wishManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerManager(JavaPlugin plugin, DatabaseManager databaseManager, WishManager wishManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.wishManager = wishManager;
    }

    /** Already-loaded data for an online player, or null if not loaded (shouldn't happen post-join). */
    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getCached(Player player) {
        return cache.get(player.getUniqueId());
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void cache(PlayerData data) {
        cache.put(data.getUuid(), data);
    }

    public void uncache(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Loads a player's data asynchronously (creating a fresh record with
     * starting Wishes/coins if this is their first join), then hands the
     * result back on the main thread.
     */
    public void loadPlayer(UUID uuid, String name, Consumer<PlayerData> onLoaded) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = databaseManager.loadPlayer(uuid, name);
            if (data == null) {
                int startingCoins = plugin.getConfig().getInt("starting-coins", 0);
                data = new PlayerData(uuid, name, startingCoins, new Bounty());
                wishManager.grantStartingWishes(data);
                databaseManager.savePlayer(data);
            } else {
                data.setName(name);
                data.pruneActiveWishes();
            }

            PlayerData finalData = data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                cache(finalData);
                if (onLoaded != null) {
                    onLoaded.accept(finalData);
                }
            });
        });
    }

    /** Saves a player's data asynchronously. Safe to call even if nothing changed. */
    public void savePlayer(PlayerData data) {
        if (data == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(data));
    }

    /** Saves and removes a player's data from the cache, typically on quit. */
    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(data));
        }
    }

    /** Saves every currently cached player synchronously. Intended for use on plugin disable. */
    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            databaseManager.savePlayer(data);
        }
    }
}
