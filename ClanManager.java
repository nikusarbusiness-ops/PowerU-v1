package de.power.bat.clan;

import de.power.Power;
import de.power.bat.storage.PlayerData;
import de.power.bat.utils.Colors;
import de.power.bat.utils.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Zentrale Clan-Logik. Alle Aktionen (Befehle und GUI) laufen hier durch, prüfen Rechte
 * und senden die passenden Nachrichten selbst.
 */
public class ClanManager {

    private final Power plugin;
    private final Map<UUID, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerIndex = new ConcurrentHashMap<>();
    private final Map<ClanRank, Set<ClanPermission>> rankPermissions = new EnumMap<>(ClanRank.class);
    private final Set<UUID> chatMode = ConcurrentHashMap.newKeySet();
    private volatile boolean dirty;

    public ClanManager(Power plugin) {
        this.plugin = plugin;
        reloadPermissions();
    }

    private Messages m() {
        return plugin.getMessages();
    }

    // ------------------------------------------------------------------ Laden/Speichern

    public void load() {
        clans.clear();
        nameIndex.clear();
        playerIndex.clear();
        for (Clan clan : plugin.getClanStorage().load()) {
            if (nameIndex.containsKey(clan.getName().toLowerCase(Locale.ROOT))) {
                plugin.getLogger().warning("Doppelter Clan-Name in clans.yml übersprungen: " + clan.getName());
                continue;
            }
            index(clan);
        }
        plugin.getLogger().info(clans.size() + " Clan(s) geladen.");
    }

    private void index(Clan clan) {
        clans.put(clan.getId(), clan);
        nameIndex.put(clan.getName().toLowerCase(Locale.ROOT), clan.getId());
        for (UUID member : clan.getMembers().keySet()) {
            playerIndex.put(member, clan.getId());
        }
    }

    private void unindex(Clan clan) {
        clans.remove(clan.getId());
        nameIndex.remove(clan.getName().toLowerCase(Locale.ROOT));
        for (UUID member : clan.getMembers().keySet()) {
            playerIndex.remove(member, clan.getId());
            chatMode.remove(member);
        }
        plugin.getInviteManager().clearClan(clan.getId());
    }

