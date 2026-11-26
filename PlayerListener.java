package de.power.bat.listeners;

import de.power.Power;
import de.power.bat.clan.InviteManager;
import de.power.bat.utils.Colors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final Power plugin;

    public PlayerListener(Power plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerData().update(player);
        plugin.getPlayerData().get(player.getUniqueId()).setLastSeen(System.currentTimeMillis());
        plugin.getPlayerData().markDirty();
        plugin.getDisplay().update(player);
        plugin.getFriendManager().notifyJoin(player);
        // LuckPerms-Daten sind beim Join geladen; ein zweites Update 1 Tick später fängt Nachzügler ab
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getDisplay().update(player);
                List<InviteManager.Invite> invites = plugin.getInviteManager().getFor(player.getUniqueId());
                if (!invites.isEmpty()) {
                    plugin.getMessages().send(player, "clan.pending-invites", "%count%", String.valueOf(invites.size()));
                }
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerData().get(player.getUniqueId()).setLastSeen(System.currentTimeMillis());
        plugin.getPlayerData().markDirty();
        if (!plugin.isShuttingDown()) {
            plugin.getFriendManager().notifyQuit(player);
        }
        plugin.getDisplay().removeNametag(player);
    }

    /** Nimmt Texteingaben aus GUIs entgegen (z.B. Clan-Name) - muss vor allen anderen Chat-Handlern laufen. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInput().has(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getChatInput().handle(player, message));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getClanManager().getClan(player.getUniqueId()) != null
                && plugin.getClanManager().isChatMode(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getClanManager().sendClanChat(player, message));
            return;
        }

        event.setMessage(de.power.ChatColors.apply(plugin, player, event.getMessage()));
        if (!plugin.getConfig().getBoolean("chat.manage-format", true)) {
            return;
        }
        event.setFormat(plugin.getDisplay().buildChatFormat(player));
    }
}
