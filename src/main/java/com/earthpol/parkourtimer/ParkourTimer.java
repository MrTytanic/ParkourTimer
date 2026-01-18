package com.earthpol.parkourtimer;

import com.earthpol.parkourtimer.listener.ParkourListener;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkourTimer extends JavaPlugin {

    private ParkourTimerManager timerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        timerManager = new ParkourTimerManager();

        getServer().getPluginManager().registerEvents(
                new ParkourListener(this), this
        );

        getLogger().info("ParkourTimer enabled!");
    }

    public ParkourTimerManager getTimerManager() {
        return timerManager;
    }
}