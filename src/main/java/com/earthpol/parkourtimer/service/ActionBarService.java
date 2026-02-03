package com.earthpol.parkourtimer.service;

import com.earthpol.parkourtimer.ParkourTimer;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import com.earthpol.parkourtimer.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ActionBarService {

    private final ParkourTimer plugin;
    private final ParkourTimerManager timerManager;
    private final long maxTimeMillis;
    private final String actionBarFormat;
    private final String timeoutMessage;

    public ActionBarService(ParkourTimer plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.maxTimeMillis = plugin.getConfig().getLong("max_time", 300) * 1000;
        this.actionBarFormat = plugin.getConfig().getString("messages.actionbar", "&aTime: {time}");
        this.timeoutMessage = plugin.getConfig().getString("messages.timeout", "&cYou took too long!");
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            for (UUID uuid : timerManager.getRunningPlayers()) {
                Player player = getOnlinePlayer(uuid);
                if (player == null) continue;

                long elapsed = timerManager.getElapsed(uuid);
                if (elapsed >= maxTimeMillis) handleTimeout(player, uuid);
                else sendActionBar(player, elapsed);
            }
        }, 0L, 2L);
    }

    // helpers

    private void handleTimeout(Player player, UUID uuid) {
        timerManager.stop(uuid);
        notifyPlayer(player, timeoutMessage);
        plugin.getParkourLogger().player(uuid, player.getName(), "TIMEOUT");
    }

    private void sendActionBar(Player player, long elapsed) {
        String time = TimeFormatter.format(elapsed);
        player.sendActionBar(color(actionBarFormat.replace("{time}", time)));
    }

    private void notifyPlayer(Player player, String message) {
        player.sendMessage(color(message));
        player.playSound(player.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1f, 1f);
    }

    private Player getOnlinePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return (player != null && player.isOnline()) ? player : null;
    }

    private Component color(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}