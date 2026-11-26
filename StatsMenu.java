package de.power.bat.gui;

import de.power.Power;
import de.power.bat.storage.PlayerData;
import de.power.bat.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class StatsMenu extends ClanMenu {

    public StatsMenu(Power plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected String title() {
        return "&8Clan &7» &6Statistiken";
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void buildClan() {
        set(10, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&c&lClan-Kills")
                .lore("", "&f" + clan.getKills(), "&8Kills aller Mitglieder gegen Nicht-Mitglieder")
                .build());
        set(12, new ItemBuilder(Material.SKELETON_SKULL)
                .name("&4&lClan-Tode")
                .lore("", "&f" + clan.getDeaths())
                .build());
        set(14, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("&e&lClan-K/D")
                .lore("", "&f" + String.format(Locale.ROOT, "%.2f", clan.getKd()))
                .build());
        set(16, new ItemBuilder(Material.GOLDEN_HELMET)
                .name("&6&lClan-Mitglieder")
                .lore("", "&f" + clan.getMemberCount() + " &7/ &f"
                        + plugin.getClanManager().maxMembers(clan),
                        "&7Online: &a" + clan.getOnlineMembers().size())
                .build());
        set(20, new ItemBuilder(Material.CLOCK)
                .name("&6&lErstellungsdatum")
                .lore("", "&f" + plugin.formatDate(clan.getCreated()))
                .build());

        PlayerData mine = plugin.getPlayerData().get(viewer.getUniqueId());
        set(22, new ItemBuilder(Material.PLAYER_HEAD)
                .head(viewer)
                .name("&a&lDeine Statistiken")
                .lore("", "&7Kills: &f" + mine.getKills(),
                        "&7Tode: &f" + mine.getDeaths(),
                        "&7K/D: &f" + String.format(Locale.ROOT, "%.2f", mine.getKd()),
                        "&7Clan-Kills: &f" + mine.getClanKills())
                .build());

        UUID top = null;
        int topKills = -1;
        for (UUID member : clan.getMembers().keySet()) {
            int kills = plugin.getPlayerData().get(member).getKills();
            if (kills > topKills) {
                topKills = kills;
                top = member;
            }
        }
        set(24, new ItemBuilder(Material.NETHERITE_SWORD)
                .name("&d&lTop-Killer des Clans")
                .lore("", top == null ? "&8-" : "&f" + plugin.getPlayerData().getName(top) + " &7(&f" + topKills + " &7Kills)")
                .build());

        back(31, () -> new MainMenu(plugin, viewer));
    }
}
