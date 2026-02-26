package com.earthpol.parkourtimer.listener;

import com.earthpol.parkourtimer.ParkourTimer;
import com.earthpol.parkourtimer.service.ControlItemService;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ParkourListener implements Listener {

    private final ParkourTimer plugin;
    private final ParkourTimerManager timerManager;
    private final ControlItemService controlService;

    private final Set<UUID> watchedPlayers = new HashSet<>();
    private final Map<UUID, Location> lastCheckpoint = new HashMap<>();
    private final List<Location> checkpoints = new ArrayList<>();
    private Location resetLocation;
    private Location startLocation;
    private Location endLocation;

    public ParkourListener(ParkourTimer plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.controlService = new ControlItemService(plugin);

        loadLocations();
    }

    // parkour movement
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location toBlock = event.getTo().getBlock().getLocation();

        // start check
        if (!timerManager.isRunning(uuid) && isSameBlock(toBlock, startLocation)) {
            watchedPlayers.add(uuid);
            timerManager.start(uuid);

            // default lastCheckpoint to resetLocation (not startLocation)
            lastCheckpoint.put(uuid, resetLocation);

            plugin.getParkourLogger().player(uuid, player.getName(), "START_RUN");
            controlService.handleStart(player);
            return;
        }

        // checkpoints
        for (Location checkpoint : checkpoints) {
            if (isSameBlock(toBlock, checkpoint)) {
                if (!checkpoint.equals(lastCheckpoint.get(uuid))) {
                    lastCheckpoint.put(uuid, checkpoint);

                    // send message from config
                    String msg = plugin.getConfig().getString("messages.checkpoint", "&aNew checkpoint reached!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    player.playSound(
                            player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1f, 1f
                    );
                }
                break;
            }
        }

        // finish check
        if (watchedPlayers.contains(uuid) && timerManager.isRunning(uuid) && isSameBlock(toBlock, endLocation)) {
            long time = timerManager.stop(uuid);
            watchedPlayers.remove(uuid);
            lastCheckpoint.remove(uuid);

            plugin.getParkourLogger().player(uuid, player.getName(), "FINISH_RUN time=" + time);
            plugin.getParkourRepository().saveRun(uuid, player.getName(), time);

            // delegate messaging and sound
            controlService.handleFinish(player, time);
            controlService.removeControlItems(player);
        }
    }

    // player interaction
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item.getType() == Material.AIR || !timerManager.isRunning(uuid) || !controlService.isControlItem(item)) return;

        int slot = player.getInventory().getHeldItemSlot();
        if (slot == ControlItemService.RESTART_SLOT) {
            controlService.handleRestart(player, () -> teleportToReset(player));
        } else if (slot == ControlItemService.CHECKPOINT_SLOT) {
            controlService.handleCheckpoint(player, () -> teleportToCheckpoint(player));
        } else if (slot == ControlItemService.CANCEL_SLOT) {
            controlService.handleCancel(player);
        }

        event.setCancelled(true);
    }

    // cleanup
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        timerManager.stop(uuid);
        watchedPlayers.remove(uuid);
        lastCheckpoint.remove(uuid);
        controlService.removeControlItems(player);

        if (timerManager.isRunning(uuid)) {
            plugin.getParkourLogger().player(uuid, player.getName(), "QUIT_DURING_RUN");
        }
    }

    // delegate item protections
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        controlService.protectDrop(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        controlService.protectInventoryClick(event);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        controlService.protectSwapHand(event);
    }

    // helpers
    private void teleportToReset(Player player) {
        if (resetLocation != null) player.teleport(resetLocation);
    }

    private void teleportToCheckpoint(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = lastCheckpoint.get(uuid);

        // if no checkpoint has been reached yet, default to resetLocation
        if (loc == null) {
            loc = resetLocation;
            lastCheckpoint.put(uuid, loc); // make sure future checkpoint uses this as base
        }

        if (loc != null) {
            player.teleport(loc);
            player.setFallDistance(0);
            player.setVelocity(new Vector(0, 0, 0));
        }
    }

    private void loadLocations() {
        // reset location
        World world = Bukkit.getWorld(plugin.getConfig().getString("reset_location.world", ""));
        if (world != null) {
            resetLocation = new Location(
                    world,
                    plugin.getConfig().getDouble("reset_location.x"),
                    plugin.getConfig().getDouble("reset_location.y"),
                    plugin.getConfig().getDouble("reset_location.z"),
                    (float) plugin.getConfig().getDouble("reset_location.yaw"),
                    (float) plugin.getConfig().getDouble("reset_location.pitch")
            );
        }

        // Start location
        World startWorld = Bukkit.getWorld(plugin.getConfig().getString("start_location.world", ""));
        if (startWorld != null) {
            startLocation = new Location(
                    startWorld,
                    plugin.getConfig().getDouble("start_location.x"),
                    plugin.getConfig().getDouble("start_location.y"),
                    plugin.getConfig().getDouble("start_location.z")
            );
        }

        // End location
        World endWorld = Bukkit.getWorld(plugin.getConfig().getString("end_location.world", ""));
        if (endWorld != null) {
            endLocation = new Location(
                    endWorld,
                    plugin.getConfig().getDouble("end_location.x"),
                    plugin.getConfig().getDouble("end_location.y"),
                    plugin.getConfig().getDouble("end_location.z")
            );
        }

        // Load checkpoints dynamically
        checkpoints.clear();
        int i = 1;
        while (plugin.getConfig().contains("checkpoint_" + i + ".world")) {
            World w = Bukkit.getWorld(plugin.getConfig().getString("checkpoint_" + i + ".world"));
            if (w != null) {
                Location loc = new Location(
                        w,
                        plugin.getConfig().getDouble("checkpoint_" + i + ".x"),
                        plugin.getConfig().getDouble("checkpoint_" + i + ".y"),
                        plugin.getConfig().getDouble("checkpoint_" + i + ".z"),
                        (float) plugin.getConfig().getDouble("checkpoint_" + i + ".yaw", 0),
                        (float) plugin.getConfig().getDouble("checkpoint_" + i + ".pitch", 0)
                );
                checkpoints.add(loc);
            }
            i++;
        }
    }

    private boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}