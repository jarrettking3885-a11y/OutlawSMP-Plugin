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
 * Handles loading, caching, and saving player data.
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
     * Added for LeaderboardManager.
     */
    public Collection<PlayerData> getAllCached() {
        return cache.values();
    }

    public void loadPlayer(UUID uuid, String name, Consumer<PlayerData> onLoaded) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            PlayerData data = databaseManager.loadPlayer(uuid, name);

            if (data == null) {
                int startingCoins = plugin.getConfig().getInt("starting-coins", 0);

                data = new PlayerData(
                        uuid,
                        name,
                        startingCoins,
                        new Bounty()
                );

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

    public void savePlayer(PlayerData data) {
        if (data == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> databaseManager.savePlayer(data)
        );
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);

        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(
                    plugin,
                    () -> databaseManager.savePlayer(data)
            );
        }
    }

    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            databaseManager.savePlayer(data);
        }
    }
}
