package de.power;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistente Spielerdaten (Sprache, Einstellungen, Spielzeit, Daily-Streak).
 * Liegt komplett im Speicher und wird regelmaessig (asynchron) gespeichert.
 */
public final class PlayerStore {

    private final Power pl;
    private final File file;
    private final YamlConfiguration cfg;
    private final Map<UUID, Long> playtime = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public PlayerStore(Power pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "userdata.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("p");
        if (root != null) {
            for (String k : root.getKeys(false)) {
                try {
                    long t = root.getLong(k + ".playtime", 0L);
                    if (t > 0) playtime.put(UUID.fromString(k), t);
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private String path(UUID id, String key) { return "p." + id + "." + key; }

    public boolean has(UUID id, String key) { return cfg.contains(path(id, key)); }

    public String getString(UUID id, String key, String def) { return cfg.getString(path(id, key), def); }

    public void setString(UUID id, String key, String value) {
        cfg.set(path(id, key), value);
        dirty = true;
    }

    public long getLong(UUID id, String key, long def) { return cfg.getLong(path(id, key), def); }

    public void setLong(UUID id, String key, long value) {
        cfg.set(path(id, key), value);
        dirty = true;
    }

    public int getInt(UUID id, String key, int def) { return cfg.getInt(path(id, key), def); }

    public void setInt(UUID id, String key, int value) {
        cfg.set(path(id, key), value);
        dirty = true;
    }

    // ---------------------------------------------------------------- Spielzeit (Sekunden)

    public long playtime(UUID id) { return playtime.getOrDefault(id, 0L); }

    public void addPlaytime(UUID id, long seconds) {
        playtime.merge(id, seconds, Long::sum);
        dirty = true;
    }

    public Map<UUID, Long> allPlaytime() { return playtime; }

    // ---------------------------------------------------------------- Speichern

    private void flushPlaytime() {
        for (Map.Entry<UUID, Long> e : playtime.entrySet()) cfg.set(path(e.getKey(), "playtime"), e.getValue());
    }

    /** Synchron speichern (beim Abschalten). */
    public synchronized void saveNow() {
        flushPlaytime();
        write(cfg.saveToString());
        dirty = false;
    }

    /** Asynchron speichern, wenn sich etwas geaendert hat. */
    public void saveAsyncIfDirty() {
        if (!dirty) return;
        flushPlaytime();
        final String data = cfg.saveToString();
        dirty = false;
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> write(data));
    }

    private synchronized void write(String data) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            File tmp = new File(file.getPath() + ".tmp");
            Files.writeString(tmp.toPath(), data, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            pl.getLogger().severe("userdata.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
