package com.earthpol.parkourtimer.service;

import com.earthpol.parkourtimer.ParkourTimer;
import com.earthpol.parkourtimer.timer.ParkourTimerManager;
import com.earthpol.parkourtimer.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ControlItemService {

    private final ParkourTimer plugin;
    private final ParkourTimerManager timerManager;
    private final NamespacedKey CONTROL_ITEM_KEY;

    private final Set<UUID> restartCooldown = new HashSet<>();

    public static final int RESTART_SLOT = 3;
    public static final int CANCEL_SLOT = 5;

    public ControlItemService(ParkourTimer plugin) {
        this.plugin = plugin;
        this.timerManager = plugin.getTimerManager();
        this.CONTROL_ITEM_KEY = new NamespacedKey(plugin, "control_item");
    }

    // public methods for ParkourListener

    public void giveControlItems(Player player) {
        player.getInventory().setItem(RESTART_SLOT, createItem(Material.DIAMOND_BLOCK, "&b&lRestart"));
        player.getInventory().setItem(CANCEL_SLOT, createItem(Material.REDSTONE_BLOCK, "&4&lCancel"));
    }

    public void removeControlItems(Player player) {
        player.getInventory().setItem(RESTART_SLOT, null);
        player.getInventory().setItem(CANCEL_SLOT, null);
        player.getInventory().setItemInOffHand(null);
    }

    public boolean isControlItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(CONTROL_ITEM_KEY, PersistentDataType.BYTE);
    }

    // called when player starts parkour
    public void handleStart(Player player) {
        giveControlItems(player);
        sendMessage(player, plugin.getConfig().getString("messages.start", "&aTimer started!"));
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    // called when player finishes parkour
    public void handleFinish(Player player, long time) {
        sendMessage(player,
                plugin.getConfig().getString("messages.complete", "&aCompleted! Time: {time}")
                        .replace("{time}", TimeFormatter.formatLong(time))
        );
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
    }

    public void handleRestart(Player player, Runnable teleportCallback) {
        UUID uuid = player.getUniqueId();

        if (restartCooldown.contains(uuid)) {
            sendMessage(player, "&cPlease wait a moment before restarting again!");
            return;
        }

        plugin.getParkourLogger().player(uuid, player.getName(), "RESTART_RUN");

        // add cooldown
        restartCooldown.add(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restartCooldown.remove(uuid), 20L); // 1 second

        // play teleport sound
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);

        // stop timer and cleanup
        timerManager.stop(uuid);
        removeControlItems(player);

        sendMessage(player, plugin.getConfig().getString("messages.reset", "&cTimer reset!"));

        // callback to teleport player (listener decides how)
        if (teleportCallback != null) teleportCallback.run();
    }

    public void handleCancel(Player player) {
        UUID uuid = player.getUniqueId();
        timerManager.stop(uuid);
        removeControlItems(player);

        plugin.getParkourLogger().player(uuid, player.getName(), "CANCEL_RUN");

        playSound(player, Sound.BLOCK_ANVIL_LAND);
        sendMessage(player, plugin.getConfig().getString("messages.cancel", "&cTimer cancelled!"));
    }

    // helpers

    public void protectDrop(PlayerDropItemEvent event) {
        if (isControlItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    public void protectInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (current != null && isControlItem(current) || event.getSlot() == 40 && isControlItem(cursor)) {
            event.setCancelled(true);
        }
    }

    public void protectSwapHand(PlayerSwapHandItemsEvent event) {
        if (isControlItem(event.getMainHandItem()) || isControlItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
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

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    private void sendMessage(Player player, String msg) {
        if (msg != null) player.sendMessage(color(msg));
    }

    private Component color(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}