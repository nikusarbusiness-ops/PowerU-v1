package de.power.bat.commands;

import de.power.Power;
import de.power.bat.friends.FriendManager;
import de.power.bat.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "add", "accept", "deny", "remove", "list", "requests", "msg", "reply", "help");

    private final Power plugin;

    public FriendCommand(Power plugin) {
        this.plugin = plugin;
    }

    private void usage(Player player, String usage) {
        plugin.getMessages().send(player, "general.usage", "%usage%", usage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            m.send(sender, "general.players-only");
            return true;
        }
        if (!player.hasPermission("powerbat.friend.use")) {
            m.send(player, "general.no-permission");
            return true;
        }
        FriendManager fm = plugin.getFriendManager();
        if (args.length == 0) {
            fm.sendList(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "?" -> {
                for (String line : m.listFor(player, "help.friend")) {
                    player.sendMessage(line);
                }
            }
            case "add" -> {
                if (args.length < 2) {
                    usage(player, "/friend add <Spieler>");
                } else {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        m.send(player, "general.player-not-found", "%player%", args[1]);
                    } else {
                        fm.sendRequest(player, target);
                    }
                }
            }
            case "accept" -> fm.accept(player, args.length >= 2 ? args[1] : null);
            case "deny", "decline" -> fm.deny(player, args.length >= 2 ? args[1] : null);
            case "remove", "delete" -> {
                if (args.length < 2) {
                    usage(player, "/friend remove <Spieler>");
                } else {
                    fm.remove(player, args[1]);
                }
            }
            case "list" -> fm.sendList(player);
            case "requests" -> fm.sendRequests(player);
            case "msg", "message", "tell", "w" -> {
                if (args.length < 3) {
                    usage(player, "/friend msg <Freund> <Nachricht>");
                } else {
                    fm.message(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                }
            }
            case "reply", "r" -> {
                if (args.length < 2) {
                    usage(player, "/friend reply <Nachricht>");
                } else {
                    fm.reply(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                }
            }
            default -> m.send(player, "general.unknown-subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return result;
        }
        FriendManager fm = plugin.getFriendManager();
        if (args.length == 1) {
            return ClanCommand.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "add" -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(player) && !fm.areFriends(player.getUniqueId(), online.getUniqueId())) {
                            result.add(online.getName());
                        }
                    }
                }
                case "remove", "delete", "msg", "message", "tell", "w" -> {
                    for (UUID id : fm.getFriends(player.getUniqueId())) {
                        result.add(plugin.getPlayerData().getName(id));
                    }
                }
                case "accept", "deny", "decline" -> {
                    for (UUID id : fm.getRequests(player.getUniqueId())) {
                        result.add(plugin.getPlayerData().getName(id));
                    }
                }
                default -> {
                }
            }
            return ClanCommand.filter(result, args[1]);
        }
        return result;
    }
}
