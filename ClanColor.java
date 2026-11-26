package de.power.bat.clan;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Die 16 Minecraft-Farben inkl. deutscher/englischer Namen. Zusätzlich werden Hex-Farben (#RRGGBB) unterstützt. */
public enum ClanColor {
    BLACK('0', "Schwarz", Material.BLACK_WOOL, "schwarz", "black"),
    DARK_BLUE('1', "Dunkelblau", Material.BLUE_WOOL, "dunkelblau", "darkblue"),
    DARK_GREEN('2', "Dunkelgrün", Material.GREEN_WOOL, "dunkelgruen", "darkgreen"),
    DARK_AQUA('3', "Dunkelaqua", Material.CYAN_WOOL, "dunkelaqua", "darkaqua", "cyan"),
    DARK_RED('4', "Dunkelrot", Material.RED_WOOL, "dunkelrot", "darkred"),
    DARK_PURPLE('5', "Lila", Material.PURPLE_WOOL, "lila", "violett", "purple", "darkpurple"),
    GOLD('6', "Gold", Material.ORANGE_WOOL, "gold", "orange"),
    GRAY('7', "Grau", Material.LIGHT_GRAY_WOOL, "grau", "gray", "grey"),
    DARK_GRAY('8', "Dunkelgrau", Material.GRAY_WOOL, "dunkelgrau", "darkgray", "darkgrey"),
    BLUE('9', "Blau", Material.LIGHT_BLUE_WOOL, "blau", "blue"),
    GREEN('a', "Grün", Material.LIME_WOOL, "gruen", "hellgruen", "green", "lime"),
    AQUA('b', "Aqua", Material.LIGHT_BLUE_CONCRETE, "aqua", "tuerkis", "hellblau"),
    RED('c', "Rot", Material.RED_CONCRETE, "rot", "red"),
    LIGHT_PURPLE('d', "Pink", Material.PINK_WOOL, "pink", "rosa", "magenta", "lightpurple", "hellila"),
    YELLOW('e', "Gelb", Material.YELLOW_WOOL, "gelb", "yellow"),
    WHITE('f', "Weiß", Material.WHITE_WOOL, "weiss", "white");

    private static final Pattern HEX = Pattern.compile("^&?#([0-9a-fA-F]{6})$");

    private final char code;
    private final String display;
    private final Material material;
    private final String[] aliases;

    ClanColor(char code, String display, Material material, String... aliases) {
        this.code = code;
        this.display = display;
        this.material = material;
        this.aliases = aliases;
    }

    /** Farbcode im Format "&6". */
    public String getCode() {
        return "&" + code;
    }

    public String getDisplay() {
        return display;
    }

    public Material getMaterial() {
        return material;
    }

    private static String normalize(String input) {
        String s = input.toLowerCase(Locale.ROOT).trim();
        s = s.replace("ü", "ue").replace("ä", "ae").replace("ö", "oe").replace("ß", "ss");
        return s.replaceAll("[^a-z0-9]", "");
    }

    /** Liefert den Farbcode ("&6" oder "&#RRGGBB") oder null bei ungültiger Eingabe. */
    public static String parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        java.util.regex.Matcher hex = HEX.matcher(trimmed);
        if (hex.matches()) {
            return "&#" + hex.group(1).toLowerCase(Locale.ROOT);
        }
        String code = trimmed.toLowerCase(Locale.ROOT);
        if (code.startsWith("&") || code.startsWith("§")) {
            code = code.substring(1);
        }
        if (code.length() == 1) {
            for (ClanColor color : values()) {
                if (color.code == code.charAt(0)) {
                    return color.getCode();
                }
            }
        }
        String normalized = normalize(trimmed);
        for (ClanColor color : values()) {
            if (normalize(color.display).equals(normalized)) {
                return color.getCode();
            }
            for (String alias : color.aliases) {
                if (alias.equals(normalized)) {
                    return color.getCode();
                }
            }
        }
        return null;
    }

    public static ClanColor fromCode(String color) {
        if (color == null || color.length() != 2) {
            return null;
        }
        for (ClanColor c : values()) {
            if (c.getCode().equalsIgnoreCase(color)) {
                return c;
            }
        }
        return null;
    }

    /** Lesbarer Name einer gespeicherten Farbe. */
    public static String nameOf(String color) {
        ClanColor c = fromCode(color);
        return c != null ? c.display : color;
    }

    /** Namen für die Tab-Vervollständigung. */
    public static List<String> tabNames() {
        List<String> names = new ArrayList<>();
        for (ClanColor c : values()) {
            names.add(normalize(c.display));
        }
        return names;
    }
}
