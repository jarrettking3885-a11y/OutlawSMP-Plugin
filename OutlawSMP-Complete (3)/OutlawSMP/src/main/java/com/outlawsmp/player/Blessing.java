package com.outlawsmp.player;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * Every blessing a Wish can permanently own.
 *
 * Blessings are applied/removed live as a player's active-blessing set
 * changes. Potion-effect based blessings use an "infinite" duration so they
 * persist without needing to be re-applied every tick; attribute based
 * blessings (extra hearts) use a namespaced {@link AttributeModifier} so they
 * can be cleanly added and removed by key.
 */
public enum Blessing {

    // ----- Common -----
    NIGHT_VISION(BlessingRarity.COMMON, "Night Vision") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.NIGHT_VISION, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.NIGHT_VISION); }
    },
    WATER_BREATHING(BlessingRarity.COMMON, "Water Breathing") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.WATER_BREATHING, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.WATER_BREATHING); }
    },
    LUCK(BlessingRarity.COMMON, "Luck") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.LUCK, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.LUCK); }
    },

    // ----- Rare -----
    SPEED_1(BlessingRarity.RARE, "Speed I") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.SPEED, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.SPEED); }
    },
    HASTE_1(BlessingRarity.RARE, "Haste I") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.HASTE, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.HASTE); }
    },
    FIRE_RESISTANCE(BlessingRarity.RARE, "Fire Resistance") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.FIRE_RESISTANCE, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE); }
    },

    // ----- Epic -----
    EXTRA_HEARTS_2(BlessingRarity.EPIC, "+2 Hearts") {
        @Override public void apply(Player p) { applyHearts(p, this, 4.0); }
        @Override public void remove(Player p) { removeHearts(p, this); }
    },
    RESISTANCE_1(BlessingRarity.EPIC, "Resistance I") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.RESISTANCE, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.RESISTANCE); }
    },
    JUMP_BOOST_2(BlessingRarity.EPIC, "Jump Boost II") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.JUMP_BOOST, 1); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.JUMP_BOOST); }
    },

    // ----- Legendary -----
    REGENERATION_1(BlessingRarity.LEGENDARY, "Regeneration I") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.REGENERATION, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.REGENERATION); }
    },
    EXTRA_HEARTS_4(BlessingRarity.LEGENDARY, "+4 Hearts") {
        @Override public void apply(Player p) { applyHearts(p, this, 8.0); }
        @Override public void remove(Player p) { removeHearts(p, this); }
    },
    STRENGTH_1(BlessingRarity.LEGENDARY, "Strength I") {
        @Override public void apply(Player p) { applyPotion(p, PotionEffectType.STRENGTH, 0); }
        @Override public void remove(Player p) { p.removePotionEffect(PotionEffectType.STRENGTH); }
    };

    private final BlessingRarity rarity;
    private final String displayName;

    Blessing(BlessingRarity rarity, String displayName) {
        this.rarity = rarity;
        this.displayName = displayName;
    }

    public BlessingRarity getRarity() {
        return rarity;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Applies this blessing's live effect to the player. */
    public abstract void apply(Player p);

    /** Removes this blessing's live effect from the player. */
    public abstract void remove(Player p);

    private static void applyPotion(Player p, PotionEffectType type, int amplifier) {
        // Effectively-permanent duration; refreshed on join so it never
        // actually expires while the blessing stays active.
        p.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, false));
    }

    private static NamespacedKey heartsKey(Blessing blessing) {
        return new NamespacedKey("outlawsmp", "hearts_" + blessing.name().toLowerCase(Locale.ROOT));
    }

    private static void applyHearts(Player p, Blessing blessing, double amount) {
        AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        NamespacedKey key = heartsKey(blessing);
        // Remove any stale modifier with this key first so re-applying (e.g.
        // on join) never stacks duplicates.
        maxHealth.getModifiers().stream()
                .filter(mod -> mod.getKey() != null && mod.getKey().equals(key))
                .findFirst()
                .ifPresent(maxHealth::removeModifier);

        AttributeModifier modifier = new AttributeModifier(
                key,
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        );
        maxHealth.addModifier(modifier);
    }

    private static void removeHearts(Player p, Blessing blessing) {
        AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        NamespacedKey key = heartsKey(blessing);
        maxHealth.getModifiers().stream()
                .filter(mod -> mod.getKey() != null && mod.getKey().equals(key))
                .findFirst()
                .ifPresent(maxHealth::removeModifier);
    }

    public static Blessing[] byRarity(BlessingRarity rarity) {
        return java.util.Arrays.stream(values())
                .filter(b -> b.rarity == rarity)
                .toArray(Blessing[]::new);
    }
}
