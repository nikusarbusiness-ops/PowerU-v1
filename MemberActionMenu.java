package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.ClanPermission;
import de.power.bat.clan.ClanRank;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Aktionen für ein Clan-Mitglied - nur Aktionen, für die der Betrachter Rechte hat. */
public class MemberActionMenu extends ClanMenu {

    private final UUID target;

    public MemberActionMenu(Power plugin, Player viewer, UUID target) {
        super(plugin, viewer);
        this.target = target;
    }

    @Override
    protected String title() {
        return "&8Mitglied &7» &6" + plugin.getPlayerData().getName(target);
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void buildClan() {
        ClanManager cm = plugin.getClanManager();
        ClanRank targetRank = clan.getRank(target);
        if (targetRank == null) {
            navigate(() -> new MembersMenu(plugin, viewer));
            return;
        }
        String name = plugin.getPlayerData().getName(target);
        ClanRank myRank = clan.getRank(viewer.getUniqueId());
        boolean self = target.equals(viewer.getUniqueId());
        boolean online = Bukkit.getPlayer(target) != null;

        set(4, new ItemBuilder(Material.PLAYER_HEAD)
                .head(Bukkit.getOfflinePlayer(target))
                .name("&e&l" + name)
                .lore("&7Rang: " + cm.rankDisplay(targetRank),
                        "&7Status: " + (online ? "&a● Online" : "&c● Offline"))
                .build());

        boolean any = false;
        if (!self) {
            if (can(ClanPermission.PROMOTE) && myRank.isHigherThan(targetRank)
                    && targetRank.promoted() != null && myRank.isHigherThan(targetRank.promoted())) {
                any = true;
                set(10, new ItemBuilder(Material.LIME_DYE)
                        .name("&a&lBefördern")
                        .lore("&7Neuer Rang: " + cm.rankDisplay(targetRank.promoted()), "", "&eKlicke zum Befördern")
                        .build(), event -> {
                    cm.promote(viewer, target);
                    refresh();
                });
            }
            if (can(ClanPermission.DEMOTE) && myRank.isHigherThan(targetRank) && targetRank.demoted() != null) {
                any = true;
                set(12, new ItemBuilder(Material.ORANGE_DYE)
                        .name("&6&lDegradieren")
                        .lore("&7Neuer Rang: " + cm.rankDisplay(targetRank.demoted()), "", "&eKlicke zum Degradieren")
                        .build(), event -> {
                    cm.demote(viewer, target);
                    refresh();
                });
            }
            if (can(ClanPermission.KICK) && myRank.isHigherThan(targetRank)) {
                any = true;
                set(14, new ItemBuilder(Material.REDSTONE)
                        .name("&c&lAus dem Clan werfen")
                        .lore("&7Entfernt &f" + name + " &7aus dem Clan.", "", "&eKlicke zum Entfernen")
                        .build(), event -> navigate(() -> new ConfirmMenu(plugin, viewer, "&8Spieler entfernen?",
                        List.of("&7Möchtest du &f" + name + " &7entfernen?"),
                        () -> {
                            cm.kick(viewer, target);
                            navigate(() -> new MembersMenu(plugin, viewer));
                        },
                        () -> new MemberActionMenu(plugin, viewer, target))));
            }
            if (myRank == ClanRank.LEADER) {
                any = true;
                set(16, new ItemBuilder(Material.NETHER_STAR)
                        .name("&4&lLeitung übertragen")
                        .lore("&7Macht &f" + name + " &7zum neuen &4Leader&7.",
                                "&7Du wirst &cCo-Leader&7.", "", "&eKlicke zum Übertragen")
                        .build(), event -> navigate(() -> new ConfirmMenu(plugin, viewer, "&8Leitung übertragen?",
                        List.of("&7Neuer Leader: &f" + name, "&cDas kann nur der neue Leader rückgängig machen!"),
                        () -> {
                            cm.transfer(viewer, target);
                            navigate(() -> MainMenu.create(plugin, viewer));
                        },
                        () -> new MemberActionMenu(plugin, viewer, target))));
            }
        }
        if (!any) {
            set(13, new ItemBuilder(Material.BARRIER)
                    .name("&c&lKeine Aktionen verfügbar")
                    .lore(self ? "&7Du kannst dich nicht selbst verwalten."
                            : "&7Dir fehlen die Rechte für dieses Mitglied.")
                    .build());
        }
        back(22, () -> new MembersMenu(plugin, viewer));
    }
}
