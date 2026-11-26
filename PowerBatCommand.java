package de.power.bat.commands;

import de.power.Power;
import de.power.bat.clan.Clan;
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

public class PowerBatCommand implements CommandExecutor, TabCompleter {

    private final Power plugin;

    public PowerBatCommand(Power plugin) {
        this.plugin = plugin;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("powerbat.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var m = plugin.getMessages();
        if (args.length == 0) {
            if (!has(sender, "powerbat.admin")) {
                m.send(sender, "general.no-permission");
                return true;
            }
            for (String line : m.listFor(sender, "help.admin")) {
                sender.sendMessage(line);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!has(sender, "powerbat.reload")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                plugin.reloadAll();
                m.send(sender, "admin.reloaded");
            }
            case "version" -> {
                if (!has(sender, "powerbat.admin")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                m.send(sender, "admin.version", "%version%", plugin.getDescription().getVersion());
            }
            case "info" -> {
                if (!has(sender, "powerbat.admin")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                for (String line : m.listFor(sender, "admin.info",
                        "%version%", plugin.getDescription().getVersion(),
                        "%clans%", String.valueOf(plugin.getClanManager().getClans().size()),
                        "%combat%", String.valueOf(plugin.getCombatManager().getCombatCount()),
                        "%duration%", String.valueOf(plugin.getCombatManager().duration()),
                        "%luckperms%", plugin.getLuckPerms() != null ? "§aAktiv" : "§cNicht gefunden")) {
                    sender.sendMessage(line);
                }
            }
            case "combat" -> {
                if (!has(sender, "powerbat.admin")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    m.send(sender, "general.usage", "%usage%", "/powerbat combat <Spieler>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    m.send(sender, "general.player-not-found", "%player%", args[1]);
                    return true;
                }
                plugin.getCombatManager().forceTag(target);
                m.send(sender, "admin.combat-started", "%player%", target.getName());
            }
            case "endcombat" -> {
                if (!has(sender, "powerbat.admin")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    m.send(sender, "general.usage", "%usage%", "/powerbat endcombat <Spieler>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    m.send(sender, "general.player-not-found", "%player%", args[1]);
                    return true;
                }
                if (plugin.getCombatManager().untag(target.getUniqueId(), true)) {
                    m.send(sender, "admin.combat-ended", "%player%", target.getName());
                } else {
                    m.send(sender, "admin.not-in-combat", "%player%", target.getName());
                }
            }
            case "clan" -> {
                if (!has(sender, "powerbat.clan.admin")) {
                    m.send(sender, "general.no-permission");
                    return true;
                }
                if (args.length < 3 || !(args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("info"))) {
                    m.send(sender, "general.usage", "%usage%", "/powerbat clan <delete|info> <Clan>");
                    return true;
                }
                Clan clan = plugin.getClanManager().getClanByName(args[2]);
                if (clan == null) {
                    m.send(sender, "clan.error.clan-not-found", "%clan%", args[2]);
                    return true;
                }
                if (args[1].equalsIgnoreCase("info")) {
                    plugin.getClanManager().sendInfo(sender, clan);
                } else {
                    String name = clan.getName();
                    plugin.getClanManager().deleteClan(clan, "admin.clan-deleted-notify");
                    m.send(sender, "admin.clan-deleted", "%clan%", name);
                }
            }
            default -> m.send(sender, "general.unknown-subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            return ClanCommand.filter(Arrays.asList("reload", "version", "info", "combat", "endcombat", "clan"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "combat", "endcombat" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        result.add(player.getName());
                    }
                }
                case "clan" -> result.addAll(Arrays.asList("delete", "info"));
                default -> {
                }
            }
            return ClanCommand.filter(result, args[1]);
        }
        if (args.length == 3 && sub.equals("clan")) {
            for (Clan clan : plugin.getClanManager().getClans()) {
                result.add(clan.getName());
            }
            return ClanCommand.filter(result, args[2]);
        }
        return result;
    }
}
