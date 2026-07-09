package com.outlawsmp.managers;

import com.outlawsmp.player.Blessing;
import com.outlawsmp.player.BlessingRarity;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Owns all Wish-related gameplay logic: rolling new Wishes for new players,
 * activating/deactivating blessings near spawn, and the Wish Shrine flow.
 *
 * Wishes no longer change hands instantly on a kill. Killing a player takes
 * one of their Wishes away immediately, but the killer only receives an
 * "unredeemed Wish Token" item - a physical, stealable item that must be
 * carried back to the Wish Shrine at spawn and redeemed before it actually
 * becomes an owned Wish (and its blessing goes live). If the token carrier
 * dies before redeeming, whoever kills them receives the token instead.
 */
public class WishManager {

    private final JavaPlugin plugin;
    private final NamespacedKey tokenMarkerKey;
    private final NamespacedKey tokenIdKey;
    private final NamespacedKey tokenBlessingKey;
    private final Random random = new Random();

    public WishManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tokenMarkerKey = new NamespacedKey(plugin, "wish_token");
        this.tokenIdKey = new NamespacedKey(plugin, "wish_token_id");
        this.tokenBlessingKey = new NamespacedKey(plugin, "wish_token_blessing");
    }

    private int startingWishes() {
        return plugin.getConfig().getInt("starting-wishes", 3);
    }

    public int maxActiveBlessings() {
        return plugin.getConfig().getInt("max-active-blessings", 5);
    }

    private int blessingChangeRadius() {
        return plugin.getConfig().getInt("blessing-change-radius", 15);
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    // ---------------------------------------------------------------
    // Rolling / starting Wishes
    // ---------------------------------------------------------------

    /** Grants a brand-new player their starting Wishes with random blessings. These are pre-redeemed, no shrine trip needed. */
    public void grantStartingWishes(PlayerData data) {
        if (!data.getWishes().isEmpty()) {
            return;
        }
        int count = startingWishes();
        for (int i = 0; i < count; i++) {
            Wish wish = Wish.create(rollRandomBlessing(), data.getUuid());
            data.addWish(wish);
            // Auto-activate starting wishes up to the active-slot cap so new
            // players immediately feel their blessings.
            data.activate(wish, maxActiveBlessings());
        }
    }

    /** Weighted-random blessing roll based on {@link BlessingRarity} weights. */
    public Blessing rollRandomBlessing() {
        int totalWeight = 0;
        for (BlessingRarity rarity : BlessingRarity.values()) {
            totalWeight += rarity.getWeight();
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (BlessingRarity rarity : BlessingRarity.values()) {
            cumulative += rarity.getWeight();
            if (roll < cumulative) {
                Blessing[] options = Blessing.byRarity(rarity);
                return options[random.nextInt(options.length)];
            }
        }
        // Should be unreachable, but fall back to a safe default.
        return Blessing.NIGHT_VISION;
    }

    // ---------------------------------------------------------------
    // Kill -> token flow
    // ---------------------------------------------------------------

    /**
     * Removes exactly one Wish from the victim (if they have any), deactivating
     * its live effect if it was active. This is the "victim loses a Wish"
     * half of a kill - it happens immediately, independent of whether the
     * killer ever redeems the resulting token.
     */
    public Wish takeWishFromVictim(PlayerData victim, Player onlineVictim) {
        List<Wish> wishes = victim.getWishes();
        if (wishes.isEmpty()) {
            return null;
        }
        Wish taken = wishes.get(random.nextInt(wishes.size()));
        if (onlineVictim != null && victim.isActive(taken)) {
            taken.getBlessing().remove(onlineVictim);
        }
        victim.removeWish(taken);
        return taken;
    }

    /** Builds the physical, stealable "unredeemed Wish Token" item for a given Wish. */
    public ItemStack createToken(Wish wish) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor color = rarityColor(wish.getBlessing());
            meta.setDisplayName(color + "" + ChatColor.BOLD + "Unredeemed Wish");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Blessing: " + color + wish.getBlessing().getDisplayName(),
                    ChatColor.DARK_GRAY + "Carry this to the Wish Shrine at spawn",
                    ChatColor.DARK_GRAY + "and redeem it before someone takes it from you!"
            ));
            meta.getPersistentDataContainer().set(tokenMarkerKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(tokenIdKey, PersistentDataType.STRING, wish.getId().toString());
            meta.getPersistentDataContainer().set(tokenBlessingKey, PersistentDataType.STRING, wish.getBlessing().name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ChatColor rarityColor(Blessing blessing) {
        return switch (blessing.getRarity()) {
            case COMMON -> ChatColor.GRAY;
            case RARE -> ChatColor.AQUA;
            case EPIC -> ChatColor.LIGHT_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    public boolean isToken(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(tokenMarkerKey, PersistentDataType.BYTE);
    }

    private UUID tokenWishId(ItemStack item) {
        String raw = item.getItemMeta().getPersistentDataContainer().get(tokenIdKey, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    private Blessing tokenBlessing(ItemStack item) {
        String raw = item.getItemMeta().getPersistentDataContainer().get(tokenBlessingKey, PersistentDataType.STRING);
        return raw == null ? null : Blessing.valueOf(raw);
    }

    /** Gives a freshly-taken Wish to the killer as an unredeemed token, dropping it at their feet if their inventory is full. */
    public void giveToken(Player player, Wish wish) {
        ItemStack token = createToken(wish);
        var leftover = player.getInventory().addItem(token);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }

    /**
     * On death, any unredeemed Wish Tokens the victim was carrying are pulled
     * out of their normal death drops and handed directly to the killer,
     * instead of being left on the ground for anyone to grab. Returns how
     * many tokens were transferred.
     */
    public int transferCarriedTokens(PlayerDeathEvent event, Player killer) {
        int count = 0;
        Iterator<ItemStack> it = event.getDrops().iterator();
        List<ItemStack> stolen = new ArrayList<>();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (isToken(item)) {
                it.remove();
                stolen.add(item);
                count++;
            }
        }
        for (ItemStack token : stolen) {
            var leftover = killer.getInventory().addItem(token);
            for (ItemStack overflow : leftover.values()) {
                killer.getWorld().dropItemNaturally(killer.getLocation(), overflow);
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Wish Shrine
    // ---------------------------------------------------------------

    private boolean shrineEnabled() {
        return plugin.getConfig().getBoolean("wish-shrine.enabled", true);
    }

    /** Resolves the configured shrine location, or null if its world isn't loaded / shrine is disabled. */
    public Location getShrineLocation() {
        if (!shrineEnabled()) {
            return null;
        }
        String worldName = plugin.getConfig().getString("wish-shrine.world", "world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = plugin.getConfig().getDouble("wish-shrine.x", 0);
        double y = plugin.getConfig().getDouble("wish-shrine.y", 64);
        double z = plugin.getConfig().getDouble("wish-shrine.z", 0);
        return new Location(world, x, y, z);
    }

    private double shrineRadius() {
        return plugin.getConfig().getDouble("wish-shrine.radius", 4);
    }

    public boolean isNearShrine(Location location) {
        Location shrine = getShrineLocation();
        if (shrine == null) {
            return false;
        }
        if (!shrine.getWorld().equals(location.getWorld())) {
            return false;
        }
        double radius = shrineRadius();
        return location.distanceSquared(shrine) <= radius * radius;
    }

    /**
     * Redeems every unredeemed Wish Token in the player's inventory: removes
     * the token items, adds the corresponding Wishes to their owned set, and
     * reports what was redeemed. Blessings stay inactive until manually
     * activated with /wish activate, same as any other newly-owned Wish.
     */
    public List<Wish> redeemAll(Player player, PlayerData data) {
        List<Wish> redeemed = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (!isToken(item)) {
                continue;
            }
            UUID wishId = tokenWishId(item);
            Blessing blessing = tokenBlessing(item);
            if (wishId == null || blessing == null) {
                continue;
            }
            Wish wish = new Wish(wishId, blessing, data.getUuid(), System.currentTimeMillis());
            data.addWish(wish);
            redeemed.add(wish);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);

            if (blessing.getRarity() == BlessingRarity.EPIC || blessing.getRarity() == BlessingRarity.LEGENDARY) {
                plugin.getServer().broadcastMessage(prefix() + rarityColor(blessing) + player.getName()
                        + " redeemed a " + blessing.getRarity().name() + " Wish (" + blessing.getDisplayName() + ") at the Shrine!");
            }

            int amount = item.getAmount();
            if (amount <= 1) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(amount - 1);
            }
        }
        return redeemed;
    }

    // ---------------------------------------------------------------
    // Blessing activation
    // ---------------------------------------------------------------

    /** Whether the given player is within the configured distance of their world's spawn. */
    public boolean isNearSpawn(Player player) {
        int radius = blessingChangeRadius();
        if (radius < 0) {
            return true;
        }
        World world = player.getWorld();
        Location spawn = world.getSpawnLocation();
        return player.getLocation().distanceSquared(spawn) <= (double) radius * radius;
    }

    /** Applies every active blessing's live effect to the player. Call on join/respawn. */
    public void applyActiveBlessings(Player player, PlayerData data) {
        for (Wish wish : data.getActiveWishes()) {
            wish.getBlessing().apply(player);
        }
    }

    /** Removes every active blessing's live effect from the player. Call on quit. */
    public void clearActiveBlessings(Player player, PlayerData data) {
        for (Wish wish : data.getActiveWishes()) {
            wish.getBlessing().remove(player);
        }
    }

    public Wish findWish(PlayerData data, UUID wishId) {
        return data.getWishes().stream()
                .filter(w -> w.getId().equals(wishId))
                .findFirst()
                .orElse(null);
    }
}
