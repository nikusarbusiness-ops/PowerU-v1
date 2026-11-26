package de.power;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Spieler-Einstellungen (/settings). Jede Einstellung hier wird im Plugin tatsaechlich ausgewertet. */
public final class Settings {

    public static final class Def {
        public final String key;
        public final Material icon;
        public final String name;
        public final List<String> desc;
        public final String[] options;
        public final String def;

        Def(String key, Material icon, String name, List<String> desc, String[] options, String def) {
            this.key = key;
            this.icon = icon;
            this.name = name;
            this.desc = desc;
            this.options = options;
            this.def = def;
        }

        public boolean isAction() { return options.length == 0; }
    }

    private static final String[] MODE3 = {"ALL", "FRIENDS", "NONE"};
    private static final String[] MODE2 = {"ALL", "NONE"};
    private static final String[] TOGGLE = {"ON", "OFF"};

    public static final List<Def> DEFS = new ArrayList<>();

    static {
        DEFS.add(new Def("tpa", Material.ENDER_PEARL, "&b&lTPA-Anfragen",
                List.of("&7Wer darf dir mit &f/tpa &7eine", "&7Teleport-Anfrage schicken?"), MODE3, "ALL"));
        DEFS.add(new Def("tpahere", Material.ENDER_EYE, "&b&lTPAHERE-Anfragen",
                List.of("&7Wer darf dich mit &f/tpahere &7zu", "&7sich teleportieren lassen?"), MODE3, "ALL"));
        DEFS.add(new Def("tpauto", Material.CLOCK, "&b&lTPA Auto-Accept",
                List.of("&7Erlaubte Anfragen werden sofort", "&7angenommen (wie &f/tpauto&7)."), TOGGLE, "OFF"));
        DEFS.add(new Def("pay", Material.GOLD_INGOT, "&6&lGeld & Blitze erhalten",
                List.of("&7Von wem darfst du Geld (&f/pay&7)", "&7und Blitze (&f/blitz pay&7) bekommen?"), MODE3, "ALL"));
        DEFS.add(new Def("friend_requests", Material.PLAYER_HEAD, "&a&lFreundschaftsanfragen",
                List.of("&7Dürfen dir andere Spieler", "&7Freundschaftsanfragen senden?"), MODE2, "ALL"));
        DEFS.add(new Def("friend_msg", Material.WRITABLE_BOOK, "&a&lFreunde-Nachrichten",
                List.of("&7Private Nachrichten deiner Freunde", "&7(&f/friend msg&7) erhalten."), TOGGLE, "ON"));
        DEFS.add(new Def("friend_notify", Material.BELL, "&a&lFreunde-Benachrichtigungen",
                List.of("&7Meldung, wenn ein Freund den", "&7Server betritt oder verlässt."), TOGGLE, "ON"));
        DEFS.add(new Def("clan_invites", Material.WHITE_BANNER, "&e&lClan-Einladungen",
                List.of("&7Wer darf dich in einen Clan", "&7einladen?"), MODE3, "ALL"));
        DEFS.add(new Def("broadcasts", Material.OAK_SIGN, "&e&lServer-Meldungen",
                List.of("&7Globale Meldungen (News, Rekorde,", "&7Kopfgelder, Clan-Gründungen)."), TOGGLE, "ON"));
        DEFS.add(new Def("keyall_msg", Material.TRIPWIRE_HOOK, "&d&lKeyAll-Nachricht",
                List.of("&7Chat-Nachricht, wenn du beim", "&7KeyAll Keys bekommst."), TOGGLE, "ON"));
        DEFS.add(new Def("afk_actionbar", Material.LIGHTNING_ROD, "&e&lAFK-Actionbar",
                List.of("&7Countdown und Blitz-Meldung in der", "&7Actionbar der AFK-Area."), TOGGLE, "ON"));
        DEFS.add(new Def("combat_actionbar", Material.IRON_SWORD, "&c&lCombat-Actionbar",
                List.of("&7Combat-Timer in der Actionbar", "&7anzeigen."), TOGGLE, "ON"));
        DEFS.add(new Def("daily_remind", Material.SUNFLOWER, "&6&lDaily-Erinnerung",
                List.of("&7Beim Login erinnern, wenn deine", "&7tägliche Belohnung bereit ist."), TOGGLE, "ON"));
        DEFS.add(new Def("sounds", Material.NOTE_BLOCK, "&b&lPlugin-Sounds",
                List.of("&7Klick-, Belohnungs- und", "&7Kauf-Sounds des Plugins."), TOGGLE, "ON"));
        DEFS.add(new Def("language", Material.BOOK, "&6&lSprache",
                List.of("&7Wähle die Sprache für das", "&7gesamte Plugin."), new String[0], ""));
    }

    private static Power pl;

    private Settings() { }

    public static void init(Power plugin) { pl = plugin; }

    public static Def def(String key) {
        for (Def d : DEFS) if (d.key.equals(key)) return d;
        return null;
    }

    public static String get(UUID id, String key) {
        Def d = def(key);
        String fallback = d == null ? "ON" : d.def;
        if (pl == null) return fallback;
        String v = pl.store().getString(id, "set." + key, fallback);
        if (d != null && d.options.length > 0) {
            for (String o : d.options) if (o.equals(v)) return v;
            return fallback;
        }
        return v;
    }

    public static void set(UUID id, String key, String value) { pl.store().setString(id, "set." + key, value); }

    public static boolean isOn(UUID id, String key) { return "ON".equals(get(id, key)); }

    /** Darf {@code from} bei {@code target} etwas ausloesen (ALL / nur Freunde / niemand)? */
    public static boolean allows(UUID target, String key, UUID from) {
        String v = get(target, key);
        if ("ALL".equals(v)) return true;
        if ("FRIENDS".equals(v)) return pl.getFriendManager().areFriends(target, from);
        return false;
    }

    public static String optionName(String opt) {
        return switch (opt) {
            case "ALL" -> "&aAlle Spieler";
            case "FRIENDS" -> "&eNur Freunde";
            case "NONE" -> "&cVollständig deaktiviert";
            case "ON" -> "&aAn";
            case "OFF" -> "&cAus";
            default -> opt;
        };
    }

    public static String next(Def d, String cur, int dir) {
        int i = 0;
        for (int k = 0; k < d.options.length; k++) if (d.options[k].equals(cur)) i = k;
        return d.options[Math.floorMod(i + dir, d.options.length)];
    }
}
