package com.earthpol.parkourtimer.listener;

import com.earthpol.parkourtimer.ParkourTimer;
import com.earthpol.parkourtimer.service.ControlItemService;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import com.earthpol.parkourtimer.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParkourListener implements Listener {

    private final ParkourTimer plugin;
    private final ParkourTimerManager timerManager;
    private final ControlItemService controlService;

    private final Set<UUID> watchedPlayers = new HashSet<>();
    private Location resetLocation;

    public ParkourListener(ParkourTimer plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.controlService = new ControlItemService(plugin);

        loadResetLocation();
    }

    // parkour movement
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!watchedPlayers.contains(uuid) || !timerManager.isRunning(uuid)) return;

        if (event.getTo().getBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            long time = timerManager.stop(uuid);
            watchedPlayers.remove(uuid);

            plugin.getParkourLogger().player(uuid, player.getName(), "FINISH_RUN time=" + time);

            // save run async
            plugin.getParkourRepository().saveRun(uuid, player.getName(), time);

            sendMessage(player, plugin.getConfig()
                    .getString("messages.complete", "&aCompleted! Time: {time}")
                    .replace("{time}", TimeFormatter.formatLong(time))
            );

            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("sounds.end", "entity.player.levelup"),
                    1f, 1f);

            controlService.removeControlItems(player);
        }
    }

    // starting parkour check
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        // start timer on light weighted pressure plate
        if (event.getAction() == Action.PHYSICAL &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {

            if (timerManager.isRunning(uuid)) return;

            watchedPlayers.add(uuid);
            timerManager.start(uuid);
            plugin.getParkourLogger().player(uuid, player.getName(), "START_RUN");

            sendMessage(player, plugin.getConfig().getString("messages.start", "&aTimer started!"));
            controlService.giveControlItems(player);

            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("sounds.start", "entity.experience_orb.pickup"),
                    1f, 1f);
            return;
        }

        // control items
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || !timerManager.isRunning(uuid) || !controlService.isControlItem(item)) return;

        int slot = player.getInventory().getHeldItemSlot();
        if (slot == ControlItemService.RESTART_SLOT) {
            controlService.handleRestart(player, () -> teleportToReset(player));
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
        controlService.removeControlItems(player);

        if (timerManager.isRunning(uuid)) {
            plugin.getParkourLogger().player(uuid, player.getName(), "QUIT_DURING_RUN");
        }
    }

    // item protection
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (controlService.isControlItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if ((current != null && controlService.isControlItem(current)) ||
                (event.getSlot() == 40 && cursor != null && controlService.isControlItem(cursor))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (controlService.isControlItem(event.getMainHandItem()) || controlService.isControlItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    // helpers

    private void teleportToReset(Player player) {
        if (resetLocation != null) player.teleport(resetLocation);
    }

    private void sendMessage(Player player, String msg) {
        if (msg != null) player.sendMessage(color(msg));
    }

    private Component color(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    private void loadResetLocation() {
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
    }
}