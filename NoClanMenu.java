package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.ClanManager;
import de.power.bat.clan.InviteManager;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Hauptmenü für Spieler ohne Clan. */
public class NoClanMenu extends Menu {

    public NoClanMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Willkommen";
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void build() {
        decorate();
        ClanManager cm = plugin.getClanManager();
        List<InviteManager.Invite> invites = plugin.getInviteManager().getFor(viewer.getUniqueId());
        boolean canCreate = cm.canCreate(viewer);

        set(11, new ItemBuilder(Material.EMERALD)
                .name("&a&lClan erstellen")
                .lore("&7Gründe deinen eigenen Clan und", "&7werde automatisch &4Leader&7.", "",
                        canCreate ? "&eKlicke, um einen Namen einzugeben" : "&c✘ Dir fehlt die Berechtigung dafür")
                .glow(canCreate)
                .build(), event -> {
            if (!cm.canCreate(viewer)) {
                plugin.getMessages().send(viewer, "clan.error.no-create-permission");
                return;
            }
            plugin.getChatInput().request(viewer, "gui.prompt-create", input -> {
                cm.createClan(viewer, input);
                MainMenu.create(plugin, viewer).open();
            }, () -> MainMenu.create(plugin, viewer).open());
        });

        set(13, new ItemBuilder(Material.LIME_DYE)
                .name("&a&lEinladung annehmen")
                .lore("&7Nimmt deine neueste Einladung an.", "",
                        "&7Offene Einladungen: &f" + invites.size(),
                        invites.isEmpty() ? "&c✘ Du hast keine Einladung" : "&eKlicke zum Annehmen")
                .glow(!invites.isEmpty())
                .build(), event -> {
            cm.acceptInvite(viewer, null);
            navigate(() -> MainMenu.create(plugin, viewer));
        });

        set(15, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&e&lEinladungen ansehen")
                .lore("&7Alle Einladungen auf einen Blick.", "",
                        "&7Offene Einladungen: &f" + invites.size(), "&eKlicke zum Öffnen")
                .build(), event -> navigate(() -> new InvitationsMenu(plugin, viewer)));
    }
}
