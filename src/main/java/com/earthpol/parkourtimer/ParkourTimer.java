package com.earthpol.parkourtimer;

import com.earthpol.parkourtimer.db.Database;
import com.earthpol.parkourtimer.db.Schema;
import com.earthpol.parkourtimer.command.ParkourCommand;
import com.earthpol.parkourtimer.db.ParkourRepository;
import com.earthpol.parkourtimer.listener.ParkourListener;
import com.earthpol.parkourtimer.service.ActionBarService;
import com.earthpol.parkourtimer.util.ParkourLogger;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ParkourTimer extends JavaPlugin {

    private ParkourTimerManager timerManager;
    private Database database;
    private ParkourRepository parkourRepository;
    private ParkourLogger parkourLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // initalize logging system
        parkourLogger = new ParkourLogger(this);
        parkourLogger.info("Initializing ParkourTimer...");

        try {
            initializeCoreSystems();
            registerListenersAndCommands();

            ActionBarService actionBarService = new ActionBarService(this);
            actionBarService.start();

            parkourLogger.info("ParkourTimer enabled successfully!");
        } catch (Exception e) {
            parkourLogger.error("Fatal error during plugin startup", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // proper shutdown of database pool
        parkourLogger.info("Shutting down ParkourTimer...");

        if (database != null) {
            database.shutdown();
            parkourLogger.info("Database connection pool closed.");
        }
    }

    // initalize plugin
    private void initializeCoreSystems() {

        timerManager = new ParkourTimerManager();

        database = new Database(
                getConfig().getString("database.host"),
                getConfig().getInt("database.port"),
                getConfig().getString("database.name"),
                getConfig().getString("database.user"),
                getConfig().getString("database.password")
        );

        Schema.init(database);
        parkourLogger.info("Database schema verified.");

        parkourRepository = new ParkourRepository(this, database);
    }

    private void registerListenersAndCommands() {

        getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        // register parkour commands
        if (getCommand("parkour") != null) {
            Objects.requireNonNull(getCommand("parkour")).setExecutor(new ParkourCommand(this));
        } else {
            parkourLogger.warning("Command 'parkour' not found in plugin.yml!");
        }
    }

    public ParkourTimerManager getTimerManager() {
        return timerManager;
    }

    public ParkourRepository getParkourRepository() {
        return parkourRepository;
    }

    public ParkourLogger getParkourLogger() {
        return parkourLogger;
    }
}