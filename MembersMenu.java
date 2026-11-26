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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class MembersMenu extends PagedMenu<UUID> {

    public MembersMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Mitglieder";
    }

    @Override
    protected List<UUID> items() {
        Clan clan = plugin.getClanManager().getClan(viewer.getUniqueId());
        return clan == null ? new ArrayList<>() : plugin.getClanManager().getSortedMembers(clan);
    }

    @Override
    protected ItemStack render(UUID id) {
        Clan clan = plugin.getClanManager().getClan(viewer.getUniqueId());
        boolean online = Bukkit.getPlayer(id) != null;
        String name = plugin.getPlayerData().getName(id);
        return new ItemBuilder(Material.PLAYER_HEAD)
                .head(Bukkit.getOfflinePlayer(id))
                .name("&e" + name)
                .lore("&7Rang: " + plugin.getClanManager().rankDisplay(clan.getRank(id)),
                        "&7Status: " + (online ? "&a● Online" : "&c● Offline"),
                        "",
                        "&eKlicke für Aktionen")
                .glow(online)
                .build();
    }

    @Override
    protected void clicked(UUID id, InventoryClickEvent event) {
        navigate(() -> new MemberActionMenu(plugin, viewer, id));
    }

    @Override
    protected Supplier<Menu> backTarget() {
        return () -> new MainMenu(plugin, viewer);
    }
}
