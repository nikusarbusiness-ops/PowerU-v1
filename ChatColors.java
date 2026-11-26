package de.power;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rangabhaengige Chatfarben (LuckPerms-kompatibel ueber Gruppen und Permissions). */
public final class ChatColors {

    private static final String COLORS = "0123456789abcdef";
    private static final String FORMATS = "lmnor";

    private ChatColors() { }

    public static Set<Character> allowed(Power pl, Player p) {
        Set<Character> set = new HashSet<>();
        boolean hex = false;
        if (p.hasPermission("powerbat.chat.color") || p.hasPermission("power.chat.color.*")) {
            addAll(set, COLORS + FORMATS + "k");
            return withHex(set);
        }
        if (p.hasPermission("power.chat.colors")) addAll(set, COLORS);
        if (p.hasPermission("power.chat.format")) addAll(set, FORMATS);
        if (p.hasPermission("power.chat.magic")) set.add('k');
        if (p.hasPermission("power.chat.hex")) hex = true;
        for (char c : (COLORS + FORMATS + "k").toCharArray()) {
            if (p.hasPermission("power.chat.color." + c)) set.add(c);
        }
        if (pl.getConfig().getBoolean("chat-colors.enabled", true)) {
            hex |= applyTokens(set, pl.getConfig().getStringList("chat-colors.default"));
            ConfigurationSection ranks = pl.getConfig().getConfigurationSection("chat-colors.ranks");
            if (ranks != null) {
                for (String group : ranks.getKeys(false)) {
                    if (p.hasPermission("group." + group)) hex |= applyTokens(set, ranks.getStringList(group));
                }
            }
        }
        return hex ? withHex(set) : set;
    }

    private static Set<Character> withHex(Set<Character> set) {
        set.add('#');
        return set;
    }

    private static void addAll(Set<Character> set, String chars) {
        for (char c : chars.toCharArray()) set.add(c);
    }

    /** @return true, wenn Hex erlaubt wird */
    private static boolean applyTokens(Set<Character> set, List<String> tokens) {
        boolean hex = false;
        for (String t : tokens) {
            String s = t.trim().toLowerCase();
            switch (s) {
                case "colors" -> addAll(set, COLORS);
                case "formats" -> addAll(set, FORMATS);
                case "magic" -> set.add('k');
                case "hex" -> hex = true;
                case "all" -> { addAll(set, COLORS + FORMATS + "k"); hex = true; }
                default -> { if (s.length() == 1) set.add(s.charAt(0)); }
            }
        }
        return hex;
    }

    /** Wandelt nur die erlaubten &-Codes um; nicht erlaubte bleiben als Text stehen. */
    public static String apply(Power pl, Player p, String msg) {
        if (msg == null || msg.indexOf('&') < 0) return msg == null ? null : msg.replace("§", "");
        Set<Character> ok = allowed(pl, p);
        String m = msg.replace("§", "");
        if (ok.isEmpty()) return m;
        StringBuilder out = new StringBuilder(m.length());
        int n = m.length();
        for (int i = 0; i < n; i++) {
            char ch = m.charAt(i);
            if (ch == '&' && i + 1 < n) {
                char c = Character.toLowerCase(m.charAt(i + 1));
                if (c == '#' && ok.contains('#') && i + 7 < n + 0 && isHex(m, i + 2)) {
                    out.append("§x");
                    for (int k = 0; k < 6; k++) out.append('§').append(m.charAt(i + 2 + k));
                    i += 7;
                    continue;
                }
                if ((COLORS.indexOf(c) >= 0 || FORMATS.indexOf(c) >= 0 || c == 'k') && ok.contains(c)) {
                    out.append('§').append(c);
                    i++;
                    continue;
                }
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static boolean isHex(String s, int start) {
        if (start + 6 > s.length()) return false;
        for (int i = start; i < start + 6; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) return false;
        }
        return true;
    }
}
