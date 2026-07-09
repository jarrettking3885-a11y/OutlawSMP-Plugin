package com.outlawsmp.managers;

import com.outlawsmp.player.PlayerData;

/**
 * Handles all coin (economy) operations. Coins are completely separate from
 * Wishes: they can be earned from bounties/events/bosses/selling items and
 * spent on food/blocks/cosmetics/utility items, but must never be able to
 * buy a Wish.
 */
public class EconomyManager {

    public int getBalance(PlayerData data) {
        return data.getCoins();
    }

    public void deposit(PlayerData data, int amount) {
        if (amount <= 0) {
            return;
        }
        data.setCoins(data.getCoins() + amount);
    }

    /** Attempts to withdraw coins. Returns false (no change) if the balance is insufficient. */
    public boolean withdraw(PlayerData data, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (data.getCoins() < amount) {
            return false;
        }
        data.setCoins(data.getCoins() - amount);
        return true;
    }

    public void setBalance(PlayerData data, int amount) {
        data.setCoins(amount);
    }
}
