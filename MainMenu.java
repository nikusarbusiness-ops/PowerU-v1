package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.ClanPermission;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Clan-Hauptmenü. */
public class MainMenu extends ClanMenu {

    public MainMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    /** Liefert je nach Clan-Zugehörigkeit das passende Hauptmenü. */
    public static Menu create(Power plugin, Player player) {
        if (plugin.getClanManager().getClan(player.getUniqueId()) == null) {
            return new NoClanMenu(plugin, player);
        }
        return new MainMenu(plugin, player);
    }

    public static void openFor(Power plugin, Player player) {
        create(plugin, player).open();
    }

    @Override
    protected String title() {
        Clan c = currentClan();
        return "&8Clan &7» &6" + (c == null ? "Menü" : c.getName());
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void buildClan() {
        ClanManager cm = plugin.getClanManager();
        boolean chat = cm.isChatMode(viewer.getUniqueId());

        set(4, new ItemBuilder(Material.NETHER_STAR)
                .name(cm.displayName(clan))
                .lore("&7Tag: " + cm.displayTag(clan),
                        "&7Dein Rang: " + cm.rankDisplay(clan.getRank(viewer.getUniqueId())),
                        "&7Mitglieder: &f" + clan.getMemberCount())
                .glow(true)
                .build());

        set(11, new ItemBuilder(Material.BOOK)
                .name("&6&lClan-Informationen")
                .lore("&7Name, Tag, Leader, Farbe und mehr.", "", "&eKlicke zum Öffnen")
                .build(), event -> navigate(() -> new InfoMenu(plugin, viewer)));

        set(13, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&e&lClan-Mitglieder")
                .head(viewer)
                .lore("&7Alle Mitglieder mit Rang und Status.", "",
                        "&7Online: &a" + clan.getOnlineMembers().size() + "&7/&f" + clan.getMemberCount(),
                        "&eKlicke zum Öffnen")
                .build(), event -> navigate(() -> new MembersMenu(plugin, viewer)));

        if (can(ClanPermission.INVITE)) {
            set(15, new ItemBuilder(Material.WRITABLE_BOOK)
                    .name("&a&lSpieler einladen")
                    .lore("&7Lade Online-Spieler in deinen Clan ein.", "", "&eKlicke zum Öffnen")
                    .build(), event -> navigate(() -> new InviteMenu(plugin, viewer)));
        } else {
            set(15, locked("Spieler einladen", "Dein Rang darf keine Spieler einladen"));
        }

        if (can(ClanPermission.EDIT_SETTINGS) || can(ClanPermission.MANAGE_INVITES)) {
            set(20, new ItemBuilder(Material.COMPARATOR)
                    .name("&6&lClan-Einstellungen")
                    .lore("&7Name, Farbe, Tag, Beschreibung", "&7und Einladungen verwalten.", "", "&eKlicke zum Öffnen")
                    .build(), event -> navigate(() -> new SettingsMenu(plugin, viewer)));
        } else {
            set(20, locked("Clan-Einstellungen", "Dein Rang darf keine Einstellungen ändern"));
        }

        if (can(ClanPermission.PROMOTE) || can(ClanPermission.DEMOTE) || can(ClanPermission.TRANSFER)) {
            set(22, new ItemBuilder(Material.GOLDEN_HELMET)
                    .name("&e&lClan-Rangverwaltung")
                    .lore("&7Mitglieder befördern oder degradieren.", "", "&eKlicke zum Öffnen")
                    .build(), event -> navigate(() -> new RankMenu(plugin, viewer)));
        } else {
            set(22, locked("Clan-Rangverwaltung", "Dein Rang darf keine Ränge verwalten"));
        }

        set(24, new ItemBuilder(Material.PAPER)
                .name("&b&lClan-Chat")
                .lore("&7Schreibe nur für deinen Clan.", "",
                        "&7Status: " + (chat ? "&aAktiv" : "&cInaktiv"),
                        "&eKlicke zum Umschalten")
                .glow(chat)
                .build(), event -> {
            cm.toggleChat(viewer);
            refresh();
        });

        set(30, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&c&lClan-Statistiken")
                .lore("&7Kills, Tode, K/D und mehr.", "", "&eKlicke zum Öffnen")
                .build(), event -> navigate(() -> new StatsMenu(plugin, viewer)));

        boolean leader = clan.getLeader().equals(viewer.getUniqueId());
        set(32, new ItemBuilder(Material.OAK_DOOR)
                .name("&4&lClan verlassen")
                .lore(leader
                        ? List.of("&7Als &4Leader &7kannst du nicht gehen.", "&7Übertrage zuerst die Leitung.")
                        : List.of("&7Verlasse deinen aktuellen Clan.", "", "&eKlicke zum Verlassen"))
                .build(), event -> {
            if (leader) {
                plugin.getMessages().send(viewer, "clan.error.leader-cannot-leave");
                return;
            }
            navigate(() -> new ConfirmMenu(plugin, viewer, "&8Clan verlassen?",
                    List.of("&7Du verlässt &f" + clan.getName() + "&7."),
                    () -> {
                        cm.leave(viewer);
                        navigate(() -> create(plugin, viewer));
                    },
                    () -> new MainMenu(plugin, viewer)));
        });
    }
}
