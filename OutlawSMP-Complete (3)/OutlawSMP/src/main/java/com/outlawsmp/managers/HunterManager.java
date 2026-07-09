package com.outlawsmp.managers;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The Hunter Event.
 *
 * Every {@code hunter-event.interval-minutes}, the online player with the
 * highest Threat Score (a mix of Wishes, bounty, and kill streak - see
 * {@link #threatScore}) becomes "The Hunted". Everyone online is given a
 * tracking compass pointed at them, and a broadcast announces the event. If
 * The Hunted survives the configured duration, they're rewarded. If anyone
 * kills them first, the killer gets a bonus on top of the normal bounty
 * payout and Wish steal that already happen on any PvP kill.
 */
public class HunterManager {

    private final OutlawSMP plugin;
    private final NamespacedKey compassKey;
    private final Random random = new Random();

    private UUID huntedId;
    private String huntedName;
    private long eventEndsAtMillis;
    private BukkitTask durationTask;
    private BukkitTask compassTask;
    private BukkitTask intervalTask;

    public HunterManager(OutlawSMP plugin) {
        this.plugin = plugin;
        this.compassKey = new NamespacedKey(plugin, "hunter_compass");
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("hunter-event.enabled", true);
    }

    private int intervalMinutes() {
        return plugin.getConfig().getInt("hunter-event.interval-minutes", 30);
    }

    private int durationMinutes() {
        return plugin.getConfig().getInt("hunter-event.duration-minutes", 10);
    }

    private int minimumOnlinePlayers() {
        return plugin.getConfig().getInt("hunter-event.minimum-online-players", 2);
    }

    private int surviveRewardCoins() {
        return plugin.getConfig().getInt("hunter-event.survive-reward-coins", 150);
    }

    private int killBonusCoins() {
        return plugin.getConfig().getInt("hunter-event.kill-bonus-coins", 250);
    }

    private long compassUpdateIntervalTicks() {
        return plugin.getConfig().getLong("hunter-event.compass-update-interval-ticks", 40L);
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    /** Starts the recurring scheduler that periodically attempts to trigger a Hunter Event. Call once on enable. */
    public void startScheduler() {
        if (intervalTask != null) {
            return;
        }
        long periodTicks = Math.max(1, intervalMinutes()) * 60L * 20L;
        intervalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tryStartEvent, periodTicks, periodTicks);
    }

    public void stopScheduler() {
        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
        }
    }

    public boolean isActive() {
        return huntedId != null;
    }

    public UUID getHuntedId() {
        return huntedId;
    }

    public String getHuntedName() {
        return huntedName;
    }

    public long getRemainingSeconds() {
        if (!isActive()) {
            return 0;
        }
        return Math.max(0, (eventEndsAtMillis - System.currentTimeMillis()) / 1000L);
    }

    private double threatWeightWishes() {
        return plugin.getConfig().getDouble("hunter-event.threat-weights.wishes", 5);
    }

    private double threatWeightBounty() {
        return plugin.getConfig().getDouble("hunter-event.threat-weights.bounty", 1);
    }

    private double threatWeightKillStreak() {
        return plugin.getConfig().getDouble("hunter-event.threat-weights.kill-streak", 3);
    }

    /** Threat Score = (Wishes * wishWeight) + (bounty * bountyWeight) + (killStreak * killStreakWeight). */
    private double threatScore(PlayerData data) {
        return (data.getWishCount() * threatWeightWishes())
                + (data.getBounty().getAmount() * threatWeightBounty())
                + (data.getBounty().getKillStreak() * threatWeightKillStreak());
    }

    /** Attempts to start a new event. Silently does nothing if conditions aren't met or one's already running. */
    public void tryStartEvent() {
        if (!enabled() || isActive()) {
            return;
        }
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        if (online.size() < minimumOnlinePlayers()) {
            return;
        }

        Player chosen = null;
        double bestScore = -1;
        for (Player p : online) {
            PlayerData data = plugin.getPlayerManager().getCached(p);
            if (data == null) {
                continue;
            }
            double score = threatScore(data);
            if (score <= 0) {
                continue;
            }
            if (score > bestScore || (score == bestScore && random.nextBoolean())) {
                bestScore = score;
                chosen = p;
            }
        }

        if (chosen == null) {
            return;
        }

        beginEvent(chosen);
    }

    /** Force-starts an event with a specific target, bypassing the automatic selection. Used by /outlawadmin. */
    public boolean forceStart(Player target) {
        if (isActive()) {
            return false;
        }
        beginEvent(target);
        return true;
    }

    private void beginEvent(Player hunted) {
        huntedId = hunted.getUniqueId();
        huntedName = hunted.getName();
        int duration = Math.max(1, durationMinutes());
        eventEndsAtMillis = System.currentTimeMillis() + duration * 60_000L;

        Bukkit.broadcastMessage(prefix() + ChatColor.RED + ChatColor.BOLD.toString() + "HUNTER EVENT! "
                + ChatColor.YELLOW + hunted.getName() + ChatColor.GOLD + " is the biggest threat on the server and is now "
                + ChatColor.RED + "THE HUNTED" + ChatColor.GOLD + "! Survive " + duration
                + " minutes, or hunt them down for huge rewards!");

        giveCompassToAll();

        durationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> endEvent(true), duration * 60L * 20L);
        compassTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateCompasses, 0L, compassUpdateIntervalTicks());
    }

    /** Called by PlayerDeathListener when The Hunted is killed. Awards the kill bonus and ends the event. */
    public void handleHuntedKilled(Player killer, PlayerData killerData) {
        if (!isActive()) {
            return;
        }
        int bonus = killBonusCoins();
        if (bonus > 0) {
            plugin.getEconomyManager().deposit(killerData, bonus);
        }
        Bukkit.broadcastMessage(prefix() + ChatColor.GOLD + killer.getName() + ChatColor.RED
                + " hunted down " + ChatColor.YELLOW + huntedName + ChatColor.RED + "! (+" + bonus + " bonus coins)");
        endEvent(false);
    }

    /** Called if The Hunted disconnects mid-event: cancels with no rewards to either side. */
    public void cancelDueToDisconnect() {
        forceStop(huntedName + " disconnected.");
    }

    /** Cancels the current event (if any) for an arbitrary reason, e.g. an admin command. No rewards are paid. */
    public void forceStop(String reason) {
        if (!isActive()) {
            return;
        }
        Bukkit.broadcastMessage(prefix() + ChatColor.GRAY + "The Hunter Event was cancelled - " + reason);
        endEvent(false);
    }

    private void endEvent(boolean survived) {
        if (!isActive()) {
            return;
        }
        if (durationTask != null) {
            durationTask.cancel();
            durationTask = null;
        }
        if (compassTask != null) {
            compassTask.cancel();
            compassTask = null;
        }

        if (survived) {
            Player hunted = Bukkit.getPlayer(huntedId);
            if (hunted != null && hunted.isOnline()) {
                PlayerData data = plugin.getPlayerManager().getCached(hunted);
                if (data != null) {
                    int reward = surviveRewardCoins();
                    plugin.getEconomyManager().deposit(data, reward);
                    hunted.sendMessage(prefix() + ChatColor.GREEN + "You survived the Hunter Event! +"
                            + reward + " coins.");
                }
            }
            Bukkit.broadcastMessage(prefix() + ChatColor.GREEN + huntedName + " survived the Hunter Event!");
        }

        removeCompassFromAll();
        huntedId = null;
        huntedName = null;
        eventEndsAtMillis = 0;
    }

    // --- Compass handling ---

    private ItemStack buildCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Hunter's Compass");
            meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE)) {
                return true;
            }
        }
        return false;
    }

    /** Gives the tracking compass to a single player. Used both at event start and for players who join mid-event. */
    public void giveCompassIfMissing(Player player) {
        if (!isActive() || hasCompass(player)) {
            return;
        }
        player.getInventory().addItem(buildCompass());
        Player hunted = Bukkit.getPlayer(huntedId);
        if (hunted != null) {
            player.setCompassTarget(hunted.getLocation());
        }
    }

    private void giveCompassToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveCompassIfMissing(p);
        }
    }

    private void removeCompassFromAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE)) {
                    p.getInventory().remove(item);
                }
            }
        }
    }

    private void updateCompasses() {
        if (!isActive()) {
            return;
        }
        Player hunted = Bukkit.getPlayer(huntedId);
        if (hunted == null || !hunted.isOnline()) {
            // The Hunted somehow went offline without the quit listener catching it; bail safely.
            endEvent(false);
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(huntedId)) {
                continue;
            }
            p.setCompassTarget(hunted.getLocation());
        }
    }
}
