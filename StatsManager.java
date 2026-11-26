package de.power.bat.storage;

import de.power.Power;
import de.power.bat.clan.Clan;

import java.util.UUID;

public class StatsManager {

    private final Power plugin;

    public StatsManager(Power plugin) {
        this.plugin = plugin;
    }

    public void recordDeath(UUID victim) {
        PlayerData data = plugin.getPlayerData().get(victim);
        data.setDeaths(data.getDeaths() + 1);
        plugin.getPlayerData().markDirty();

        Clan clan = plugin.getClanManager().getClan(victim);
        if (clan != null) {
            clan.addDeath();
            plugin.getClanManager().markDirty();
        }
    }

    public void recordKill(UUID killer, UUID victim) {
        PlayerData data = plugin.getPlayerData().get(killer);
        data.setKills(data.getKills() + 1);

        Clan killerClan = plugin.getClanManager().getClan(killer);
        if (killerClan != null) {
            Clan victimClan = plugin.getClanManager().getClan(victim);
            // Kills gegen eigene Clan-Mitglieder zählen nicht als Clan-Kills
            if (victimClan == null || !victimClan.getId().equals(killerClan.getId())) {
                data.setClanKills(data.getClanKills() + 1);
                killerClan.addKill();
                plugin.getClanManager().markDirty();
            }
        }
        plugin.getPlayerData().markDirty();
    }
}
