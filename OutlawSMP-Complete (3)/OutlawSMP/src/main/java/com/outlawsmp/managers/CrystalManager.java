package com.outlawsmp.managers;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.Wish;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wish Crystals - Random world spawns that create PvP hotspots.
 * First to claim gets rewards: coins, Wish Token, rare loot.
 */
public class CrystalManager {

    private final OutlawSMP plugin;
    private final NamespacedKey crystalKey;
    private final Random random = new Random();

    private BukkitTask spawnTask;
    private BossBar activeBar;

    public CrystalManager(OutlawSMP plugin) {
        this.plugin = plugin;
        this.crystalKey = new NamespacedKey(plugin, "wish_crystal");
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("wish-crystals.enabled", true);
    }

    private int spawnIntervalMinutes() {
        return plugin.getConfig().getInt("wish-crystals.spawn-interval-minutes", 45);
    }

    private int crystalLifetimeMinutes() {
        return plugin.getConfig().getInt("wish-crystals.lifetime-minutes", 15);
    }

    /** Starts crystal spawning scheduler. Call on enable. */
    public void start() {
        if (!enabled() || spawnTask != null) return;
        long period = spawnIntervalMinutes() * 60L * 20L;
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::trySpawnCrystal, period, period);
    }

    public void stop() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        if (activeBar != null) {
            activeBar.removeAll();
            activeBar = null;
        }
    }

    private void trySpawnCrystal() {
        if (!enabled()) return;

        World world = Bukkit.getWorlds().get(0); // Main world
        if (world == null) return;

        // Random location (configurable bounds in future)
        int x = random.nextInt(2000) - 1000;
        int z = random.nextInt(2000) - 1000;
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location loc = new Location(world, x, y, z);

        // Spawn beacon-like crystal
        ItemStack crystal = createCrystalItem();
        world.dropItemNaturally(loc, crystal);

        // Effects
        world.spawnParticle(Particle.END_ROD, loc, 50, 0.5, 0.5, 0.5, 0.1);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.0f);

        // Broadcast
        String msg = plugin.getConfig().getString("messages.crystal-spawn", "&c&lA Wish Crystal has spawned at &eX:%d Y:%d Z:%d&c&l!");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            String.format(msg, x, y, z)));

        // Boss bar
        activeBar = Bukkit.createBossBar("§cWish Crystal Active", BarColor.PURPLE, BarStyle.SOLID);
        activeBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) activeBar.addPlayer(p);

        // Auto-despawn after lifetime
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeBar != null) {
                activeBar.removeAll();
                activeBar = null;
            }
        }, crystalLifetimeMinutes() * 60L * 20L);
    }

    private ItemStack createCrystalItem() {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "§lWish Crystal");
            meta.setLore(List.of(
                ChatColor.GRAY + "First to claim wins big rewards!",
                ChatColor.DARK_GRAY + "PvP hotspot incoming..."
            ));
            meta.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCrystal(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(crystalKey, PersistentDataType.BYTE);
    }

    /** Reward the player who claims it. Integrate with listeners. */
    public void claim(Player claimer) {
        PlayerData data = plugin.getPlayerManager().getCached(claimer);
        if (data == null) return;

        // Rewards
        plugin.getEconomyManager().deposit(data, 300); // coins

        // Give Wish Token (random blessing)
        Blessing randomBlessing = plugin.getWishManager().rollRandomBlessing();
        Wish tempWish = Wish.create(randomBlessing, claimer.getUniqueId());
        plugin.getWishManager().giveToken(claimer, tempWish);

        // Effects
        claimer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, claimer.getLocation(), 30);
        claimer.playSound(claimer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&a" + claimer.getName() + " claimed a Wish Crystal! (+300 coins + Wish Token)"));

        if (activeBar != null) {
            activeBar.removeAll();
            activeBar = null;
        }
    }
}
