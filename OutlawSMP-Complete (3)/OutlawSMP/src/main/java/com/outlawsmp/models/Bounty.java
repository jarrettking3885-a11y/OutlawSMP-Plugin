package com.outlawsmp.models;

/**
 * A player's bounty state.
 *
 * The bounty amount grows every time its owner lands a PvP kill. When the
 * owner is killed by another player, the killer is paid coins equal to the
 * current amount and the bounty resets to zero.
 *
 * Also tracks the owner's current PvP kill streak (consecutive kills without
 * dying), which feeds into the Hunter Event's Threat Score alongside Wishes
 * and bounty.
 */
public class Bounty {

    private int amount;
    private int totalEarned;
    private int kills;
    private int deaths;
    private int killStreak;

    public Bounty() {
        this(0, 0, 0, 0, 0);
    }

    public Bounty(int amount, int totalEarned, int kills, int deaths, int killStreak) {
        this.amount = amount;
        this.totalEarned = totalEarned;
        this.kills = kills;
        this.deaths = deaths;
        this.killStreak = killStreak;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    /** Increases the bounty amount, used whenever the owner lands a kill. */
    public void increase(int by) {
        this.amount = Math.max(0, this.amount + by);
    }

    /** Pays out and resets the bounty, used whenever the owner is killed. */
    public int payout() {
        int paid = this.amount;
        this.totalEarned += paid;
        this.amount = 0;
        return paid;
    }

    public int getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(int totalEarned) {
        this.totalEarned = totalEarned;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public void incrementStreak() {
        this.killStreak++;
    }

    /** Called whenever the owner dies - a death always ends the streak. */
    public void resetStreak() {
        this.killStreak = 0;
    }

    public void setKillStreak(int killStreak) {
        this.killStreak = Math.max(0, killStreak);
    }
}
