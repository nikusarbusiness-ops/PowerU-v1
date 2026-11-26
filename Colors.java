package de.power.bat.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Colors {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Colors() {
    }

    /** Wandelt &-Codes (inkl. &#RRGGBB) in Minecraft-Farbcodes um. */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Entfernt alle Farbcodes. */
    public static String strip(String input) {
        return ChatColor.stripColor(color(input));
    }

    /** Maskiert % für String.format-basierte Formate (Chat). */
    public static String escapeFormat(String input) {
        return input.replace("%", "%%");
    }
}
