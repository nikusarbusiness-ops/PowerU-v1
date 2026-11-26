package de.power.bat.commands;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.ClanColor;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.InviteManager;
import de.power.bat.gui.MainMenu;
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

public class ClanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "invite", "accept", "deny", "leave", "kick", "promote", "demote", "transfer", "disband",
            "info", "chat", "setname", "setcolor", "settag", "setdesc", "pvp", "members", "gui", "help");

    private final Power plugin;

    public ClanCommand(Power plugin) {
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
        if (!player.hasPermission("powerbat.clan.use")) {
            m.send(player, "general.no-permission");
            return true;
        }
        ClanManager cm = plugin.getClanManager();

        if (args.length == 0) {
            MainMenu.openFor(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "?" -> {
                for (String line : m.listFor(player, "help.clan")) {
                    player.sendMessage(line);
                }
            }
            case "gui", "menu" -> MainMenu.openFor(plugin, player);
            case "pvp" -> cm.togglePvp(player);
            case "create" -> {
                if (args.length < 2) {
                    usage(player, "/clan create <Name>");
                } else {
                    cm.createClan(player, args[1]);
                }
            }
            case "invite" -> {
                if (args.length < 2) {
                    usage(player, "/clan invite <Spieler>");
                } else {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        m.send(player, "general.player-not-found", "%player%", args[1]);
                    } else {
                        cm.invite(player, target);
                    }
                }
            }
            case "accept" -> cm.acceptInvite(player, args.length >= 2 ? args[1] : null);
            case "deny", "decline" -> cm.denyInvite(player, args.length >= 2 ? args[1] : null);
            case "leave" -> cm.leave(player);
            case "kick", "promote", "demote", "transfer" -> {
                if (args.length < 2) {
                    usage(player, "/clan " + sub + " <Spieler>");
                    break;
                }
                Clan clan = cm.getClan(player.getUniqueId());
                if (clan == null) {
                    m.send(player, "clan.error.not-in-clan");
                    break;
                }
                UUID target = cm.findMember(clan, args[1]);
                if (target == null) {
                    m.send(player, "clan.error.target-not-member", "%player%", args[1]);
                    break;
                }
                switch (sub) {
                    case "kick" -> cm.kick(player, target);
                    case "promote" -> cm.promote(player, target);
                    case "demote" -> cm.demote(player, target);
                    default -> cm.transfer(player, target);
                }
            }
            case "disband" -> {
                Clan clan = cm.getClan(player.getUniqueId());
                if (clan == null) {
                    m.send(player, "clan.error.not-in-clan");
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    cm.disband(player);
                } else {
                    m.send(player, "clan.disband-confirm");
                }
            }
            case "info" -> {
                Clan clan = args.length >= 2 ? cm.getClanByName(args[1]) : cm.getClan(player.getUniqueId());
                if (clan == null) {
                    m.send(player, args.length >= 2 ? "clan.error.clan-not-found" : "clan.error.not-in-clan",
                            "%clan%", args.length >= 2 ? args[1] : "");
                } else {
                    cm.sendInfo(player, clan);
                }
            }
            case "members" -> {
                Clan clan = cm.getClan(player.getUniqueId());
                if (clan == null) {
                    m.send(player, "clan.error.not-in-clan");
                } else {
                    cm.sendMembers(player, clan);
                }
            }
            case "chat" -> {
                if (args.length >= 2) {
                    cm.sendClanChat(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                } else {
                    cm.toggleChat(player);
                }
            }
            case "setname", "rename" -> {
                if (args.length < 2) {
                    usage(player, "/clan setname <Name>");
                } else {
                    cm.rename(player, args[1]);
                }
            }
            case "setcolor", "color" -> {
                if (args.length < 2) {
                    usage(player, "/clan setcolor <Farbe>");
                } else {
                    cm.setColor(player, args[1]);
                }
            }
            case "settag", "tag" -> {
                if (args.length < 2) {
                    usage(player, "/clan settag <Tag>");
                } else {
                    cm.setTag(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim());
                }
            }
            case "setdesc", "description", "desc" -> {
                if (args.length < 2) {
                    usage(player, "/clan setdesc <Text>");
                } else {
                    cm.setDescription(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim());
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
        ClanManager cm = plugin.getClanManager();
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            Clan own = cm.getClan(player.getUniqueId());
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "invite" -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (cm.getClan(online.getUniqueId()) == null && !online.equals(player)) {
                            result.add(online.getName());
                        }
                    }
                }
                case "kick", "promote", "demote", "transfer" -> {
                    if (own != null) {
                        for (UUID id : own.getMembers().keySet()) {
                            result.add(plugin.getPlayerData().getName(id));
                        }
                    }
                }
                case "setcolor", "color" -> result.addAll(ClanColor.tabNames());
                case "accept", "deny", "decline" -> {
                    for (InviteManager.Invite invite : plugin.getInviteManager().getFor(player.getUniqueId())) {
                        Clan clan = cm.getClanById(invite.clanId());
                        if (clan != null) {
                            result.add(clan.getName());
                        }
                    }
                }
                case "info" -> {
                    for (Clan clan : cm.getClans()) {
                        result.add(clan.getName());
                    }
                }
                case "disband" -> result.add("confirm");
                default -> {
                }
            }
            return filter(result, args[1]);
        }
        return result;
    }

    static List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
