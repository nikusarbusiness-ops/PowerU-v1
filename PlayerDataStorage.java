package de.power.bat.storage;

import de.power.Power;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataStorage {

    private final Power plugin;
    private final File file;
    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public PlayerDataStorage(Power plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        data.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                UUID id = UUID.fromString(key);
                PlayerData pd = new PlayerData(id, s.getString("name"));
                pd.setKills(s.getInt("kills", 0));
                pd.setDeaths(s.getInt("deaths", 0));
                pd.setClanKills(s.getInt("clan-kills", 0));
                pd.setLastSeen(s.getLong("last-seen", 0L));
                data.put(id, pd);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ungültiger Eintrag in players.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.createSection("players");
        for (PlayerData pd : data.values()) {
            String path = "players." + pd.getUuid() + ".";
            config.set(path + "name", pd.getName());
            config.set(path + "kills", pd.getKills());
            config.set(path + "deaths", pd.getDeaths());
            config.set(path + "clan-kills", pd.getClanKills());
            config.set(path + "last-seen", pd.getLastSeen());
        }
        try {
            YamlIO.save(config, file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("players.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public PlayerData get(UUID id) {
        return data.computeIfAbsent(id, key -> {
            dirty = true;
            return new PlayerData(key, Bukkit.getOfflinePlayer(key).getName());
        });
    }

    public void update(Player player) {
        PlayerData pd = get(player.getUniqueId());
        if (!player.getName().equals(pd.getName())) {
            pd.setName(player.getName());
            dirty = true;
        }
    }

    /** Sucht eine bekannte UUID anhand des Namens (Groß-/Kleinschreibung egal) oder null. */
    public UUID findUuid(String name) {
        for (PlayerData pd : data.values()) {
            if (pd.getName() != null && pd.getName().equalsIgnoreCase(name)) {
                return pd.getUuid();
            }
        }
        return null;
    }

    /** Letzter bekannter Name eines Spielers (nie null). */
    public String getName(UUID id) {
        PlayerData pd = data.get(id);
        if (pd != null && pd.getName() != null) {
            return pd.getName();
        }
        String name = Bukkit.getOfflinePlayer(id).getName();
        if (name != null) {
            return name;
        }
        String s = id.toString();
        return s.substring(0, 8);
    }
}
