package de.power.bat.storage;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.clan.ClanRank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanStorage {

    private final Power plugin;
    private final File file;

    public ClanStorage(Power plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
    }

    public List<Clan> load() {
        List<Clan> result = new ArrayList<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("clans");
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                UUID id = UUID.fromString(key);
                String name = s.getString("name");
                if (name == null || name.isBlank()) {
                    continue;
                }
                Clan clan = new Clan(id, name, s.getLong("created", System.currentTimeMillis()));
                clan.setTag(s.getString("tag", name));
                clan.setColor(s.getString("color", "&6"));
                clan.setDescription(s.getString("description", ""));
                clan.setKills(s.getInt("kills", 0));
                clan.setDeaths(s.getInt("deaths", 0));
                clan.setPvp(s.getBoolean("pvp", false));
                clan.setMemberLimit(s.getInt("member-limit", 0));

                ConfigurationSection ms = s.getConfigurationSection("members");
                if (ms != null) {
                    for (String memberKey : ms.getKeys(false)) {
                        try {
                            clan.getMembers().put(UUID.fromString(memberKey), ClanRank.fromString(ms.getString(memberKey)));
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Ungültige Mitglieds-UUID in Clan " + name + ": " + memberKey);
                        }
                    }
                }
                if (clan.getMembers().isEmpty()) {
                    plugin.getLogger().warning("Clan " + name + " hat keine Mitglieder und wird übersprungen.");
                    continue;
                }

                UUID leader = null;
                String leaderString = s.getString("leader");
                if (leaderString != null) {
                    try {
                        leader = UUID.fromString(leaderString);
                    } catch (IllegalArgumentException ignored) {
                        // wird unten repariert
                    }
                }
                if (leader == null || !clan.getMembers().containsKey(leader)) {
                    leader = null;
                    for (Map.Entry<UUID, ClanRank> entry : clan.getMembers().entrySet()) {
                        if (leader == null || entry.getValue().isHigherThan(clan.getMembers().get(leader))) {
                            leader = entry.getKey();
                        }
                    }
                }
                clan.setLeader(leader);
                for (Map.Entry<UUID, ClanRank> entry : clan.getMembers().entrySet()) {
                    if (entry.getKey().equals(leader)) {
                        entry.setValue(ClanRank.LEADER);
                    } else if (entry.getValue() == ClanRank.LEADER) {
                        entry.setValue(ClanRank.CO_LEADER);
                    }
                }
                result.add(clan);
            } catch (Exception ex) {
                plugin.getLogger().warning("Clan " + key + " konnte nicht geladen werden: " + ex.getMessage());
            }
        }
        return result;
    }

    public void save(Collection<Clan> clans) {
        YamlConfiguration config = new YamlConfiguration();
        config.createSection("clans");
        for (Clan clan : clans) {
            String path = "clans." + clan.getId() + ".";
            config.set(path + "name", clan.getName());
            config.set(path + "tag", clan.getTag());
            config.set(path + "color", clan.getColor());
            config.set(path + "description", clan.getDescription());
            config.set(path + "leader", clan.getLeader().toString());
            config.set(path + "created", clan.getCreated());
            config.set(path + "kills", clan.getKills());
            config.set(path + "deaths", clan.getDeaths());
            config.set(path + "pvp", clan.isPvp());
            config.set(path + "member-limit", clan.getMemberLimit());
            for (Map.Entry<UUID, ClanRank> entry : clan.getMembers().entrySet()) {
                config.set(path + "members." + entry.getKey(), entry.getValue().name());
            }
        }
        try {
            YamlIO.save(config, file);
        } catch (IOException ex) {
            plugin.getLogger().severe("clans.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
