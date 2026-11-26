package de.power.bat.listeners;

import de.power.Power;
import de.power.bat.clan.Clan;
import de.power.bat.combat.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;
import java.util.UUID;

public class CombatListener implements Listener {

    private final Power plugin;
    private final CombatManager combat;

    public CombatListener(Power plugin) {
        this.plugin = plugin;
        this.combat = plugin.getCombatManager();
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    /** Friendly-Fire-Schutz innerhalb eines Clans. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        Clan attackerClan = plugin.getClanManager().getClan(attacker.getUniqueId());
        if (attackerClan != null && !attackerClan.isPvp() && attackerClan.isMember(victim.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessages().send(attacker, "clan.friendly-fire");
        }
    }

    /** Combat-Tag bei Treffer. MONITOR + ignoreCancelled: nur echte, nicht abgebrochene Treffer zählen. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (victim.hasMetadata("NPC") || attacker.hasMetadata("NPC")) {
            return;
        }
        combat.tag(attacker, victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("combat.block-commands", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!combat.isInCombat(player.getUniqueId()) || combat.isBypassed(player)) {
            return;
        }
        String message = event.getMessage();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }
        String command = message.split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = command.indexOf(':');
        if (colon >= 0) {
            command = command.substring(colon + 1);
        }
        for (String blocked : plugin.getConfig().getStringList("combat.blocked-commands")) {
            if (blocked.equalsIgnoreCase(command)) {
                event.setCancelled(true);
                plugin.getMessages().send(player, "combat.command-blocked");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        plugin.getChatInput().clear(id);
        plugin.getClanManager().removeChatMode(id);

        if (plugin.isShuttingDown()) {
            return;
        }
        if (combat.isInCombat(id) && !combat.isBypassed(player) && !player.isDead()) {
            combat.handleLogout(player);
        } else {
            combat.untag(id, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        boolean logout = combat.isLoggingOut(victimId);

        Player killer = victim.getKiller();
        if (killer == null && logout) {
            UUID opponentId = combat.getLastOpponent(victimId);
            if (opponentId != null) {
                killer = Bukkit.getPlayer(opponentId);
            }
        }
        if (killer != null && killer.getUniqueId().equals(victimId)) {
            killer = null;
        }

        if (logout) {
            event.setDeathMessage(plugin.getMessages().get("combat.logout-death-message", "%player%", victim.getName()));
        }

        plugin.getStats().recordDeath(victimId);
        if (killer != null) {
            plugin.getStats().recordKill(killer.getUniqueId(), victimId);
        }
        combat.onDeath(victim, killer);
    }
}
