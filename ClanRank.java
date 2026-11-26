package de.power.bat.clan;

import java.util.Locale;

public enum ClanRank {
    LEADER(4, "leader"),
    CO_LEADER(3, "co-leader"),
    MODERATOR(2, "moderator"),
    MEMBER(1, "member");

    private final int level;
    private final String key;

    ClanRank(int level, String key) {
        this.level = level;
        this.key = key;
    }

    public int getLevel() {
        return level;
    }

    /** Schlüssel für messages.yml (ranks.<key>). */
    public String getKey() {
        return key;
    }

    public boolean isHigherThan(ClanRank other) {
        return level > other.level;
    }

    /** Nächsthöherer Rang per Beförderung (nie LEADER), sonst null. */
    public ClanRank promoted() {
        return switch (this) {
            case MEMBER -> MODERATOR;
            case MODERATOR -> CO_LEADER;
            default -> null;
        };
    }

    /** Nächstniedrigerer Rang per Degradierung, sonst null. */
    public ClanRank demoted() {
        return switch (this) {
            case CO_LEADER -> MODERATOR;
            case MODERATOR -> MEMBER;
            default -> null;
        };
    }

    public static ClanRank fromString(String value) {
        if (value == null) {
            return MEMBER;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MEMBER;
        }
    }
}
