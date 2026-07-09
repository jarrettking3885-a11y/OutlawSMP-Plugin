package com.outlawsmp.database;

import com.outlawsmp.models.Bounty;
import com.outlawsmp.player.Blessing;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite-backed persistence for player data.
 *
 * A single {@link Connection} is kept open for the plugin's lifetime.
 * SQLite handles one writer at a time internally, and all callers of this
 * class are expected to be off the main thread (see PlayerManager), so we
 * simply synchronize on the connection to keep things safe if two async
 * tasks ever overlap.
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private final String fileName;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    /** Opens the SQLite connection and ensures the schema exists. Call once, synchronously, on enable. */
    public synchronized void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, fileName);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            createTables();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite JDBC driver not found on the classpath.", e);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to the OutlawSMP database.", e);
        }
    }

    public synchronized void disconnect() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close the OutlawSMP database cleanly.", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    coins INTEGER NOT NULL DEFAULT 0,
                    bounty INTEGER NOT NULL DEFAULT 0,
                    bounty_total_earned INTEGER NOT NULL DEFAULT 0,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    kill_streak INTEGER NOT NULL DEFAULT 0
                );
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS wishes (
                    id TEXT PRIMARY KEY,
                    owner_uuid TEXT NOT NULL,
                    blessing TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                );
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_wishes_owner ON wishes (owner_uuid);");
        }
        migrateSchema();
    }

    /**
     * Best-effort migration for databases created before a given column
     * existed. SQLite's ADD COLUMN throws if the column is already there, so
     * each attempt is wrapped individually and ignored on failure.
     */
    private void migrateSchema() {
        addColumnIfMissing("players", "kill_streak", "INTEGER NOT NULL DEFAULT 0");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";");
        } catch (SQLException e) {
            // Column already exists - nothing to do.
        }
    }

    /** Loads a player's row + wishes, returning null if the player has never been seen before. */
    public synchronized PlayerData loadPlayer(UUID uuid, String currentName) {
        if (connection == null) {
            return null;
        }
        try {
            PlayerData data = null;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT name, coins, bounty, bounty_total_earned, kills, deaths, kill_streak FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Bounty bounty = new Bounty(
                                rs.getInt("bounty"),
                                rs.getInt("bounty_total_earned"),
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("kill_streak")
                        );
                        data = new PlayerData(uuid, rs.getString("name"), rs.getInt("coins"), bounty);
                    }
                }
            }

            if (data == null) {
                return null;
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, blessing, active, created_at FROM wishes WHERE owner_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Blessing blessing = Blessing.valueOf(rs.getString("blessing"));
                        Wish wish = new Wish(
                                UUID.fromString(rs.getString("id")),
                                blessing,
                                uuid,
                                rs.getLong("created_at")
                        );
                        data.addWish(wish);
                        if (rs.getInt("active") == 1) {
                            data.getActiveWishIds().add(wish.getId());
                        }
                    }
                }
            }
            data.clearDirty();
            return data;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player " + uuid, e);
            return null;
        }
    }

    /** Full save of a player's row and their complete wish set. Safe to call repeatedly. */
    public synchronized void savePlayer(PlayerData data) {
        if (connection == null || data == null) {
            return;
        }
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO players (uuid, name, coins, bounty, bounty_total_earned, kills, deaths, kill_streak)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        name = excluded.name,
                        coins = excluded.coins,
                        bounty = excluded.bounty,
                        bounty_total_earned = excluded.bounty_total_earned,
                        kills = excluded.kills,
                        deaths = excluded.deaths,
                        kill_streak = excluded.kill_streak
                    """)) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getName());
                ps.setInt(3, data.getCoins());
                ps.setInt(4, data.getBounty().getAmount());
                ps.setInt(5, data.getBounty().getTotalEarned());
                ps.setInt(6, data.getBounty().getKills());
                ps.setInt(7, data.getBounty().getDeaths());
                ps.setInt(8, data.getBounty().getKillStreak());
                ps.executeUpdate();
            }

            try (PreparedStatement del = connection.prepareStatement("DELETE FROM wishes WHERE owner_uuid = ?")) {
                del.setString(1, data.getUuid().toString());
                del.executeUpdate();
            }

            List<Wish> wishes = data.getWishes();
            if (!wishes.isEmpty()) {
                try (PreparedStatement ins = connection.prepareStatement(
                        "INSERT INTO wishes (id, owner_uuid, blessing, active, created_at) VALUES (?, ?, ?, ?, ?)")) {
                    for (Wish wish : wishes) {
                        ins.setString(1, wish.getId().toString());
                        ins.setString(2, data.getUuid().toString());
                        ins.setString(3, wish.getBlessing().name());
                        ins.setInt(4, data.isActive(wish) ? 1 : 0);
                        ins.setLong(5, wish.getCreatedAt());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            connection.commit();
            data.clearDirty();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player " + data.getUuid(), e);
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to roll back a failed save.", rollbackEx);
            }
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to restore auto-commit mode.", e);
            }
        }
    }

    /** Returns the top N players by current bounty amount, as uuid/name/amount triples. */
    public synchronized List<BountyRow> loadTopBounties(int limit) {
        List<BountyRow> rows = new ArrayList<>();
        if (connection == null) {
            return rows;
        }
        String sql = "SELECT uuid, name, bounty FROM players WHERE bounty > 0 ORDER BY bounty DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new BountyRow(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getInt("bounty")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bounty leaderboard.", e);
        }
        return rows;
    }

    public record BountyRow(UUID uuid, String name, int amount) {
    }
}
