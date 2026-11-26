package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.ClanPermission;
import de.power.bat.clan.ClanRank;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Rangverwaltung: Linksklick = befördern, Rechtsklick = degradieren, Shift+Linksklick = Leitung übertragen. */
public class RankMenu extends PagedMenu<UUID> {

    public RankMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Rangverwaltung";
    }

    @Override
    protected List<UUID> items() {
        Clan clan = plugin.getClanManager().getClan(viewer.getUniqueId());
        return clan == null ? new ArrayList<>() : plugin.getClanManager().getSortedMembers(clan);
    }

    @Override
    protected ItemStack render(UUID id) {
        ClanManager cm = plugin.getClanManager();
        Clan clan = cm.getClan(viewer.getUniqueId());
        ClanRank rank = clan.getRank(id);
        ClanRank mine = clan.getRank(viewer.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add("&7Rang: " + cm.rankDisplay(rank));
        lore.add("");
        boolean self = id.equals(viewer.getUniqueId());
        if (self || !mine.isHigherThan(rank)) {
            lore.add("&8Keine Aktionen möglich");
        } else {
            if (cm.hasPermission(clan, viewer.getUniqueId(), ClanPermission.PROMOTE)
                    && rank.promoted() != null && mine.isHigherThan(rank.promoted())) {
                lore.add("&a▶ Linksklick: Befördern &7(" + cm.rankDisplay(rank.promoted()) + "&7)");
            }
            if (cm.hasPermission(clan, viewer.getUniqueId(), ClanPermission.DEMOTE) && rank.demoted() != null) {
                lore.add("&6▶ Rechtsklick: Degradieren &7(" + cm.rankDisplay(rank.demoted()) + "&7)");
            }
            if (mine == ClanRank.LEADER) {
                lore.add("&4▶ Shift+Linksklick: Leitung übertragen");
            }
        }
        return new ItemBuilder(Material.PLAYER_HEAD)
                .head(Bukkit.getOfflinePlayer(id))
                .name("&e" + plugin.getPlayerData().getName(id))
                .lore(lore)
                .build();
    }

    @Override
    protected void clicked(UUID id, InventoryClickEvent event) {
        ClanManager cm = plugin.getClanManager();
        ClickType click = event.getClick();
        if (click == ClickType.SHIFT_LEFT) {
            String name = plugin.getPlayerData().getName(id);
            navigate(() -> new ConfirmMenu(plugin, viewer, "&8Leitung übertragen?",
                    List.of("&7Neuer Leader: &f" + name, "&cDu wirst &fCo-Leader&c!"),
                    () -> {
                        cm.transfer(viewer, id);
                        navigate(() -> MainMenu.create(plugin, viewer));
                    },
                    () -> new RankMenu(plugin, viewer)));
            return;
        }
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            cm.demote(viewer, id);
        } else {
            cm.promote(viewer, id);
        }
        refresh();
    }

    @Override
    protected Supplier<Menu> backTarget() {
        return () -> new MainMenu(plugin, viewer);
    }
}
