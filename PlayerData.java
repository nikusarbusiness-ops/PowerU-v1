package de.power.bat.storage;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int clanKills;
    private long lastSeen;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getClanKills() {
        return clanKills;
    }

    public void setClanKills(int clanKills) {
        this.clanKills = clanKills;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public double getKd() {
        return deaths == 0 ? kills : kills / (double) deaths;
    }
}
