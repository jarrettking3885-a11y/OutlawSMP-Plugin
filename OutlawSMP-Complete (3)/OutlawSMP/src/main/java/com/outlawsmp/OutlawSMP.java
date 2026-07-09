package com.outlawsmp;

import com.outlawsmp.commands.BalanceCommand;
import com.outlawsmp.commands.BountyCommand;
import com.outlawsmp.commands.HunterCommand;
import com.outlawsmp.commands.OutlawAdminCommand;
import com.outlawsmp.commands.PayCommand;
import com.outlawsmp.commands.StatsCommand;
import com.outlawsmp.commands.WishCommand;

import com.outlawsmp.database.DatabaseManager;

import com.outlawsmp.gui.BlessingsGUI;

import com.outlawsmp.hooks.PlaceholderAPIHook;

import com.outlawsmp.listeners.PlayerDeathListener;
import com.outlawsmp.listeners.PlayerInteractListener;
import com.outlawsmp.listeners.PlayerJoinListener;
import com.outlawsmp.listeners.PlayerQuitListener;

import com.outlawsmp.managers.BountyManager;
import com.outlawsmp.managers.CrystalManager;
import com.outlawsmp.managers.EconomyManager;
import com.outlawsmp.managers.HunterManager;
import com.outlawsmp.managers.PlayerManager;
import com.outlawsmp.managers.WishManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OutlawSMP extends JavaPlugin {

    private DatabaseManager databaseManager;
    private WishManager wishManager;
    private EconomyManager economyManager;
    private BountyManager bountyManager;
    private PlayerManager playerManager;
    private HunterManager hunterManager;
    private BlessingsGUI blessingsGUI;
    private CrystalManager crystalManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this, getConfig().getString("database.file", "outlawsmp.db"));
        databaseManager.connect();

        wishManager = new WishManager(this);
        economyManager = new EconomyManager();
        bountyManager = new BountyManager(this, economyManager, databaseManager);
        playerManager = new PlayerManager(this, databaseManager, wishManager);
        hunterManager = new HunterManager(this);
        blessingsGUI = new BlessingsGUI(this);
        crystalManager = new CrystalManager(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);

        registerCommand("wish", new WishCommand(this));
        registerCommand("balance", new BalanceCommand(this));
        registerCommand("bounty", new BountyCommand(this));
        registerCommand("stats", new StatsCommand(this));
        registerCommand("hunter", new HunterCommand(this));
        registerCommand("outlawadmin", new OutlawAdminCommand(this));
        registerCommand("pay", new PayCommand(this));

        hunterManager.startScheduler();
        crystalManager.start();

        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        getLogger().info("OutlawSMP enabled!");
    }

    @Override
    public void onDisable() {
        if (hunterManager != null) {
            hunterManager.stopScheduler();
        }
        if (crystalManager != null) {
            crystalManager.stop();
        }
        if (playerManager != null) {
            // Synchronous save on shutdown - the scheduler is no longer
            // available for async tasks at this point.
            playerManager.saveAllSync();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("OutlawSMP disabled!");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public WishManager getWishManager() {
        return wishManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public HunterManager getHunterManager() {
        return hunterManager;
    }

    public BlessingsGUI getBlessingsGUI() {
        return blessingsGUI;
    }

    public CrystalManager getCrystalManager() {
        return crystalManager;
    }
}
