package de.power.bat.gui;

import de.power.Power;
import de.power.bat.clan.ClanColor;
import de.power.bat.clan.ClanManager;
import de.power.bat.utils.Colors;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InfoMenu extends ClanMenu {

    public InfoMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Informationen";
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void buildClan() {
        ClanManager cm = plugin.getClanManager();
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = plugin.getPlayerData().getName(clan.getLeader());

        set(10, new ItemBuilder(Material.NAME_TAG)
                .name("&6&lClan-Name")
                .lore("", cm.displayName(clan))
                .build());
        set(11, new ItemBuilder(Material.OAK_SIGN)
                .name("&6&lClan-Tag")
                .lore("", cm.displayTag(clan))
                .build());
        set(12, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&6&lLeader")
                .head(leader)
                .lore("", "&c" + leaderName)
                .build());
        set(13, new ItemBuilder(Material.GOLDEN_HELMET)
                .name("&6&lMitglieder")
                .lore("", "&f" + clan.getMemberCount() + " &7/ &f"
                        + plugin.getClanManager().maxMembers(clan))
                .build());
        set(14, new ItemBuilder(Material.CLOCK)
                .name("&6&lErstellungsdatum")
                .lore("", "&f" + plugin.formatDate(clan.getCreated()))
                .build());
        ClanColor color = ClanColor.fromCode(clan.getColor());
        set(15, new ItemBuilder(color != null ? color.getMaterial() : Material.WHITE_WOOL)
                .name("&6&lClan-Farbe")
                .lore("", clan.getColor() + ClanColor.nameOf(clan.getColor()))
                .build());
        set(16, new ItemBuilder(Material.PAPER)
                .name("&6&lBeschreibung")
                .lore("", clan.getDescription().isEmpty() ? "&8Keine Beschreibung" : "&f" + Colors.color(clan.getDescription()))
                .build());

        List<String> lore = new ArrayList<>();
        List<Player> online = clan.getOnlineMembers();
        lore.add("");
        int shown = 0;
        for (Player player : online) {
            if (shown++ >= 12) {
                lore.add("&7... und &f" + (online.size() - 12) + " &7weitere");
                break;
            }
            lore.add("&a● &f" + player.getName() + " &8(" + cm.rankDisplay(clan.getRank(player.getUniqueId())) + "&8)");
        }
        set(22, new ItemBuilder(Material.LIME_DYE)
                .name("&a&lOnline-Mitglieder &7(" + online.size() + ")")
                .lore(lore)
                .build());

        back(31, () -> new MainMenu(plugin, viewer));
    }
}
