package com.earthpol.parkourtimer.command;

import com.earthpol.parkourtimer.ParkourTimer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParkourCommand implements CommandExecutor {

    private final ParkourTimer plugin;

    public ParkourCommand(ParkourTimer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /parkour <clear|leaderboard> [player]");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /parkour clear <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }

                UUID uuid = target.getUniqueId();
                plugin.getParkourRepository().clearRecord(uuid, () ->
                        sender.sendMessage("§aCleared parkour record for " + target.getName()));
                return true;
            }

            case "leaderboard" -> {
                plugin.getParkourRepository().fetchLeaderboard(10, leaderboard ->
                        sender.sendMessage(leaderboard));
                return true;
            }

            default -> sender.sendMessage("§cUnknown subcommand. Usage: /parkour <clear|leaderboard> [player]");
        }

        return true;
    }
}