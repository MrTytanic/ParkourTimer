package com.earthpol.parkourtimer.db;

import com.earthpol.parkourtimer.ParkourTimer;
import com.earthpol.parkourtimer.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;

public class ParkourRepository {

    private final ParkourTimer plugin;
    private final Database database;

    public ParkourRepository(ParkourTimer plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    // saves asynchronously + updates name always, time only if new PB
    public void saveRun(UUID uuid, String playerName, long timeMs) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = database.getConnection()) {
                // check player's record
                PreparedStatement select = conn.prepareStatement(
                        "SELECT time_ms FROM time_records WHERE uuid = ?"
                );
                select.setString(1, uuid.toString());
                ResultSet rs = select.executeQuery();

                if (rs.next()) {
                    long existingTime = rs.getLong("time_ms");

                    // always update player name in case they changed it
                    PreparedStatement updateName = conn.prepareStatement(
                            "UPDATE time_records SET player_name = ? WHERE uuid = ?"
                    );
                    updateName.setString(1, playerName);
                    updateName.setString(2, uuid.toString());
                    updateName.executeUpdate();

                    // update PB if new time is better
                    if (timeMs < existingTime) {
                        // new personal best
                        PreparedStatement updatePB = conn.prepareStatement(
                                "UPDATE time_records SET time_ms = ?, recorded_at = NOW() WHERE uuid = ?"
                        );
                        updatePB.setLong(1, timeMs);
                        updatePB.setString(2, uuid.toString());
                        updatePB.executeUpdate();

                        plugin.getLogger().info("New personal best for " + playerName + ": " + timeMs + "ms");

                        // personal best message
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                String rawMsg = plugin.getConfig().getString("messages.personal_best", "&6&lNEW PERSONAL BEST!");
                                String coloredMsg = ChatColor.translateAlternateColorCodes('&', rawMsg)
                                        .replace("{time}", TimeFormatter.formatLong(timeMs));
                                player.sendMessage(coloredMsg);
                            }
                        });
                    }

                } else {
                    // first-time record
                    PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO time_records (uuid, player_name, time_ms) VALUES (?, ?, ?)"
                    );
                    insert.setString(1, uuid.toString());
                    insert.setString(2, playerName);
                    insert.setLong(3, timeMs);
                    insert.executeUpdate();
                    plugin.getLogger().info("First record for " + playerName + ": " + timeMs + "ms");
                }

            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save parkour run for " + playerName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // clear player's record
    public void clearRecord(UUID uuid, Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = database.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM time_records WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();

                Bukkit.getScheduler().runTask(plugin, callback);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to clear parkour record for " + uuid + ": " + e.getMessage());
            }
        });
    }

    // fetches top number of leaderboard
    public void fetchLeaderboard(int limit, java.util.function.Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StringBuilder sb = new StringBuilder("§aParkour Leaderboard:\n");
            try (Connection conn = database.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT player_name, time_ms FROM time_records ORDER BY time_ms ASC LIMIT ?"
                );
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("player_name");
                    long time = rs.getLong("time_ms");

                    // use the new leaderboard format
                    String formattedTime = TimeFormatter.formatLeaderboard(time);

                    sb.append(rank).append(". ").append(name).append(" - ").append(formattedTime).append("\n");
                    rank++;
                }
            } catch (SQLException e) {
                sb.append("Failed to fetch leaderboard: ").append(e.getMessage());
            }

            String result = sb.toString();
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }
}