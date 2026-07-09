package com.outlawsmp.player;

/**
 * Rarity tiers for blessings, along with the relative weight used when a
 * new Wish rolls a random blessing. Weights don't need to sum to 100; they're
 * just relative shares.
 */
public enum BlessingRarity {
    COMMON(60),
    RARE(25),
    EPIC(12),
    LEGENDARY(3);

    private final int weight;

    BlessingRarity(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
