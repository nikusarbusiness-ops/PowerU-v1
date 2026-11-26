package de.power.bat.combat;

import de.power.Power;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final Power plugin;
    private final Map<UUID, Long> endTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> opponents = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastOpponent = new ConcurrentHashMap<>();
    private final Set<UUID> loggingOut = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public CombatManager(Power plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    /** Beim Herunterfahren: Combat-Status wird verworfen, niemand wird bestraft. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID id : new ArrayList<>(endTimes.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                sendActionBar(player, "");
            }
        }
        endTimes.clear();
        opponents.clear();
        lastOpponent.clear();
        loggingOut.clear();
    }

    public int duration() {
        return Math.max(1, plugin.getConfig().getInt("combat.duration-seconds", 20));
    }

    public boolean isInCombat(UUID id) {
        Long end = endTimes.get(id);
        return end != null && end > System.currentTimeMillis();
    }

    public int getRemainingSeconds(UUID id) {
        Long end = endTimes.get(id);
        if (end == null) {
            return 0;
        }
        long left = end - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    public int getCombatCount() {
        return endTimes.size();
    }

    public boolean isBypassed(Player player) {
        return player.hasPermission("powerbat.combat.bypass");
    }

    public Set<UUID> getOpponents(UUID id) {
        Set<UUID> set = opponents.get(id);
        return set == null ? Collections.emptySet() : new HashSet<>(set);
    }

    public UUID getLastOpponent(UUID id) {
        return lastOpponent.get(id);
    }

    public boolean isLoggingOut(UUID id) {
        return loggingOut.contains(id);
    }

    // ------------------------------------------------------------------ Tagging

    /** Beide Spieler in Combat versetzen (Timer wird bei jedem Treffer zurückgesetzt). */
    public void tag(Player attacker, Player victim) {
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        tagSingle(attacker, victim);
        tagSingle(victim, attacker);
    }

    private void tagSingle(Player player, Player other) {
        if (isBypassed(player)) {
            return;
        }
        if (plugin.getConfig().getBoolean("combat.ignore-creative", true)
                && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
            return;
        }
        UUID id = player.getUniqueId();
        boolean wasTagged = isInCombat(id);
        endTimes.put(id, System.currentTimeMillis() + duration() * 1000L);
        if (other != null) {
            opponents.computeIfAbsent(id, key -> ConcurrentHashMap.newKeySet()).add(other.getUniqueId());
            lastOpponent.put(id, other.getUniqueId());
        }
        if (!wasTagged) {
            plugin.getMessages().send(player, "combat.start");
        }
    }

    /** Erzwingt Combat für einen Spieler (Admin-Befehl), ohne Gegner. */
    public void forceTag(Player player) {
        UUID id = player.getUniqueId();
        boolean wasTagged = isInCombat(id);
        endTimes.put(id, System.currentTimeMillis() + duration() * 1000L);
        if (!wasTagged) {
            plugin.getMessages().send(player, "combat.start");
        }
    }

    /** @return true, wenn der Spieler vorher in Combat war. */
    public boolean untag(UUID id, boolean notify) {
        boolean was = endTimes.remove(id) != null;
        Set<UUID> opps = opponents.remove(id);
        lastOpponent.remove(id);
        if (opps != null) {
            for (UUID other : opps) {
                Set<UUID> set = opponents.get(other);
                if (set != null) {
                    set.remove(id);
                }
            }
        }
        Player player = Bukkit.getPlayer(id);
        if (player != null && was) {
            sendActionBar(player, "");
            if (notify) {
                plugin.getMessages().send(player, "combat.end");
            }
        }
        return was;
    }

    // ------------------------------------------------------------------ Logout / Tod

    /** Bestraft einen Spieler, der im Combat den Server verlässt. Wird aus PlayerQuitEvent aufgerufen. */
    public void handleLogout(Player player) {
        UUID id = player.getUniqueId();
        loggingOut.add(id);
        try {
            for (UUID opponentId : getOpponents(id)) {
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    plugin.getMessages().send(opponent, "combat.logout-notify");
                }
            }
            if (plugin.getConfig().getBoolean("combat.kill-on-logout", true) && !player.isDead()) {
                player.setHealth(0.0D);
            }
        } finally {
            loggingOut.remove(id);
            untag(id, false);
        }
    }

    /** Wird nach einem Spielertod aufgerufen: Combat-Status des Opfers (und ggf. des Killers) entfernen. */
    public void onDeath(Player victim, Player killer) {
        untag(victim.getUniqueId(), false);
        if (killer != null && plugin.getConfig().getBoolean("combat.untag-killer-on-kill", true)) {
            Set<UUID> remaining = opponents.get(killer.getUniqueId());
            if (remaining == null || remaining.isEmpty()) {
                untag(killer.getUniqueId(), true);
            }
        }
    }

    // ------------------------------------------------------------------ Timer

    private void tick() {
        long now = System.currentTimeMillis();
        boolean actionbar = plugin.getConfig().getBoolean("combat.show-actionbar", true);
        for (Map.Entry<UUID, Long> entry : endTimes.entrySet()) {
            UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                untag(id, false);
                continue;
            }
            if (entry.getValue() <= now) {
                untag(id, true);
                continue;
            }
            if (actionbar && de.power.Settings.isOn(id, "combat_actionbar")) {
                sendActionBar(player, plugin.getMessages().getFor(player, "combat.actionbar",
                        "%seconds%", String.valueOf(getRemainingSeconds(id))));
            }
        }
    }

    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
