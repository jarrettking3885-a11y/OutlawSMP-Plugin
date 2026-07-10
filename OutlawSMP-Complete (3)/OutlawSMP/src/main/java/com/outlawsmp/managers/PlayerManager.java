package com.outlawsmp.managers;

 
import com.outlawsmp.database.DatabaseManager;
import com.outlawsmp.models.Bounty;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
 
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
 
/**
 * Owns the in-memory cache of {@link PlayerData} and coordinates loading
 * from / saving to the {@link DatabaseManager}. All database I/O happens off
 * the main thread; callers get results back on the main thread via the
 * supplied callback.
 *
 * <p>Per-player database operations (load/save/unload) are chained through
 * {@link #pendingOps} so they always execute in the order they were
 * requested. Without this, a quick relog could race: the async save
 * triggered by a quit could still be in flight when the very next join's
 * async load starts, so the load could read stale pre-quit data, and the
 * quit's save could then land afterward and silently overwrite whatever the
 * new session had already done. Chaining per UUID means a save started by
 * a quit is always fully applied before the following join's load reads the
 * database, while different players' I/O still runs concurrently.</p>
 */
public class PlayerManager {
 
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final WishManager wishManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
 
    /** Tail of the pending-operation chain for each UUID currently mid-flight. */
    private final Map<UUID, CompletableFuture<Void>> pendingOps = new ConcurrentHashMap<>();
 
    /** Runs a task on Bukkit's async scheduler; used as the executor for chained ops. */
    private final Executor asyncExecutor = task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
 
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
     * Queues {@code task} to run asynchronously after any already-pending
     * database operation for {@code uuid} finishes, guaranteeing per-player
     * ordering without blocking operations for other players.
     */
    private CompletableFuture<Void> chain(UUID uuid, Runnable task) {
        CompletableFuture<Void>[] result = new CompletableFuture[1];
        pendingOps.compute(uuid, (id, previous) -> {
            CompletableFuture<Void> base = previous != null ? previous : CompletableFuture.completedFuture(null);
            CompletableFuture<Void> next = base.thenRunAsync(task, asyncExecutor);
            result[0] = next;
            return next;
        });
        CompletableFuture<Void> next = result[0];
        // Once this is the most recent op for the UUID and it's finished, clear
        // the entry so the map doesn't grow unboundedly for long-lived servers.
        next.whenComplete((v, ex) -> pendingOps.remove(uuid, next));
        return next;
    }
 
    /**
     * Loads a player's data asynchronously (creating a fresh record with
     * starting Wishes/coins if this is their first join), then hands the
     * result back on the main thread. Waits for any pending save for this
     * UUID (e.g. from a very recent quit) to finish first.
     */
    public void loadPlayer(UUID uuid, String name, Consumer<PlayerData> onLoaded) {
        chain(uuid, () -> {
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
 
    /**
     * Saves a player's data asynchronously, after any earlier pending
     * operation for this UUID. Safe to call even if nothing changed.
     */
    public void savePlayer(PlayerData data) {
        if (data == null) {
            return;
        }
        chain(data.getUuid(), () -> databaseManager.savePlayer(data));
    }
 
    /**
     * Saves and removes a player's data from the cache, typically on quit.
     * The save is chained like any other operation, so a fast rejoin's load
     * will wait for it to finish before reading the database.
     */
    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            chain(uuid, () -> databaseManager.savePlayer(data));
        }
    }
 
    /** Saves every currently cached player synchronously. Intended for use on plugin disable. */
    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            databaseManager.savePlayer(data);
        }
    }
}
