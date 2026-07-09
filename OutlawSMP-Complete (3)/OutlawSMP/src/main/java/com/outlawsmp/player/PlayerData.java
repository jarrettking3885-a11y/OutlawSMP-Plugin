package com.outlawsmp.player;

import com.outlawsmp.models.Bounty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Full in-memory representation of a player's OutlawSMP state: their
 * Wishes, coins, bounty, and hunter event statistics.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private int coins;
    private final Bounty bounty;
    private final List<Wish> wishes = new ArrayList<>();

    /** Hunter Event statistics. */
    private int hunterSurvivals = 0;

    /** Ordered set of Wish IDs whose blessings are currently active. */
    private final Set<UUID> activeWishes = new LinkedHashSet<>();

    private boolean dirty = false;

    public PlayerData(UUID uuid, String name, int coins, Bounty bounty) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.bounty = bounty;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
        markDirty();
    }

    public Bounty getBounty() {
        return bounty;
    }

    public List<Wish> getWishes() {
        return wishes;
    }

    public int getWishCount() {
        return wishes.size();
    }

    public int getHunterSurvivals() {
        return hunterSurvivals;
    }

    public void addHunterSurvival() {
        hunterSurvivals++;
        markDirty();
    }

    public void setHunterSurvivals(int amount) {
        hunterSurvivals = Math.max(0, amount);
        markDirty();
    }

    public void addWish(Wish wish) {
        if (hasBlessing(wish.getBlessing())) {
            return;
        }

        wishes.add(wish);
        markDirty();
    }

    public boolean hasBlessing(Blessing blessing) {
        return wishes.stream().anyMatch(w -> w.getBlessing() == blessing);
    }

    public void removeWish(Wish wish) {
        wishes.remove(wish);
        activeWishes.remove(wish.getId());
        markDirty();
    }

    public Set<UUID> getActiveWishIds() {
        return activeWishes;
    }

    public List<Wish> getActiveWishes() {
        List<Wish> result = new ArrayList<>();

        for (Wish w : wishes) {
            if (activeWishes.contains(w.getId())) {
                result.add(w);
            }
        }

        return result;
    }

    public boolean isActive(Wish wish) {
        return activeWishes.contains(wish.getId());
    }

    public boolean activate(Wish wish, int maxActive) {
        if (!wishes.contains(wish)) {
            return false;
        }

        if (activeWishes.contains(wish.getId())) {
            return true;
        }

        if (activeWishes.size() >= maxActive) {
            return false;
        }

        activeWishes.add(wish.getId());
        markDirty();

        return true;
    }

    public void deactivate(Wish wish) {
        if (activeWishes.remove(wish.getId())) {
            markDirty();
        }
    }

    public void pruneActiveWishes() {
        activeWishes.removeIf(id ->
                wishes.stream().noneMatch(w -> w.getId().equals(id))
        );
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }
}
