package de.power.bat.utils;

import de.power.Power;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

/** Zeigt das Clan-Tag über dem Kopf (Scoreboard-Team-Prefix). Jeder Spieler bekommt ein eigenes Team "pb_...". */
public class NametagManager {

    private static final String TEAM_PREFIX = "pb_";

    private final Power plugin;
    private final DisplayManager display;

    public NametagManager(Power plugin, DisplayManager display) {
        this.plugin = plugin;
        this.display = display;
    }

    private String teamName(Player player) {
        return TEAM_PREFIX + player.getUniqueId().toString().replace("-", "").substring(0, 13);
    }

    private Scoreboard board() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        return manager == null ? null : manager.getMainScoreboard();
    }

    private String buildPrefix(Player player) {
        String template = plugin.getConfig().getString("nametag.format", "{clan}");
        String text = template
                .replace("{prefix}", display.rawPrefix(player))
                .replace("{clan}", display.clanTag(player));
        return Colors.color(text);
    }

    public void update(Player player) {
        Scoreboard board = board();
        if (board == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("nametag.enabled", true)) {
            remove(player);
            return;
        }
        String prefix = buildPrefix(player);
        String name = teamName(player);
        Team team = board.getTeam(name);
        if (prefix.isBlank()) {
            if (team != null) {
                team.unregister();
            }
            return;
        }
        if (team == null) {
            team = board.registerNewTeam(name);
        }
        try {
            team.setPrefix(prefix);
        } catch (IllegalArgumentException ex) {
            team.setPrefix(prefix.substring(0, Math.min(prefix.length(), 64)));
        }
        team.setColor(ChatColor.WHITE);
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void remove(Player player) {
        Scoreboard board = board();
        if (board == null) {
            return;
        }
        Team team = board.getTeam(teamName(player));
        if (team != null) {
            team.unregister();
        }
    }

    public void removeAll() {
        Scoreboard board = board();
        if (board == null) {
            return;
        }
        for (Team team : new ArrayList<>(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }
}