    public void save() {
        plugin.getClanStorage().save(clans.values());
        dirty = false;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void reloadPermissions() {
        rankPermissions.clear();
        for (ClanRank rank : ClanRank.values()) {
            Set<ClanPermission> set = EnumSet.noneOf(ClanPermission.class);
            for (String entry : plugin.getConfig().getStringList("clan.rank-permissions." + rank.name())) {
                if (entry.equalsIgnoreCase("ALL")) {
                    set.addAll(Arrays.asList(ClanPermission.values()));
                    continue;
                }
                try {
                    set.add(ClanPermission.valueOf(entry.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Unbekannte Clan-Permission in config.yml: " + entry);
                }
            }
            rankPermissions.put(rank, set);
        }
    }

    // ------------------------------------------------------------------ Abfragen

    public Clan getClan(UUID player) {
        UUID id = playerIndex.get(player);
        return id == null ? null : clans.get(id);
    }

    public Clan getClanById(UUID id) {
        return clans.get(id);
    }

    public Clan getClanByName(String name) {
        if (name == null) {
            return null;
        }
        UUID id = nameIndex.get(name.toLowerCase(Locale.ROOT));
        return id == null ? null : clans.get(id);
    }

    public Collection<Clan> getClans() {
        return Collections.unmodifiableCollection(clans.values());
    }

    public boolean hasPermission(Clan clan, UUID player, ClanPermission permission) {
        ClanRank rank = clan.getRank(player);
        if (rank == null) {
            return false;
        }
        if (rank == ClanRank.LEADER) {
            return true;
        }
        if (permission == ClanPermission.DISBAND || permission == ClanPermission.TRANSFER) {
            return false;
        }
        return rankPermissions.getOrDefault(rank, Collections.emptySet()).contains(permission);
    }

    public UUID findMember(Clan clan, String name) {
        for (UUID id : clan.getMembers().keySet()) {
            if (plugin.getPlayerData().getName(id).equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    public List<UUID> getSortedMembers(Clan clan) {
        List<UUID> list = new ArrayList<>(clan.getMembers().keySet());
        list.sort(Comparator
                .comparingInt((UUID id) -> -clan.getRank(id).getLevel())
                .thenComparing(id -> plugin.getPlayerData().getName(id).toLowerCase(Locale.ROOT)));
        return list;
    }

    public boolean isChatMode(UUID player) {
        return chatMode.contains(player);
    }

    public void removeChatMode(UUID player) {
        chatMode.remove(player);
    }

    public boolean canCreate(Player player) {
        if (!plugin.getConfig().getBoolean("clan.create-requires-permission", true)) {
            return true;
        }
        if (player.hasPermission("powerbat.admin")) {
            return true;
        }
        for (String permission : plugin.getConfig().getStringList("clan.create-permissions")) {
            if (!permission.isBlank() && player.hasPermission(permission)) {
                return true;
            }
        }
        for (String group : plugin.getConfig().getStringList("clan.create-groups")) {
            if (group.isBlank()) {
                continue;
            }
            // LuckPerms setzt für jede (auch vererbte) Gruppe die Permission group.<name>
            if (player.hasPermission("group." + group.toLowerCase(Locale.ROOT))) {
                return true;
            }
            if (plugin.getLuckPerms() != null) {
                String primary = plugin.getLuckPerms().getPrimaryGroup(player);
                if (primary != null && primary.equalsIgnoreCase(group)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Darstellung

    public String displayTag(Clan clan) {
        String tag = clan.getTag();
        if (tag.contains("&")) {
            return Colors.color(tag);
        }
        String format = plugin.getConfig().getString("clan.tag-format", "&8[%color%&l%tag%&8]");
        return Colors.color(format.replace("%color%", clan.getColor()).replace("%tag%", tag));
    }

    public String displayName(Clan clan) {
        String format = plugin.getConfig().getString("clan.name-format", "%color%&l%name%");
        return Colors.color(format.replace("%color%", clan.getColor()).replace("%name%", clan.getName()));
    }

    public String rankDisplay(ClanRank rank) {
        return m().get("ranks." + rank.getKey());
    }

    public void refreshDisplay(Clan clan) {
        for (Player player : clan.getOnlineMembers()) {
            plugin.getDisplay().update(player);
        }
    }

    public void sendInfo(CommandSender to, Clan clan) {
        int online = clan.getOnlineMembers().size();
        for (String line : m().listFor(to, "info.lines",
                "%clan%", displayName(clan),
                "%plainname%", clan.getName(),
                "%tag%", displayTag(clan),
                "%leader%", plugin.getPlayerData().getName(clan.getLeader()),
                "%members%", String.valueOf(clan.getMemberCount()),
                "%max%", String.valueOf(maxMembers(clan)),
                "%created%", plugin.formatDate(clan.getCreated()),
                "%color%", clan.getColor() + ClanColor.nameOf(clan.getColor()),
                "%online%", String.valueOf(online),
                "%pvp%", m().getFor(to, clan.isPvp() ? "info.pvp-on" : "info.pvp-off"),
                "%description%", clan.getDescription().isEmpty() ? m().getFor(to, "info.no-description") : clan.getDescription(),
                "%kills%", String.valueOf(clan.getKills()),
                "%deaths%", String.valueOf(clan.getDeaths()),
                "%kd%", String.format(Locale.ROOT, "%.2f", clan.getKd()))) {
            to.sendMessage(line);
        }
    }

    public void sendMembers(CommandSender to, Clan clan) {
        m().send(to, "members.header", "%clan%", displayName(clan), "%count%", String.valueOf(clan.getMemberCount()));
        for (UUID id : getSortedMembers(clan)) {
            boolean online = Bukkit.getPlayer(id) != null;
            m().send(to, "members.line",
                    "%rank%", rankDisplay(clan.getRank(id)),
                    "%player%", plugin.getPlayerData().getName(id),
                    "%status%", m().get(online ? "members.online" : "members.offline"));
        }
    }

    /**
     * Mitglieder-Limit eines Clans: richtet sich nach dem Rang (LuckPerms-Gruppe/Permission/Meta) des Leaders.
     * Ist der Leader offline, gilt der zuletzt berechnete Wert.
     */
    public int maxMembers(Clan clan) {
        int base = Math.max(1, plugin.getConfig().getInt("clan.max-members", 10));
        Player leader = clan.getLeader() == null ? null : Bukkit.getPlayer(clan.getLeader());
        if (leader != null) {
            int v = Math.max(base, plugin.rankInt(leader, "clan.member-limits", base, "power.clan.members.", "power.clan.members"));
            if (v != clan.getMemberLimit()) {
                clan.setMemberLimit(v);
                markDirty();
            }
            return v;
        }
        return Math.max(base, clan.getMemberLimit());
    }

    /** Wird bei LuckPerms-Aenderungen aufgerufen. */
    public void refreshLimitsFor(UUID uuid) {
        Clan clan = getClan(uuid);
        if (clan != null && uuid.equals(clan.getLeader())) {
            maxMembers(clan);
        }
    }

    /** Nachricht an alle Online-Mitglieder, jeweils in ihrer Sprache. */
    public void broadcastClan(Clan clan, String key, String... placeholders) {
        for (Player member : clan.getOnlineMembers()) {
            m().send(member, key, placeholders);
        }
    }

    // ------------------------------------------------------------------ Validierung

    public boolean validateName(CommandSender sender, String name, Clan ignore) {
        int min = plugin.getConfig().getInt("clan.name-min-length", 3);
        int max = plugin.getConfig().getInt("clan.name-max-length", 16);
        if (name.length() < min || name.length() > max) {
            m().send(sender, "clan.error.name-length", "%min%", String.valueOf(min), "%max%", String.valueOf(max));
            return false;
        }
        String pattern = plugin.getConfig().getString("clan.name-pattern", "[A-Za-z0-9_-]+");
        boolean matches;
        try {
            matches = name.matches(pattern);
        } catch (PatternSyntaxException ex) {
            matches = name.matches("[A-Za-z0-9_-]+");
        }
        if (!matches) {
            m().send(sender, "clan.error.name-invalid");
            return false;
        }
        for (String blocked : plugin.getConfig().getStringList("clan.blocked-names")) {
            if (blocked.equalsIgnoreCase(name) && !sender.hasPermission("powerbat.clan.admin")) {
                m().send(sender, "clan.error.name-blocked");
                return false;
            }
        }
        Clan existing = getClanByName(name);
        if (existing != null && existing != ignore) {
            m().send(sender, "clan.error.name-taken", "%clan%", name);
            return false;
        }
        return true;
    }

    public boolean validateTag(CommandSender sender, String tag) {
        int min = plugin.getConfig().getInt("clan.tag-min-length", 2);
        int max = plugin.getConfig().getInt("clan.tag-max-length", 6);
        if (tag.contains("§") || tag.length() > 64) {
            m().send(sender, "clan.error.tag-invalid");
            return false;
        }
        if (tag.contains("&") && !plugin.getConfig().getBoolean("clan.allow-tag-colors", true)) {
            m().send(sender, "clan.error.tag-colors-disabled");
            return false;
        }
        int plainLength = Colors.strip(tag).trim().length();
        if (plainLength < min || plainLength > max) {
            m().send(sender, "clan.error.tag-length", "%min%", String.valueOf(min), "%max%", String.valueOf(max));
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ Aktionen

    public boolean createClan(Player player, String name) {
        UUID uuid = player.getUniqueId();
        if (getClan(uuid) != null) {
            m().send(player, "clan.error.already-in-clan");
            return false;
        }
        if (!canCreate(player)) {
            m().send(player, "clan.error.no-create-permission");
            return false;
        }
        if (!validateName(player, name, null)) {
            return false;
        }
        Clan clan = new Clan(UUID.randomUUID(), name, System.currentTimeMillis());
        int tagMax = Math.max(1, plugin.getConfig().getInt("clan.tag-max-length", 6));
        String upper = name.toUpperCase(Locale.ROOT);
        clan.setTag(upper.length() > tagMax ? upper.substring(0, tagMax) : upper);
        String color = ClanColor.parse(plugin.getConfig().getString("clan.default-color", "&6"));
        clan.setColor(color == null ? "&7" : color);
        clan.setPvp(plugin.getConfig().getBoolean("clan.default-pvp", false));
        clan.setLeader(uuid);
        clan.getMembers().put(uuid, ClanRank.LEADER);
        index(clan);
        plugin.getInviteManager().clearTarget(uuid);
        save();
        plugin.getDisplay().update(player);

        m().send(player, "clan.created", "%clan%", displayName(clan));
        if (plugin.getConfig().getBoolean("clan.broadcast-create", true)) {
            plugin.broadcastKey("clan.created-broadcast", "%player%", player.getName(), "%clan%", displayName(clan));
        }
        return true;
    }

    public boolean invite(Player actor, Player target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.INVITE)) {
            m().send(actor, "clan.error.no-permission");
            return false;
        }
        if (target.getUniqueId().equals(actor.getUniqueId())) {
            m().send(actor, "clan.error.self-invite");
            return false;
        }
        if (getClan(target.getUniqueId()) != null) {
            m().send(actor, "clan.error.target-in-clan", "%player%", target.getName());
            return false;
        }
        if (!de.power.Settings.allows(target.getUniqueId(), "clan_invites", actor.getUniqueId())) {
            m().send(actor, "clan.error.invites-disabled", "%player%", target.getName());
            return false;
        }
        int pending = plugin.getInviteManager().getForClan(clan.getId()).size();
        if (clan.getMemberCount() + pending >= maxMembers(clan)) {
            m().send(actor, "clan.error.clan-full");
            return false;
        }
        if (!plugin.getInviteManager().add(clan.getId(), actor.getUniqueId(), target.getUniqueId())) {
            m().send(actor, "clan.error.already-invited", "%player%", target.getName());
            return false;
        }
        m().send(actor, "clan.invite-sent", "%player%", target.getName());
        m().send(target, "clan.invite-received",
                "%clan%", displayName(clan),
                "%player%", actor.getName(),
                "%seconds%", String.valueOf(plugin.getInviteManager().expireMillis() / 1000L));
        sendInviteButtons(target, clan);
        return true;
    }

    private void sendInviteButtons(Player target, Clan clan) {
        TextComponent accept = new TextComponent(TextComponent.fromLegacyText(m().getFor(target, "invite.accept-button")));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan accept " + clan.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(m().getFor(target, "invite.accept-hover"))));

        TextComponent deny = new TextComponent(TextComponent.fromLegacyText(m().getFor(target, "invite.deny-button")));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan deny " + clan.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(m().getFor(target, "invite.deny-hover"))));

        TextComponent line = new TextComponent("");
        line.addExtra(accept);
        line.addExtra(new TextComponent("  "));
        line.addExtra(deny);
        target.spigot().sendMessage(line);
    }

    private InviteManager.Invite resolveInvite(Player player, String clanName) {
        if (clanName == null) {
            return plugin.getInviteManager().getLatest(player.getUniqueId());
        }
        Clan clan = getClanByName(clanName);
        return clan == null ? null : plugin.getInviteManager().find(player.getUniqueId(), clan.getId());
    }

    public boolean acceptInvite(Player player, String clanName) {
        if (getClan(player.getUniqueId()) != null) {
            m().send(player, "clan.error.already-in-clan");
            return false;
        }
        InviteManager.Invite invite = resolveInvite(player, clanName);
        Clan clan = invite == null ? null : getClanById(invite.clanId());
        if (clan == null) {
            m().send(player, "clan.error.no-invite");
            return false;
        }
        if (clan.getMemberCount() >= maxMembers(clan)) {
            m().send(player, "clan.error.clan-full");
            plugin.getInviteManager().remove(player.getUniqueId(), clan.getId());
            return false;
        }
        plugin.getInviteManager().clearTarget(player.getUniqueId());
        clan.getMembers().put(player.getUniqueId(), ClanRank.MEMBER);
        playerIndex.put(player.getUniqueId(), clan.getId());
        save();
        broadcastClan(clan, "clan.member-joined", "%player%", player.getName());
        m().send(player, "clan.joined", "%clan%", displayName(clan));
        plugin.getDisplay().update(player);
        return true;
    }

    public boolean denyInvite(Player player, String clanName) {
        InviteManager.Invite invite = resolveInvite(player, clanName);
        Clan clan = invite == null ? null : getClanById(invite.clanId());
        if (invite == null || clan == null) {
            m().send(player, "clan.error.no-invite");
            return false;
        }
        plugin.getInviteManager().remove(player.getUniqueId(), clan.getId());
        m().send(player, "clan.invite-denied", "%clan%", displayName(clan));
        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviter != null) {
            m().send(inviter, "clan.invite-denied-notify", "%player%", player.getName());
        }
        return true;
    }

    public boolean cancelInvite(Player actor, UUID target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.MANAGE_INVITES)) {
            m().send(actor, "clan.error.no-permission");
            return false;
        }
        plugin.getInviteManager().remove(target, clan.getId());
        m().send(actor, "clan.invite-cancelled", "%player%", plugin.getPlayerData().getName(target));
        return true;
    }

    public boolean leave(Player player) {
        Clan clan = getClan(player.getUniqueId());
        if (clan == null) {
            m().send(player, "clan.error.not-in-clan");
            return false;
        }
        if (clan.getRank(player.getUniqueId()) == ClanRank.LEADER) {
            m().send(player, "clan.error.leader-cannot-leave");
            return false;
        }
        removeMember(clan, player.getUniqueId());
        m().send(player, "clan.left", "%clan%", displayName(clan));
        broadcastClan(clan, "clan.member-left", "%player%", player.getName());
        return true;
    }

    private void removeMember(Clan clan, UUID member) {
        clan.getMembers().remove(member);
        playerIndex.remove(member);
        chatMode.remove(member);
        save();
        Player online = Bukkit.getPlayer(member);
        if (online != null) {
            plugin.getDisplay().update(online);
        }
    }

    public boolean kick(Player actor, UUID target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.KICK)) {
            m().send(actor, "clan.error.no-permission");
            return false;
        }
        String targetName = plugin.getPlayerData().getName(target);
        if (target.equals(actor.getUniqueId())) {
            m().send(actor, "clan.error.cannot-target-self");
            return false;
        }
        ClanRank targetRank = clan.getRank(target);
        if (targetRank == null) {
            m().send(actor, "clan.error.target-not-member", "%player%", targetName);
            return false;
        }
        if (!clan.getRank(actor.getUniqueId()).isHigherThan(targetRank)) {
            m().send(actor, "clan.error.cannot-manage", "%player%", targetName);
            return false;
        }
        removeMember(clan, target);
        Player online = Bukkit.getPlayer(target);
        if (online != null) {
            m().send(online, "clan.you-were-kicked", "%clan%", displayName(clan), "%actor%", actor.getName());
        }
        broadcastClan(clan, "clan.member-kicked", "%player%", targetName, "%actor%", actor.getName());
        m().send(actor, "clan.kicked", "%player%", targetName);
        return true;
    }

    public boolean promote(Player actor, UUID target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.PROMOTE)) {
            m().send(actor, "clan.error.no-permission");
            return false;
        }
        String targetName = plugin.getPlayerData().getName(target);
        ClanRank targetRank = clan.getRank(target);
        if (targetRank == null) {
            m().send(actor, "clan.error.target-not-member", "%player%", targetName);
            return false;
        }
        ClanRank actorRank = clan.getRank(actor.getUniqueId());
        if (target.equals(actor.getUniqueId()) || !actorRank.isHigherThan(targetRank)) {
            m().send(actor, "clan.error.cannot-manage", "%player%", targetName);
            return false;
        }
        ClanRank newRank = targetRank.promoted();
        if (newRank == null) {
            m().send(actor, "clan.error.max-rank", "%player%", targetName);
            return false;
        }
        if (!actorRank.isHigherThan(newRank)) {
            m().send(actor, "clan.error.cannot-promote-that-high", "%player%", targetName);
            return false;
        }
        clan.getMembers().put(target, newRank);
        save();
        broadcastClan(clan, "clan.promoted", "%player%", targetName, "%rank%", rankDisplay(newRank), "%actor%", actor.getName());
        return true;
    }

    public boolean demote(Player actor, UUID target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.DEMOTE)) {
            m().send(actor, "clan.error.no-permission");
            return false;
        }
        String targetName = plugin.getPlayerData().getName(target);
        ClanRank targetRank = clan.getRank(target);
        if (targetRank == null) {
            m().send(actor, "clan.error.target-not-member", "%player%", targetName);
            return false;
        }
        ClanRank actorRank = clan.getRank(actor.getUniqueId());
        if (target.equals(actor.getUniqueId()) || !actorRank.isHigherThan(targetRank)) {
            m().send(actor, "clan.error.cannot-manage", "%player%", targetName);
            return false;
        }
        ClanRank newRank = targetRank.demoted();
        if (newRank == null) {
            m().send(actor, "clan.error.min-rank", "%player%", targetName);
            return false;
        }
        clan.getMembers().put(target, newRank);
        save();
        broadcastClan(clan, "clan.demoted", "%player%", targetName, "%rank%", rankDisplay(newRank), "%actor%", actor.getName());
        return true;
    }

    public boolean transfer(Player actor, UUID target) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (clan.getRank(actor.getUniqueId()) != ClanRank.LEADER) {
            m().send(actor, "clan.error.leader-only");
            return false;
        }
        String targetName = plugin.getPlayerData().getName(target);
        if (target.equals(actor.getUniqueId())) {
            m().send(actor, "clan.error.cannot-target-self");
            return false;
        }
        if (!clan.isMember(target)) {
            m().send(actor, "clan.error.target-not-member", "%player%", targetName);
            return false;
        }
        clan.getMembers().put(actor.getUniqueId(), ClanRank.CO_LEADER);
        clan.getMembers().put(target, ClanRank.LEADER);
        clan.setLeader(target);
        save();
        broadcastClan(clan, "clan.transferred", "%player%", targetName, "%actor%", actor.getName());
        return true;
    }

