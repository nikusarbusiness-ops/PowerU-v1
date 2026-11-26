package de.power.bat.utils;

import de.power.Lang;
import de.power.Power;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Lädt messages.yml. Fehlende Schlüssel werden automatisch aus der mitgelieferten Standarddatei ergänzt. */
public class Messages {

    private final Power plugin;
    private FileConfiguration config;

    public Messages(Power plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Konnte Standard-Nachrichten nicht laden: " + ex.getMessage());
        }
    }

    public String raw(String key) {
        String value = config.getString(key);
        return value == null ? "&cFehlende Nachricht: " + key : value;
    }

    /** Rohtext in der Sprache (Sprachdatei), sonst deutscher Text aus messages.yml. */
    public String raw(String lang, String key) {
        String v = Lang.message(lang, key);
        return v != null ? v : raw(key);
    }

    public String prefix() {
        return config.getString("prefix", "&8[&bPower&8] &7");
    }

    private String prefix(String lang) {
        String v = Lang.message(lang, "prefix");
        return v != null ? v : prefix();
    }

    /** Liefert die fertig eingefärbte Nachricht (Standardsprache Deutsch). Platzhalter als Paare: "%name%", "wert", ... */
    public String get(String key, String... placeholders) {
        String text = raw(key).replace("%prefix%", prefix());
        return Colors.color(apply(text, placeholders));
    }

    /** Wie get(), aber in der Sprache des Empfängers. */
    public String getFor(CommandSender to, String key, String... placeholders) {
        String lang = Lang.code(to);
        if (Lang.SOURCE.equals(lang)) {
            return get(key, placeholders);
        }
        String text = raw(lang, key).replace("%prefix%", prefix(lang));
        return Lang.tr(lang, Colors.color(apply(text, placeholders)));
    }

    public List<String> list(String key, String... placeholders) {
        List<String> result = new ArrayList<>();
        for (String line : config.getStringList(key)) {
            result.add(Colors.color(apply(line.replace("%prefix%", prefix()), placeholders)));
        }
        return result;
    }

    public List<String> listFor(CommandSender to, String key, String... placeholders) {
        String lang = Lang.code(to);
        if (Lang.SOURCE.equals(lang)) {
            return list(key, placeholders);
        }
        List<String> src = Lang.messageList(lang, key);
        if (src == null) {
            src = config.getStringList(key);
        }
        List<String> result = new ArrayList<>();
        for (String line : src) {
            result.add(Lang.tr(lang, Colors.color(apply(line.replace("%prefix%", prefix(lang)), placeholders))));
        }
        return result;
    }

    public void send(CommandSender target, String key, String... placeholders) {
        if (raw(key).isEmpty()) {
            return;
        }
        target.sendMessage(getFor(target, key, placeholders));
    }

    private String apply(String text, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String value = placeholders[i + 1];
            text = text.replace(placeholders[i], value == null ? "" : value);
        }
        return text;
    }
}
