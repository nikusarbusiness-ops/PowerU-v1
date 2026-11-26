package de.power.bat.friends;

import de.power.Power;
import de.power.bat.utils.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Freundesliste: Anfragen, Freunde, private Nachrichten und Online/Offline-Benachrichtigungen. */
public class FriendManager {

    private final Power plugin;
    private final File file;
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> requests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastMessage = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public FriendManager(Power plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "friends.yml");
    }

    private Messages m() {
        return plugin.getMessages();
    }

    // ------------------------------------------------------------------ Speicher

    public void load() {
        friends.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("friends");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String s : root.getStringList(key)) {
                    try {
                        set.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {
                        // ungültigen Eintrag überspringen
                    }
                }
                if (!set.isEmpty()) {
                    friends.put(owner, set);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ungültiger Eintrag in friends.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.createSection("friends");
        for (Map.Entry<UUID, Set<UUID>> entry : friends.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            List<String> list = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                list.add(id.toString());
            }
            config.set("friends." + entry.getKey(), list);
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File temp = new File(file.getPath() + ".tmp");
            config.save(temp);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("friends.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    // ------------------------------------------------------------------ Abfragen

    public boolean areFriends(UUID a, UUID b) {
        Set<UUID> set = friends.get(a);
        return set != null && set.contains(b);
    }

    public List<UUID> getFriends(UUID id) {
        Set<UUID> set = friends.get(id);
        return set == null ? new ArrayList<>() : new ArrayList<>(set);
    }

    public UUID findFriend(UUID owner, String name) {
        for (UUID id : getFriends(owner)) {
            if (plugin.getPlayerData().getName(id).equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    private final Map<UUID, Integer> limitCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** Freunde-Limit eines Spielers nach Rang (LuckPerms-Gruppe, Permission power.friends.<zahl> oder Meta). */
    public int maxFriends(UUID id) {
        int base = Math.max(1, plugin.getConfig().getInt("friends.max-friends", 10));
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            int v = Math.max(base, plugin.rankInt(p, "friends.ranks", base, "power.friends.", "power.friends"));
            limitCache.put(id, v);
            return v;
        }
        return Math.max(base, limitCache.getOrDefault(id, base));
    }

    private long requestMillis() {
        return Math.max(10, plugin.getConfig().getInt("friends.request-expire-seconds", 300)) * 1000L;
    }

    private void purge(UUID target) {
        Map<UUID, Long> map = requests.get(target);
        if (map == null) {
            return;
        }
        long now = System.currentTimeMillis();
        map.values().removeIf(expires -> expires <= now);
        if (map.isEmpty()) {
            requests.remove(target);
        }
    }

    /** Offene Anfragen an einen Spieler (Absender-UUIDs). */
    public List<UUID> getRequests(UUID target) {
        purge(target);
        Map<UUID, Long> map = requests.get(target);
        return map == null ? new ArrayList<>() : new ArrayList<>(map.keySet());
    }

    private UUID findRequest(UUID target, String name) {
        List<UUID> list = getRequests(target);
        if (name == null) {
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }
        for (UUID id : list) {
            if (plugin.getPlayerData().getName(id).equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Aktionen

    public boolean sendRequest(Player from, Player to) {
        UUID a = from.getUniqueId();
        UUID b = to.getUniqueId();
        if (a.equals(b)) {
            m().send(from, "friend.error.self");
            return false;
        }
        if (areFriends(a, b)) {
            m().send(from, "friend.error.already-friends", "%player%", to.getName());
            return false;
        }
        if (!de.power.Settings.allows(b, "friend_requests", a)) {
            m().send(from, "friend.error.requests-disabled", "%player%", to.getName());
            return false;
        }
        if (getFriends(a).size() >= maxFriends(a)) {
            m().send(from, "friend.error.limit-self", "%max%", String.valueOf(maxFriends(a)));
            return false;
        }
        if (getFriends(b).size() >= maxFriends(b)) {
            m().send(from, "friend.error.limit-other", "%player%", to.getName());
            return false;
        }
        // Hat der andere uns schon eine Anfrage geschickt? Dann direkt Freunde werden.
        if (getRequests(a).contains(b)) {
            return accept(from, to.getName());
        }
        purge(b);
        Map<UUID, Long> map = requests.computeIfAbsent(b, key -> new ConcurrentHashMap<>());
        if (map.containsKey(a)) {
            m().send(from, "friend.error.already-requested", "%player%", to.getName());
            return false;
        }
        map.put(a, System.currentTimeMillis() + requestMillis());
        m().send(from, "friend.request-sent", "%player%", to.getName());
        m().send(to, "friend.request-received", "%player%", from.getName(),
                "%seconds%", String.valueOf(requestMillis() / 1000L));

        TextComponent accept = new TextComponent(TextComponent.fromLegacyText(m().getFor(to, "friend.accept-button")));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + from.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(m().getFor(to, "friend.accept-hover"))));
        TextComponent deny = new TextComponent(TextComponent.fromLegacyText(m().getFor(to, "friend.deny-button")));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + from.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(m().getFor(to, "friend.deny-hover"))));
        TextComponent line = new TextComponent("");
        line.addExtra(accept);
        line.addExtra(new TextComponent("  "));
        line.addExtra(deny);
        to.spigot().sendMessage(line);
        return true;
    }

    public boolean accept(Player player, String name) {
        UUID senderId = findRequest(player.getUniqueId(), name);
        if (senderId == null) {
            m().send(player, "friend.error.no-request");
            return false;
        }
        if (getFriends(player.getUniqueId()).size() >= maxFriends(player.getUniqueId())) {
            m().send(player, "friend.error.limit-self", "%max%", String.valueOf(maxFriends(player.getUniqueId())));
            return false;
        }
        if (getFriends(senderId).size() >= maxFriends(senderId)) {
            m().send(player, "friend.error.limit-other", "%player%", plugin.getPlayerData().getName(senderId));
            return false;
        }
        Map<UUID, Long> map = requests.get(player.getUniqueId());
        if (map != null) {
            map.remove(senderId);
        }
        Map<UUID, Long> reverse = requests.get(senderId);
        if (reverse != null) {
            reverse.remove(player.getUniqueId());
        }
        friends.computeIfAbsent(player.getUniqueId(), key -> ConcurrentHashMap.newKeySet()).add(senderId);
        friends.computeIfAbsent(senderId, key -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        dirty = true;
        save();
        String senderName = plugin.getPlayerData().getName(senderId);
        m().send(player, "friend.added", "%player%", senderName);
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) {
            m().send(sender, "friend.added", "%player%", player.getName());
        }
        return true;
    }

    public boolean deny(Player player, String name) {
        UUID senderId = findRequest(player.getUniqueId(), name);
        if (senderId == null) {
            m().send(player, "friend.error.no-request");
            return false;
        }
        Map<UUID, Long> map = requests.get(player.getUniqueId());
        if (map != null) {
            map.remove(senderId);
        }
        m().send(player, "friend.denied", "%player%", plugin.getPlayerData().getName(senderId));
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) {
            m().send(sender, "friend.denied-notify", "%player%", player.getName());
        }
        return true;
    }

    public boolean remove(Player player, String name) {
        UUID target = findFriend(player.getUniqueId(), name);
        if (target == null) {
            m().send(player, "friend.error.not-friend", "%player%", name);
            return false;
        }
        Set<UUID> mine = friends.get(player.getUniqueId());
        if (mine != null) {
            mine.remove(target);
        }
        Set<UUID> theirs = friends.get(target);
        if (theirs != null) {
            theirs.remove(player.getUniqueId());
        }
        dirty = true;
        save();
        m().send(player, "friend.removed", "%player%", plugin.getPlayerData().getName(target));
        Player online = Bukkit.getPlayer(target);
        if (online != null) {
            m().send(online, "friend.removed-notify", "%player%", player.getName());
        }
        return true;
    }

    public void sendList(Player player) {
        List<UUID> list = getFriends(player.getUniqueId());
        if (list.isEmpty()) {
            m().send(player, "friend.list-empty");
            return;
        }
        list.sort(Comparator
                .comparing((UUID id) -> Bukkit.getPlayer(id) == null)
                .thenComparing(id -> plugin.getPlayerData().getName(id).toLowerCase(Locale.ROOT)));
        int online = 0;
        for (UUID id : list) {
            if (Bukkit.getPlayer(id) != null) {
                online++;
            }
        }
        m().send(player, "friend.list-header", "%online%", String.valueOf(online), "%total%", String.valueOf(list.size()));
        for (UUID id : list) {
            String name = plugin.getPlayerData().getName(id);
            if (Bukkit.getPlayer(id) != null) {
                m().send(player, "friend.list-online", "%player%", name);
            } else {
                long seen = plugin.getPlayerData().get(id).getLastSeen();
                m().send(player, "friend.list-offline", "%player%", name,
                        "%lastseen%", seen <= 0 ? m().getFor(player, "friend.never") : plugin.formatAgo(player, seen));
            }
        }
    }

    public void sendRequests(Player player) {
        List<UUID> list = getRequests(player.getUniqueId());
        if (list.isEmpty()) {
            m().send(player, "friend.requests-empty");
            return;
        }
        m().send(player, "friend.requests-header", "%count%", String.valueOf(list.size()));
        for (UUID id : list) {
            m().send(player, "friend.requests-line", "%player%", plugin.getPlayerData().getName(id));
        }
    }

    public boolean message(Player from, String targetName, String text) {
        UUID id = findFriend(from.getUniqueId(), targetName);
        if (id == null) {
            m().send(from, "friend.error.not-friend", "%player%", targetName);
            return false;
        }
        Player target = Bukkit.getPlayer(id);
        if (target == null) {
            m().send(from, "friend.error.offline", "%player%", plugin.getPlayerData().getName(id));
            return false;
        }
        if (!de.power.Settings.isOn(id, "friend_msg")) {
            m().send(from, "friend.error.msg-disabled", "%player%", target.getName());
            return false;
        }
        String message = de.power.ChatColors.apply(plugin, from, text);
        from.sendMessage(m().getFor(from, "friend.msg-to", "%player%", target.getName()).replace("%message%", message));
        target.sendMessage(m().getFor(target, "friend.msg-from", "%player%", from.getName()).replace("%message%", message));
        lastMessage.put(from.getUniqueId(), target.getUniqueId());
        lastMessage.put(target.getUniqueId(), from.getUniqueId());
        return true;
    }

    public boolean reply(Player from, String text) {
        UUID id = lastMessage.get(from.getUniqueId());
        if (id == null) {
            m().send(from, "friend.error.nobody-to-reply");
            return false;
        }
        return message(from, plugin.getPlayerData().getName(id), text);
    }

    // ------------------------------------------------------------------ Benachrichtigungen

    public void notifyJoin(Player player) {
        if (!plugin.getConfig().getBoolean("friends.notifications", true)) {
            return;
        }
        int online = 0;
        List<UUID> list = getFriends(player.getUniqueId());
        for (UUID id : list) {
            Player friend = Bukkit.getPlayer(id);
            if (friend != null) {
                online++;
                if (de.power.Settings.isOn(id, "friend_notify")) {
                    m().send(friend, "friend.notify-online", "%player%", player.getName());
                }
            }
        }
        if (!list.isEmpty() && de.power.Settings.isOn(player.getUniqueId(), "friend_notify")) {
            m().send(player, "friend.join-summary", "%online%", String.valueOf(online), "%total%", String.valueOf(list.size()));
        }
        int pending = getRequests(player.getUniqueId()).size();
        if (pending > 0) {
            m().send(player, "friend.pending-requests", "%count%", String.valueOf(pending));
        }
    }

    public void notifyQuit(Player player) {
        lastMessage.remove(player.getUniqueId());
        if (!plugin.getConfig().getBoolean("friends.notifications", true)) {
            return;
        }
        for (UUID id : getFriends(player.getUniqueId())) {
            Player friend = Bukkit.getPlayer(id);
            if (friend != null && de.power.Settings.isOn(id, "friend_notify")) {
                m().send(friend, "friend.notify-offline", "%player%", player.getName());
            }
        }
    }
}
