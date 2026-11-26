
package de.power;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Lang {

    public static final String SOURCE = "de";

    public static final class Info {
        public final String code;
        public final String name;
        public final Material icon;
        final YamlConfiguration messages;
        final Map<Character, List<String[]>> index = new HashMap<>();
        int phraseCount;

        Info(String code, String name, Material icon, YamlConfiguration messages) {
            this.code = code;
            this.name = name;
            this.icon = icon;
            this.messages = messages;
        }
    }

    private static Power pl;
    private static final Map<String, Info> langs = new LinkedHashMap<>();
    private static String defaultLang = SOURCE;

    private Lang() { }

    public static void init(Power plugin) {
        pl = plugin;
        reload();
    }

    private static YamlConfiguration bundled(String code) {
        try (InputStream in = pl.getResource("lang/" + code + ".yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public static void reload() {
        langs.clear();

        defaultLang = pl.getConfig()
                .getString("language.default", SOURCE)
                .toLowerCase(Locale.ROOT);

        File dir = new File(pl.getDataFolder(), "lang");

        if (!dir.exists()) dir.mkdirs();

        for (String c : List.of("de", "en")) {
            File f = new File(dir, c + ".yml");

            if (!f.exists() && pl.getResource("lang/" + c + ".yml") != null) {
                pl.saveResource("lang/" + c + ".yml", false);
            }
        }

        List<String> codes = pl.getConfig().getStringList("language.available");

        if (codes.isEmpty()) codes = List.of("de", "en");

        for (String raw : codes) {
            String code = raw.toLowerCase(Locale.ROOT);

            YamlConfiguration user = null;
            File f = new File(dir, code + ".yml");

            if (f.exists()) {
                user = YamlConfiguration.loadConfiguration(f);
            }

            YamlConfiguration base = bundled(code);

            if (user == null && base == null) {
                pl.getLogger().warning(
                        "Sprachdatei lang/" + code + ".yml nicht gefunden - Sprache wird uebersprungen."
                );
                continue;
            }

            YamlConfiguration main = user != null ? user : base;

            String name = main.getString("name", code);
            Material icon = Material.matchMaterial(main.getString("icon", "PAPER"));

            if (icon == null || !icon.isItem()) {
                icon = Material.PAPER;
            }

            YamlConfiguration messages = new YamlConfiguration();

            copySection(base, "messages", messages);
            copySection(user, "messages", messages);

            Info info = new Info(code, name, icon, messages);

            if (!code.equals(SOURCE)) {
                Map<String, String> phrases = new HashMap<>();

                readPhrases(base, phrases);
                readPhrases(user, phrases);

                build(info, phrases);
            }

            langs.put(code, info);
        }

        if (!langs.containsKey(SOURCE)) {
            langs.put(SOURCE, new Info(
                    SOURCE, "Deutsch", Material.PAPER, new YamlConfiguration()
            ));
        }

        if (!langs.containsKey(defaultLang)) {
            defaultLang = SOURCE;
        }
    }

    private static void copySection(
            YamlConfiguration from,
            String section,
            YamlConfiguration to
    ) {
        if (from == null) return;

        ConfigurationSection s = from.getConfigurationSection(section);

        if (s == null) return;

        for (String key : s.getKeys(true)) {
            if (s.isConfigurationSection(key)) continue;
            to.set(key, s.get(key));
        }
    }

    private static void readPhrases(
            YamlConfiguration y,
            Map<String, String> out
    ) {
        if (y == null) return;

        ConfigurationSection s = y.getConfigurationSection("phrases");

        if (s == null) return;

        for (String key : s.getKeys(false)) {
            String v = s.getString(key);

            if (v != null && !key.isEmpty()) {
                out.put(key.replace('§', '&'), v);
            }
        }
    }

    private static void build(Info info, Map<String, String> phrases) {
        List<String[]> all = new ArrayList<>();

        for (Map.Entry<String, String> e : phrases.entrySet()) {
            all.add(new String[]{e.getKey(), e.getValue()});
        }

        all.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));

        for (String[] e : all) {
            info.index.computeIfAbsent(
                    e[0].charAt(0),
                    k -> new ArrayList<>()
            ).add(e);
        }

        info.phraseCount = all.size();
    }

    // Abfragen

    public static Collection<Info> available() {
        return langs.values();
    }

    public static boolean exists(String code) {
        return code != null && langs.containsKey(code.toLowerCase(Locale.ROOT));
    }

    public static Info info(String code) {
        return langs.get(code);
    }

    public static String defaultCode() {
        return defaultLang;
    }

    public static String code(CommandSender s) {
        if (s instanceof Player p) return code(p.getUniqueId());
        return SOURCE;
    }

    public static String code(UUID id) {
        String c = pl.store().getString(id, "lang", null);

        if (c != null && langs.containsKey(c)) return c;

        return defaultLang;
    }

    public static boolean hasChosen(UUID id) {
        return pl.store().has(id, "lang");
    }

    public static void set(UUID id, String code) {
        if (langs.containsKey(code)) {
            pl.store().setString(id, "lang", code);
        }
    }

    // Übersetzen

    public static String tr(CommandSender s, String text) {
        return tr(code(s), text);
    }

    public static String tr(UUID id, String text) {
        return tr(code(id), text);
    }

    public static String tr(String lang, String text) {
        if (text == null || text.isEmpty() || SOURCE.equals(lang)) {
            return text;
        }

        Info info = langs.get(lang);

        if (info == null || info.index.isEmpty()) {
            return text;
        }

        String t = text.indexOf('§') >= 0
                ? text.replace('§', '&')
                : text;

        int n = t.length();

        StringBuilder sb = null;
        int last = 0;
        int i = 0;

        while (i < n) {
            List<String[]> cand = info.index.get(t.charAt(i));
            boolean hit = false;

            if (cand != null) {
                for (String[] e : cand) {
                    if (t.startsWith(e[0], i)) {
                        if (sb == null) {
                            sb = new StringBuilder(n + 16);
                        }

                        sb.append(t, last, i).append(e[1]);

                        i += e[0].length();
                        last = i;
                        hit = true;
                        break;
                    }
                }
            }

            if (!hit) i++;
        }

        if (sb == null) return text;

        sb.append(t, last, n);

        return sb.toString().replace('&', '§');
    }

    public static List<String> tr(CommandSender s, List<String> lines) {
        String l = code(s);

        if (SOURCE.equals(l)) return lines;

        List<String> out = new ArrayList<>(lines.size());

        for (String line : lines) {
            out.add(tr(l, line));
        }

        return out;
    }

    public static ItemStack tr(CommandSender s, ItemStack it) {
        String l = code(s);

        if (SOURCE.equals(l) || it == null || it.getType() == Material.AIR) {
            return it;
        }

        ItemMeta m = it.getItemMeta();

        if (m == null) return it;

        if (m.hasDisplayName()) {
            m.setDisplayName(tr(l, m.getDisplayName()));
        }

        if (m.hasLore() && m.getLore() != null) {
            List<String> translatedLore = new ArrayList<>();

            for (String line : m.getLore()) {
                translatedLore.add(tr(l, line));
            }

            m.setLore(translatedLore);
        }

        it.setItemMeta(m);

        return it;
    }

    /** Übersetzt alle Items eines Inventars für den Spieler. */
    public static void translate(Inventory inv, Player p) {
        if (inv == null || SOURCE.equals(code(p))) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);

            if (it == null || it.getType() == Material.AIR) continue;

            inv.setItem(i, tr(p, it));
        }
    }

    /** Inventar übersetzen und öffnen. */
    public static void open(Player p, Inventory inv) {
        translate(inv, p);
        p.openInventory(inv);
    }

    // Nachrichten

    public static String message(String lang, String key) {
        if (SOURCE.equals(lang)) return null;

        Info info = langs.get(lang);

        if (info == null) return null;

        return info.messages.getString(key);
    }

    public static List<String> messageList(String lang, String key) {
        if (SOURCE.equals(lang)) return null;

        Info info = langs.get(lang);

        if (info == null || !info.messages.isList(key)) return null;

        return info.messages.getStringList(key);
    }

    public static int phraseCount(String code) {
        Info i = langs.get(code);

        return i == null ? 0 : i.phraseCount;
    }

    /** Nur für Logs/Tests. */
    public static void logStatus() {
        for (Info i : langs.values()) {
            pl.getLogger().info(
                    "Sprache " + i.code + " (" + i.name + "): "
                            + i.phraseCount + " Phrasen."
            );
        }

        Bukkit.getLogger().fine("Lang geladen.");
    }
}
