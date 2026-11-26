package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.InviteManager;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Offene Einladungen des Clans; Klick zieht eine Einladung zurück. */
public class ManageInvitesMenu extends PagedMenu<InviteManager.Invite> {

    public ManageInvitesMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Offene Einladungen";
    }

    @Override
    protected List<InviteManager.Invite> items() {
        Clan clan = plugin.getClanManager().getClan(viewer.getUniqueId());
        return clan == null ? new ArrayList<>() : plugin.getInviteManager().getForClan(clan.getId());
    }

    @Override
    protected String emptyText() {
        return "&7Es sind keine Einladungen offen.";
    }

    @Override
    protected ItemStack render(InviteManager.Invite invite) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .head(Bukkit.getOfflinePlayer(invite.target()))
                .name("&e" + plugin.getPlayerData().getName(invite.target()))
                .lore("&7Eingeladen von: &f" + plugin.getPlayerData().getName(invite.inviter()),
                        "&7Läuft ab in: &f" + invite.secondsLeft() + "s",
                        "",
                        "&c▶ Klicke, um die Einladung zurückzuziehen")
                .build();
    }

    @Override
    protected void clicked(InviteManager.Invite invite, InventoryClickEvent event) {
        plugin.getClanManager().cancelInvite(viewer, invite.target());
        refresh();
    }

    @Override
    protected Supplier<Menu> backTarget() {
        return () -> new SettingsMenu(plugin, viewer);
    }
}
