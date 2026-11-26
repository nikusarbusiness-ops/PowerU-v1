package de.power.bat.clan;

import de.power.Power;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Verwaltet offene Clan-Einladungen (zeitlich begrenzt, nur im Speicher). */
public class InviteManager {

    public record Invite(UUID clanId, UUID inviter, UUID target, long expires) {
        public int secondsLeft() {
            return (int) Math.max(0, (expires - System.currentTimeMillis()) / 1000L);
        }
    }

    private final Power plugin;
    private final Map<UUID, List<Invite>> invites = new ConcurrentHashMap<>();

    public InviteManager(Power plugin) {
        this.plugin = plugin;
    }

    public long expireMillis() {
        return Math.max(5, plugin.getConfig().getInt("clan.invite-expire-seconds", 60)) * 1000L;
    }

    private void purge(UUID target) {
        List<Invite> list = invites.get(target);
        if (list == null) {
            return;
        }
        long now = System.currentTimeMillis();
        list.removeIf(invite -> invite.expires() <= now);
        if (list.isEmpty()) {
            invites.remove(target);
        }
    }

    public boolean has(UUID clanId, UUID target) {
        return find(target, clanId) != null;
    }

    /** @return false, wenn bereits eine Einladung dieses Clans besteht. */
    public boolean add(UUID clanId, UUID inviter, UUID target) {
        if (has(clanId, target)) {
            return false;
        }
        invites.computeIfAbsent(target, key -> new CopyOnWriteArrayList<>())
                .add(new Invite(clanId, inviter, target, System.currentTimeMillis() + expireMillis()));
        return true;
    }

    /** Alle offenen Einladungen eines Spielers (älteste zuerst). */
    public List<Invite> getFor(UUID target) {
        purge(target);
        List<Invite> list = invites.get(target);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public Invite find(UUID target, UUID clanId) {
        for (Invite invite : getFor(target)) {
            if (invite.clanId().equals(clanId)) {
                return invite;
            }
        }
        return null;
    }

    public Invite getLatest(UUID target) {
        List<Invite> list = getFor(target);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    public void remove(UUID target, UUID clanId) {
        List<Invite> list = invites.get(target);
        if (list != null) {
            list.removeIf(invite -> invite.clanId().equals(clanId));
            if (list.isEmpty()) {
                invites.remove(target);
            }
        }
    }

    public void clearTarget(UUID target) {
        invites.remove(target);
    }

    public void clearClan(UUID clanId) {
        for (UUID target : new ArrayList<>(invites.keySet())) {
            remove(target, clanId);
        }
    }

    /** Alle offenen Einladungen eines Clans. */
    public List<Invite> getForClan(UUID clanId) {
        List<Invite> result = new ArrayList<>();
        for (UUID target : new ArrayList<>(invites.keySet())) {
            for (Invite invite : getFor(target)) {
                if (invite.clanId().equals(clanId)) {
                    result.add(invite);
                }
            }
        }
        return result;
    }
}
