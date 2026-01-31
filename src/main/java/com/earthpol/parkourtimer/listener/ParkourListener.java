package com.earthpol.parkourtimer.listener;

import com.earthpol.parkourtimer.ParkourTimer;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParkourListener implements Listener {

    private final ParkourTimer plugin;
    private final ParkourTimerManager timerManager;
    private final Set<UUID> watchedPlayers = new HashSet<>();
    private final Set<UUID> restartCooldown = new HashSet<>();
    private final NamespacedKey CONTROL_ITEM_KEY;

    private Location resetLocation;

    private static final int RESTART_SLOT = 3;
    private static final int CANCEL_SLOT = 5;

    public ParkourListener(ParkourTimer plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.CONTROL_ITEM_KEY = new NamespacedKey(plugin, "control_item");

        loadResetLocation();
        startActionBarUpdater();
    }

    // end of parkour movement check
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!watchedPlayers.contains(uuid)) return;
        if (!timerManager.isRunning(uuid)) return;

        if (event.getTo().getBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            long time = timerManager.stop(uuid);
            watchedPlayers.remove(uuid);

            plugin.getLogger().info("Saving parkour run for " + player.getName() + " time=" + time);

            // save run async
            plugin.getParkourRepository().saveRun(uuid, player.getName(), time);

            sendMessage(player, plugin.getConfig()
                    .getString("messages.complete", "&aCompleted! Time: {time}")
                    .replace("{time}", TimeFormatter.formatLong(time))
            );

            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("sounds.end", "entity.player.levelup"),
                    1f, 1f);
            removeControlItems(player);
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

            sendMessage(player, plugin.getConfig().getString("messages.start", "&aTimer started!"));
            giveControlItems(player);

            player.playSound(player.getLocation(),
                    plugin.getConfig().getString("sounds.start", "entity.experience_orb.pickup"),
                    1f, 1f);
            return;
        }

        // control items (restart/cancel)
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || !timerManager.isRunning(uuid) || !isControlItem(item)) return;

        int slot = player.getInventory().getHeldItemSlot();
        if (slot == RESTART_SLOT) handleRestart(player);
        else if (slot == CANCEL_SLOT) handleCancel(player);

        event.setCancelled(true);
    }

    // cleanup
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        timerManager.stop(uuid);
        watchedPlayers.remove(uuid);
        restartCooldown.remove(uuid);
        removeControlItems(event.getPlayer());
    }

    // item protection
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isControlItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if ((event.getCurrentItem() != null && isControlItem(event.getCurrentItem())) ||
                (event.getSlot() == 40 && event.getCursor() != null && isControlItem(event.getCursor()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (isControlItem(event.getMainHandItem()) || isControlItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    // helpers
    private void handleRestart(Player player) {
        UUID uuid = player.getUniqueId();

        // cooldown check
        if (restartCooldown.contains(uuid)) {
            sendMessage(player, "&cPlease wait a moment before restarting again!");
            return;
        }

        // add to cooldown
        restartCooldown.add(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restartCooldown.remove(uuid), 20L); // 1 second

        // play teleport sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // stop timer and cleanup
        timerManager.stop(uuid);
        watchedPlayers.remove(uuid);

        sendMessage(player, plugin.getConfig().getString("messages.reset", "&cTimer reset!"));
        teleportToReset(player);
        removeControlItems(player);
    }

    private void handleCancel(Player player) {
        UUID uuid = player.getUniqueId();
        timerManager.stop(uuid);
        watchedPlayers.remove(uuid);

        // play anvil sound
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

        sendMessage(player, plugin.getConfig().getString("messages.cancel", "&cTimer cancelled!"));
        removeControlItems(player);
    }

    private void teleportToReset(Player player) {
        if (resetLocation != null) player.teleport(resetLocation);
    }

    private void startActionBarUpdater() {
        long maxTime = plugin.getConfig().getLong("max_time", 300) * 1000;

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (watchedPlayers.isEmpty()) return;

            for (UUID uuid : new HashSet<>(watchedPlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !timerManager.isRunning(uuid)) continue;

                long elapsed = timerManager.getElapsed(uuid);

                // timeout
                if (elapsed >= maxTime) {
                    timerManager.stop(uuid);
                    watchedPlayers.remove(uuid);
                    removeControlItems(player);

                    // timout message + sound
                    String msg = plugin.getConfig().getString("messages.timeout", "&cYou took too long!");
                    player.sendMessage(color(msg));
                    player.playSound(player.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1f, 1f);

                    continue;
                }

                // action bar display
                String time = TimeFormatter.format(elapsed);
                String bar = plugin.getConfig().getString("messages.actionbar", "&aTime: {time}")
                        .replace("{time}", time);
                player.sendActionBar(color(bar));
            }
        }, 0L, 2L);
    }

    private void giveControlItems(Player player) {
        player.getInventory().setItem(RESTART_SLOT, createItem(Material.DIAMOND_BLOCK, "&b&lRestart"));
        player.getInventory().setItem(CANCEL_SLOT, createItem(Material.REDSTONE_BLOCK, "&4&lCancel"));
    }

    private void removeControlItems(Player player) {
        player.getInventory().setItem(RESTART_SLOT, null);
        player.getInventory().setItem(CANCEL_SLOT, null);
        player.getInventory().setItemInOffHand(null);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.getPersistentDataContainer().set(CONTROL_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isControlItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(CONTROL_ITEM_KEY, PersistentDataType.BYTE);
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