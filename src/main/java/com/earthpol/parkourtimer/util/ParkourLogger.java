package com.earthpol.parkourtimer.util;

import com.earthpol.parkourtimer.ParkourTimer;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;

public class ParkourLogger {

    private final ParkourTimer plugin;
    private final boolean debug;

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParkourLogger(ParkourTimer plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    /* ---------- Public API ---------- */

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void error(String message, Throwable t) {
        log(Level.SEVERE, message + " | " + t.getMessage());
        if (debug) t.printStackTrace();
    }

    public void player(UUID uuid, String playerName, String action) {
        log(Level.INFO, formatPlayer(uuid, playerName, action));
    }

    /* ---------- Internal ---------- */

    private void log(Level level, String message) {
        String time = LocalDateTime.now().format(FORMAT);
        String formatted = "[" + time + "] " + message;

        // If plugin is disabled, DO NOT try to schedule async tasks
        if (!plugin.isEnabled()) {
            plugin.getLogger().log(level, formatted);
            return;
        }

        // Normal async logging while plugin is running
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getLogger().log(level, formatted)
        );
    }

    private String formatPlayer(UUID uuid, String name, String action) {
        return "Player=" + name +
                " UUID=" + uuid +
                " Action=" + action;
    }
}
