package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Online-Spieler ohne Clan, die eingeladen werden können. */
public class InviteMenu extends PagedMenu<Player> {

    public InviteMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Spieler einladen";
    }

    @Override
    protected List<Player> items() {
        List<Player> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(viewer.getUniqueId())
                    && plugin.getClanManager().getClan(player.getUniqueId()) == null) {
                list.add(player);
            }
        }
        list.sort(Comparator.comparing(p -> p.getName().toLowerCase()));
        return list;
    }

    @Override
    protected String emptyText() {
        return "&7Aktuell ist kein einladbarer Spieler online.";
    }

    @Override
    protected ItemStack render(Player player) {
        Clan clan = plugin.getClanManager().getClan(viewer.getUniqueId());
        boolean invited = clan != null && plugin.getInviteManager().has(clan.getId(), player.getUniqueId());
        return new ItemBuilder(Material.PLAYER_HEAD)
                .head(player)
                .name("&e" + player.getName())
                .lore(invited ? List.of("&a✔ Bereits eingeladen", "&7Warte auf die Antwort.")
                                : List.of("&7Noch in keinem Clan.", "", "&eKlicke zum Einladen"))
                .glow(invited)
                .build();
    }

    @Override
    protected void clicked(Player player, InventoryClickEvent event) {
        if (!player.isOnline()) {
            refresh();
            return;
        }
        plugin.getClanManager().invite(viewer, player);
        refresh();
    }

    @Override
    protected Supplier<Menu> backTarget() {
        return () -> new MainMenu(plugin, viewer);
    }
}
