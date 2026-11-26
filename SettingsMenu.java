package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.ClanPermission;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class SettingsMenu extends ClanMenu {

    public SettingsMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Einstellungen";
    }

    @Override
    protected int rows() {
        return 4;
    }

    private void prompt(String key, java.util.function.Consumer<String> action) {
        plugin.getChatInput().request(viewer, key, input -> {
            action.accept(input);
            new SettingsMenu(plugin, viewer).open();
        }, () -> new SettingsMenu(plugin, viewer).open());
    }

    @Override
    protected void buildClan() {
        ClanManager cm = plugin.getClanManager();
        boolean edit = can(ClanPermission.EDIT_SETTINGS);
        String noRight = "Dein Rang darf das nicht";

        if (edit) {
            set(10, new ItemBuilder(Material.NAME_TAG)
                    .name("&6&lClan-Namen ändern")
                    .lore("&7Aktuell: " + cm.displayName(clan), "", "&eKlicke, um einen neuen Namen einzugeben")
                    .build(), event -> prompt("gui.prompt-rename", input -> cm.rename(viewer, input)));
            set(12, new ItemBuilder(Material.CYAN_DYE)
                    .name("&6&lClan-Farbe ändern")
                    .lore("&7Aktuell: " + clan.getColor() + de.power.bat.clan.ClanColor.nameOf(clan.getColor()),
                            "", "&eKlicke, um eine Farbe zu wählen")
                    .build(), event -> navigate(() -> new ColorMenu(plugin, viewer)));
            set(14, new ItemBuilder(Material.WRITABLE_BOOK)
                    .name("&6&lBeschreibung ändern")
                    .lore("&7Aktuell: " + (clan.getDescription().isEmpty() ? "&8keine"
                            : "&f" + de.power.bat.utils.Colors.color(clan.getDescription())),
                            "", "&eKlicke, um eine Beschreibung einzugeben")
                    .build(), event -> prompt("gui.prompt-description", input -> cm.setDescription(viewer, input)));
            set(16, new ItemBuilder(Material.OAK_SIGN)
                    .name("&6&lClan-Tag ändern")
                    .lore("&7Aktuell: " + cm.displayTag(clan), "", "&eKlicke, um ein neues Tag einzugeben")
                    .build(), event -> prompt("gui.prompt-tag", input -> cm.setTag(viewer, input)));
        } else {
            set(10, locked("Clan-Namen ändern", noRight));
            set(12, locked("Clan-Farbe ändern", noRight));
            set(14, locked("Beschreibung ändern", noRight));
            set(16, locked("Clan-Tag ändern", noRight));
        }

        if (edit) {
            set(22, new ItemBuilder(Material.IRON_SWORD)
                    .name("&c&lClan-PvP")
                    .lore("&7PvP zwischen Clan-Mitgliedern:", clan.isPvp() ? "&a● Aktiv" : "&c● Aus", "", "&eKlicke zum Umschalten")
                    .glow(clan.isPvp())
                    .build(), event -> {
                cm.togglePvp(viewer);
                refresh();
            });
        } else {
            set(22, locked("Clan-PvP", noRight));
        }

        if (can(ClanPermission.MANAGE_INVITES)) {
            int pending = plugin.getInviteManager().getForClan(clan.getId()).size();
            set(20, new ItemBuilder(Material.ENDER_EYE)
                    .name("&e&lEinladungen verwalten")
                    .lore("&7Offene Einladungen: &f" + pending, "", "&eKlicke zum Öffnen")
                    .build(), event -> navigate(() -> new ManageInvitesMenu(plugin, viewer)));
        } else {
            set(20, locked("Einladungen verwalten", noRight));
        }

        if (can(ClanPermission.DISBAND)) {
            set(24, new ItemBuilder(Material.TNT)
                    .name("&4&lClan auflösen")
                    .lore("&cLöscht den Clan unwiderruflich!", "", "&eKlicke zum Auflösen")
                    .build(), event -> navigate(() -> new ConfirmMenu(plugin, viewer, "&8Clan auflösen?",
                    List.of("&cDer Clan &f" + clan.getName() + " &cwird gelöscht!", "&cDas kann nicht rückgängig gemacht werden."),
                    () -> {
                        cm.disband(viewer);
                        navigate(() -> MainMenu.create(plugin, viewer));
                    },
                    () -> new SettingsMenu(plugin, viewer))));
        } else {
            set(24, locked("Clan auflösen", "Nur der Leader darf den Clan auflösen"));
        }

        back(31, () -> new MainMenu(plugin, viewer));
    }
}
