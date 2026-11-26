package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.InviteManager;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Eigene Einladungen: Linksklick = annehmen, Rechtsklick = ablehnen. */
public class InvitationsMenu extends PagedMenu<InviteManager.Invite> {

    public InvitationsMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Einladungen";
    }

    @Override
    protected List<InviteManager.Invite> items() {
        List<InviteManager.Invite> list = new ArrayList<>();
        for (InviteManager.Invite invite : plugin.getInviteManager().getFor(viewer.getUniqueId())) {
            if (plugin.getClanManager().getClanById(invite.clanId()) != null) {
                list.add(invite);
            }
        }
        return list;
    }

    @Override
    protected String emptyText() {
        return "&7Du hast aktuell keine Einladungen.";
    }

    @Override
    protected ItemStack render(InviteManager.Invite invite) {
        Clan clan = plugin.getClanManager().getClanById(invite.clanId());
        return new ItemBuilder(Material.PAPER)
                .name(plugin.getClanManager().displayName(clan))
                .lore("&7Tag: " + plugin.getClanManager().displayTag(clan),
                        "&7Eingeladen von: &f" + plugin.getPlayerData().getName(invite.inviter()),
                        "&7Mitglieder: &f" + clan.getMemberCount(),
                        "&7Läuft ab in: &f" + invite.secondsLeft() + "s",
                        "",
                        "&a▶ Linksklick: Annehmen",
                        "&c▶ Rechtsklick: Ablehnen")
                .build();
    }

    @Override
    protected void clicked(InviteManager.Invite invite, InventoryClickEvent event) {
        Clan clan = plugin.getClanManager().getClanById(invite.clanId());
        if (clan == null) {
            refresh();
            return;
        }
        ClickType click = event.getClick();
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            plugin.getClanManager().denyInvite(viewer, clan.getName());
            refresh();
        } else {
            plugin.getClanManager().acceptInvite(viewer, clan.getName());
            navigate(() -> MainMenu.create(plugin, viewer));
        }
    }

    @Override
    protected Supplier<Menu> backTarget() {
        return () -> MainMenu.create(plugin, viewer);
    }
}
