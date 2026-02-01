package com.earthpol.parkourtimer.command;

import com.earthpol.parkourtimer.ParkourTimer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can clear parkour records!");
                    return true;
                }

                Player target;
                if (args.length < 2) {
                    // if no player specified, target is themselves
                    target = player;
                } else {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cPlayer not found!");
                        return true;
                    }

                    // if trying to clear someone else, check OP
                    if (!player.isOp() && !target.getUniqueId().equals(player.getUniqueId())) {
                        sender.sendMessage("§cYou can only clear your own parkour record!");
                        return true;
                    }
                }

                UUID uuid = target.getUniqueId();
                plugin.getParkourRepository().clearRecord(uuid, () -> {
                    sender.sendMessage("§aCleared parkour record for " + target.getName());

                    // log the clear action
                    String actor = player.getName();
                    plugin.getParkourLogger().info("Parkour record cleared for " + target.getName() + " by " + actor);
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
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(typed)) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}