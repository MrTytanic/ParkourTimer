package com.earthpol.parkourtimer.command;

import com.earthpol.parkourtimer.ParkourTimer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ParkourCommand implements CommandExecutor, TabCompleter {

    private final ParkourTimer plugin;

    public ParkourCommand(ParkourTimer plugin) {
        this.plugin = plugin;
        // Register as both executor and tab completer
        plugin.getCommand("parkour").setExecutor(this);
        plugin.getCommand("parkour").setTabCompleter(this);
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

                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[1]);

                if (offlineTarget == null ||
                        (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }

                UUID uuid = offlineTarget.getUniqueId();
                String targetName = offlineTarget.getName() != null
                        ? offlineTarget.getName()
                        : args[1];

                // If sender is a player and not OP, only allow self-clear
                if (sender instanceof Player player) {
                    if (!player.isOp() && !player.getUniqueId().equals(uuid)) {
                        sender.sendMessage("§cYou can only clear your own parkour record!");
                        return true;
                    }
                }

                plugin.getParkourRepository().clearRecord(uuid, () -> {
                    sender.sendMessage("§aCleared parkour record for " + targetName);

                    String actor = (sender instanceof Player p)
                            ? p.getName()
                            : "Console";

                    plugin.getParkourLogger().info(
                            "Parkour record cleared for " + targetName + " by " + actor
                    );
                });

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

    // Tab completion
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> subcommands = List.of("clear", "leaderboard");

        if (args.length == 1) {
            // suggest subcommands
            String typed = args[0].toLowerCase();

            for (String sub : subcommands) {
                if (sub.startsWith(typed)) {
                    completions.add(sub);
                }
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            // op tab-complete player names for /parkour clear <player>
            if (sender instanceof Player player && player.isOp()) {
                String typed = args[1].toLowerCase();
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(typed)) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}