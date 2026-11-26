package de.power.gui;

import de.power.Power;
import de.power.bat.gui.Menu;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /rekord, /rekorde - Server-Rekorde und Bestenlisten. */
public class RecordsMenu extends Menu {

    public RecordsMenu(Power plugin, Player viewer) { super(plugin, viewer); }

    @Override
    protected String title() { return "&8Rekorde"; }

    @Override
    protected int rows() { return 5; }

    private List<String> board(String stat, boolean time) {
        List<String> lore = new ArrayList<>();
        List<Map.Entry<String, Long>> top = plugin.topList(stat, 5);
        if (top.isEmpty()) lore.add("&8Noch keine Einträge.");
        String[] medal = {"&6", "&7", "&c", "&f", "&f"};
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Long> e = top.get(i);
            String val = time ? plugin.duration(e.getValue()) : e.getValue() + " ⚡";
            lore.add(medal[i] + "#" + (i + 1) + " &f" + e.getKey() + " &8- &e" + val);
        }
        return lore;
    }

    @Override
    protected void build() {
        decorate();
        UUID id = viewer.getUniqueId();
        var data = plugin.getData();

        List<String> server = new ArrayList<>();
        server.add("&7Spieler-Rekord: &e" + data.getInt("records.max-online", 0));
        server.add("&7Aufgestellt am: &f" + data.getString("records.max-online-date", "-"));
        server.add("");
        server.add("&7Aktuell online: &a" + Bukkit.getOnlinePlayers().size());
        server.add("&7Registrierte Spieler: &e" + Bukkit.getOfflinePlayers().length);
        set(11, new ItemBuilder(Material.NETHER_STAR).name("&b&lServer-Rekord").lore(server).glow(true).build());

        List<String> blitze = new ArrayList<>();
        blitze.add("&7Meiste Blitze verdient:");
        blitze.add("");
        blitze.addAll(board("earned", false));
        set(13, new ItemBuilder(Material.LIGHTNING_ROD).name("&e&l⚡ Blitz-Bestenliste").lore(blitze).build());

        List<String> afk = new ArrayList<>();
        afk.add("&7Längste AFK-Zeit:");
        afk.add("");
        afk.addAll(board("afk-seconds", true));
        set(15, new ItemBuilder(Material.CLOCK).name("&a&lAFK-Bestenliste").lore(afk).build());

        long earned = data.getLong("stats." + id + ".earned", 0);
        long afkSec = data.getLong("stats." + id + ".afk-seconds", 0);
        int r1 = plugin.statRank("earned", id);
        int r2 = plugin.statRank("afk-seconds", id);
        List<String> me = new ArrayList<>();
        me.add("&7Blitze verdient: &e" + earned + " ⚡" + (r1 > 0 ? " &8(Platz &6" + r1 + "&8)" : ""));
        me.add("&7AFK-Zeit: &e" + plugin.duration(afkSec) + (r2 > 0 ? " &8(Platz &6" + r2 + "&8)" : ""));
        set(31, new ItemBuilder(Material.PLAYER_HEAD).name("&6&lDeine Werte").lore(me).head(viewer).build());

        set(40, new ItemBuilder(Material.BARRIER).name("&c&lSchließen").build(), e -> viewer.closeInventory());
    }
}
