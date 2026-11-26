package de.power.bat.clan;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Clan {

    private final UUID id;
    private final long created;
    private final Map<UUID, ClanRank> members = new ConcurrentHashMap<>();
    private volatile String name;
    private volatile String tag;
    private volatile String color = "&6";
    private volatile String description = "";
    private volatile UUID leader;
    private volatile int kills;
    private volatile int deaths;
    private volatile boolean pvp;
    private volatile int memberLimit;

    public Clan(UUID id, String name, long created) {
        this.id = id;
        this.name = name;
        this.tag = name;
        this.created = created;
    }

    public UUID getId() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public double getKd() {
        return deaths == 0 ? kills : kills / (double) deaths;
    }

    /** Zuletzt berechnetes Mitglieder-Limit (Rang des Leaders); 0 = unbekannt. */
    public int getMemberLimit() {
        return memberLimit;
    }

    public void setMemberLimit(int memberLimit) {
        this.memberLimit = memberLimit;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public Map<UUID, ClanRank> getMembers() {
        return members;
    }

    public ClanRank getRank(UUID player) {
        return members.get(player);
    }

    public boolean isMember(UUID player) {
        return members.containsKey(player);
    }

    public int getMemberCount() {
        return members.size();
    }

    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID member : members.keySet()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    public void broadcast(String message) {
        for (Player player : getOnlineMembers()) {
            player.sendMessage(message);
        }
    }
}
