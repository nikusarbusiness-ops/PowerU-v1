package de.power.bat.utils;

import de.power.Power;
import de.power.bat.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Baut Chat- und Tablist-Darstellung (LuckPerms-Prefix + Clan-Tag + Name). */
public class DisplayManager {

    private static final String NAME_MARK = "\u0001";
    private static final String MESSAGE_MARK = "\u0002";

    private final Power plugin;
    private final NametagManager nametags;

    public DisplayManager(Power plugin) {
        this.plugin = plugin;
        this.nametags = new NametagManager(plugin, this);
    }

    /** LuckPerms-Prefix (mit &-Codes), leer wenn nicht vorhanden. */
    public String rawPrefix(Player player) {
        if (plugin.getLuckPerms() == null) {
            return "";
        }
        try {
            String prefix = plugin.getLuckPerms().getPrefix(player);
            return prefix == null ? "" : prefix;
        } catch (Throwable t) {
            return "";
        }
    }

    /** Eingefärbtes Clan-Tag inkl. abschließendem Leerzeichen oder leer. */
    public String clanTag(Player player) {
        Clan clan = plugin.getClanManager().getClan(player.getUniqueId());
        if (clan == null) {
            return "";
        }
        return plugin.getClanManager().displayTag(clan) + "§r ";
    }

    /** Format für AsyncPlayerChatEvent#setFormat (%1$s = Name, %2$s = Nachricht). */
    public String buildChatFormat(Player player) {
        String template = plugin.getConfig().getString("chat.format", "{prefix}{clan}{name}&8: &f{message}");
        String text = template
                .replace("{prefix}", rawPrefix(player))
                .replace("{clan}", clanTag(player))
                .replace("{name}", NAME_MARK)
                .replace("{message}", MESSAGE_MARK);
        text = Colors.escapeFormat(Colors.color(text));
        return text.replace(NAME_MARK, "%1$s").replace(MESSAGE_MARK, "%2$s");
    }

    public String buildTabName(Player player) {
        String template = plugin.getConfig().getString("tablist.format", "{prefix}{clan}{name}");
        String text = template
                .replace("{prefix}", rawPrefix(player))
                .replace("{clan}", clanTag(player))
                .replace("{name}", player.getName());
        return Colors.color(text);
    }

    public void update(Player player) {
        if (plugin.getConfig().getBoolean("tablist.enabled", true)) {
            player.setPlayerListName(buildTabName(player));
        }
        nametags.update(player);
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void reset(Player player) {
        player.setPlayerListName(null);
        nametags.remove(player);
    }

    /** Entfernt nur das Namensschild (z.B. beim Logout). */
    public void removeNametag(Player player) {
        nametags.remove(player);
    }

    public void removeAllNametags() {
        nametags.removeAll();
    }
}
