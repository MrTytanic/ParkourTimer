package com.earthpol.parkourtimer;

import com.earthpol.parkourtimer.db.Database;
import com.earthpol.parkourtimer.db.Schema;
import org.bukkit.command.PluginCommand;
import com.earthpol.parkourtimer.command.ParkourCommand;
import com.earthpol.parkourtimer.db.ParkourRepository;
import com.earthpol.parkourtimer.listener.ParkourListener;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkourTimer extends JavaPlugin {

    private ParkourTimerManager timerManager;
    private Database database;
    private ParkourRepository parkourRepository;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        timerManager = new ParkourTimerManager();

        database = new Database(
                getConfig().getString("database.host"),
                getConfig().getInt("database.port"),
                getConfig().getString("database.name"),
                getConfig().getString("database.user"),
                getConfig().getString("database.password")
        );

        Schema.init(database);
        parkourRepository = new ParkourRepository(this, database);

        getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        // register parkour commands
        if (getCommand("parkour") != null) {
            getCommand("parkour").setExecutor(new ParkourCommand(this));
        } else {
            getLogger().warning("parkour command not found!");
        }
        getLogger().info("ParkourTimer enabled!");
    }

    @Override
    public void onDisable() {
        if (database != null) database.shutdown();
    }

    public ParkourTimerManager getTimerManager() {
        return timerManager;
    }

    public ParkourRepository getParkourRepository() {
        return parkourRepository;
    }
}