    public boolean disband(Player actor) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return false;
        }
        if (clan.getRank(actor.getUniqueId()) != ClanRank.LEADER) {
            m().send(actor, "clan.error.leader-only");
            return false;
        }
        deleteClan(clan, "clan.disbanded");
        return true;
    }

    /** Löscht einen Clan (auch für Admins). Alle Online-Mitglieder erhalten die Nachricht messageKey. */
    public void deleteClan(Clan clan, String messageKey) {
        String display = displayName(clan);
        List<Player> online = clan.getOnlineMembers();
        unindex(clan);
        save();
        for (Player player : online) {
            m().send(player, messageKey, "%clan%", display);
            plugin.getDisplay().update(player);
        }
    }

    public boolean rename(Player actor, String newName) {
        Clan clan = requireSettingsClan(actor);
        if (clan == null) {
            return false;
        }
        if (clan.getName().equals(newName)) {
            m().send(actor, "clan.error.same-name");
            return false;
        }
        if (!validateName(actor, newName, clan)) {
            return false;
        }
        nameIndex.remove(clan.getName().toLowerCase(Locale.ROOT));
        clan.setName(newName);
        nameIndex.put(newName.toLowerCase(Locale.ROOT), clan.getId());
        save();
        refreshDisplay(clan);
        broadcastClan(clan, "clan.renamed", "%clan%", displayName(clan), "%actor%", actor.getName());
        return true;
    }

    public boolean setColor(Player actor, String input) {
        Clan clan = requireSettingsClan(actor);
        if (clan == null) {
            return false;
        }
        String color = ClanColor.parse(input);
        if (color == null) {
            m().send(actor, "clan.error.invalid-color");
            return false;
        }
        if (!checkFormatting(actor, color)) {
            return false;
        }
        clan.setColor(color);
        save();
        refreshDisplay(clan);
        broadcastClan(clan, "clan.color-changed", "%clan%", displayName(clan), "%color%", color + ClanColor.nameOf(color), "%actor%", actor.getName());
        return true;
    }

    public boolean setTag(Player actor, String tag) {
        Clan clan = requireSettingsClan(actor);
        if (clan == null) {
            return false;
        }
        if (!validateTag(actor, tag) || !checkFormatting(actor, tag)) {
            return false;
        }
        clan.setTag(tag);
        save();
        refreshDisplay(clan);
        broadcastClan(clan, "clan.tag-changed", "%tag%", displayTag(clan), "%actor%", actor.getName());
        return true;
    }

    public boolean setDescription(Player actor, String description) {
        Clan clan = requireSettingsClan(actor);
        if (clan == null) {
            return false;
        }
        int max = plugin.getConfig().getInt("clan.max-description-length", 60);
        if (description.length() > max || description.contains("§")) {
            m().send(actor, "clan.error.description-length", "%max%", String.valueOf(max));
            return false;
        }
        if (!checkFormatting(actor, description)) {
            return false;
        }
        clan.setDescription(description);
        save();
        m().send(actor, "clan.description-changed", "%description%", Colors.color(description));
        return true;
    }

    /** Schaltet PvP zwischen Clan-Mitgliedern um. */
    public boolean togglePvp(Player actor) {
        Clan clan = requireSettingsClan(actor);
        if (clan == null) {
            return false;
        }
        clan.setPvp(!clan.isPvp());
        save();
        broadcastClan(clan, clan.isPvp() ? "clan.pvp-enabled" : "clan.pvp-disabled", "%actor%", actor.getName());
        return true;
    }

    private static final Pattern FORMAT_CODE = Pattern.compile("&(#[0-9a-fA-F]{6}|[0-9a-fA-FklmnorxKLMNORX])");

    /**
     * Farben, Fett, Kursiv usw. in Clan-Tag/-Farbe/-Beschreibung sind nur mit speziellem Rang erlaubt:
     * powerbat.clan.color, powerbat.clan.bold, powerbat.clan.italic, powerbat.clan.format (unterstrichen/durchgestrichen/magisch).
     */
    public boolean checkFormatting(Player player, String raw) {
        if (player.hasPermission("powerbat.admin")) {
            return true;
        }
        Matcher matcher = FORMAT_CODE.matcher(raw);
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase(Locale.ROOT);
            String permission;
            String what;
            if (code.startsWith("#") || "0123456789abcdef".contains(code)) {
                permission = "powerbat.clan.color";
                what = "Farben";
            } else if (code.equals("l")) {
                permission = "powerbat.clan.bold";
                what = "Fettschrift";
            } else if (code.equals("o")) {
                permission = "powerbat.clan.italic";
                what = "Kursivschrift";
            } else if (code.equals("r")) {
                continue;
            } else {
                permission = "powerbat.clan.format";
                what = "Sonderformate";
            }
            if (!player.hasPermission(permission)) {
                m().send(player, "clan.error.format-no-permission", "%what%", what, "%perm%", permission);
                return false;
            }
        }
        return true;
    }

    private Clan requireSettingsClan(Player actor) {
        Clan clan = getClan(actor.getUniqueId());
        if (clan == null) {
            m().send(actor, "clan.error.not-in-clan");
            return null;
        }
        if (!hasPermission(clan, actor.getUniqueId(), ClanPermission.EDIT_SETTINGS)) {
            m().send(actor, "clan.error.no-permission");
            return null;
        }
        return clan;
    }

    // ------------------------------------------------------------------ Clan-Chat

    public boolean toggleChat(Player player) {
        Clan clan = getClan(player.getUniqueId());
        if (clan == null) {
            m().send(player, "clan.error.not-in-clan");
            return false;
        }
        if (!hasPermission(clan, player.getUniqueId(), ClanPermission.CLAN_CHAT)) {
            m().send(player, "clan.error.no-permission");
            return false;
        }
        if (chatMode.remove(player.getUniqueId())) {
            m().send(player, "clan.chat-disabled");
        } else {
            chatMode.add(player.getUniqueId());
            m().send(player, "clan.chat-enabled");
        }
        return true;
    }

    public void sendClanChat(Player sender, String message) {
        Clan clan = getClan(sender.getUniqueId());
        if (clan == null) {
            chatMode.remove(sender.getUniqueId());
            m().send(sender, "clan.error.not-in-clan");
            return;
        }
        if (!hasPermission(clan, sender.getUniqueId(), ClanPermission.CLAN_CHAT)) {
            m().send(sender, "clan.error.no-permission");
            return;
        }
        String text = de.power.ChatColors.apply(plugin, sender, message);
        String format = plugin.getConfig().getString("chat.clan-format",
                "&8[%clancolor%%clanname%&8] &6[%rank%&6] &f%player%&8: &f%message%");
        String line = Colors.color(format
                .replace("%clancolor%", clan.getColor())
                .replace("%clanname%", clan.getName())
                .replace("%clantag%", displayTag(clan))
                .replace("%rank%", rankDisplay(clan.getRank(sender.getUniqueId())))
                .replace("%player%", sender.getName()));
        line = line.replace("%message%", text);
        clan.broadcast(line);
        Bukkit.getConsoleSender().sendMessage("[ClanChat] " + Colors.strip(line));
    }

    // ------------------------------------------------------------------ Hilfsfunktionen für Statistiken

    public PlayerData stats(UUID id) {
        return plugin.getPlayerData().get(id);
    }
}
