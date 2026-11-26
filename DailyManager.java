package de.power;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /daily - Login-Streak mit konfigurierbaren Belohnungen. */
public final class DailyManager {

    private final Power pl;

    public DailyManager(Power pl) { this.pl = pl; }

    public boolean enabled() { return pl.getConfig().getBoolean("daily.enabled", true); }

    private ZoneId zone() {
        String tz = pl.getConfig().getString("daily.timezone", "");
        try {
            if (tz != null && !tz.isBlank()) return ZoneId.of(tz.trim());
        } catch (Exception ignored) { }
        return ZoneId.systemDefault();
    }

    public long today() { return LocalDate.now(zone()).toEpochDay(); }

    private int grace() { return Math.max(0, pl.getConfig().getInt("daily.grace-days", 0)); }

    /** Anzahl der konfigurierten Tage (hoechste Tagesnummer, max. 28). */
    public int cycle() {
        ConfigurationSection s = pl.getConfig().getConfigurationSection("daily.rewards");
        int max = 0;
        if (s != null) {
            for (String k : s.getKeys(false)) {
                try { max = Math.max(max, Integer.parseInt(k.trim())); } catch (NumberFormatException ignored) { }
            }
        }
        return Math.max(1, Math.min(28, max));
    }

    private long last(UUID id) { return pl.store().getLong(id, "daily.last", 0L); }

    private int storedStreak(UUID id) { return pl.store().getInt(id, "daily.streak", 0); }

    public boolean claimedToday(UUID id) { return last(id) == today(); }

    /** Aktuelle Streak (0, wenn sie schon verfallen ist). */
    public int streak(UUID id) {
        long l = last(id);
        if (l == 0) return 0;
        long diff = today() - l;
        return diff <= 1 + grace() ? storedStreak(id) : 0;
    }

    public boolean canClaim(UUID id) { return enabled() && !claimedToday(id); }

    /** Streak, die man nach einem Claim jetzt haette. */
    public int streakAfterClaim(UUID id) { return streak(id) + 1; }

    /** Tagesnummer (1..cycle) der Belohnung zu einer Streak. */
    public int dayIndex(int streak) {
        int c = cycle();
        if (streak < 1) return 1;
        if (pl.getConfig().getBoolean("daily.loop", true)) return ((streak - 1) % c) + 1;
        return Math.min(streak, c);
    }

    public ConfigurationSection reward(int day) {
        return pl.getConfig().getConfigurationSection("daily.rewards." + day);
    }

    public List<String> describe(int day) {
        List<String> out = new ArrayList<>();
        ConfigurationSection r = reward(day);
        if (r == null) return out;
        if (r.getInt("blitze", 0) > 0) out.add("&7⚡ Blitze: &e+" + r.getInt("blitze"));
        if (r.getDouble("money", 0) > 0) out.add("&7Geld: &e+" + pl.eco().format(r.getDouble("money")));
        for (String it : r.getStringList("items")) {
            String[] sp = it.split(":");
            Material m = Material.matchMaterial(sp[0].trim());
            if (m == null) continue;
            out.add("&7Item: &e" + (sp.length > 1 ? sp[1].trim() : "1") + "x " + de.power.eco.U.pretty(m));
        }
        for (String l : r.getStringList("lore")) out.add(l);
        return out;
    }

    /** Belohnung abholen. */
    public boolean claim(Player p) {
        UUID id = p.getUniqueId();
        if (!enabled()) { pl.msg(p, "§cDas Daily-System ist deaktiviert."); return false; }
        if (claimedToday(id)) { pl.msg(p, "§cDu hast deine Belohnung heute schon abgeholt. Komm morgen wieder!"); return false; }
        int streak = streakAfterClaim(id);
        int day = dayIndex(streak);
        pl.store().setInt(id, "daily.streak", streak);
        pl.store().setLong(id, "daily.last", today());
        ConfigurationSection r = reward(day);
        if (r != null) {
            int blitze = r.getInt("blitze", 0);
            if (blitze > 0) pl.addBlitze(id, blitze);
            double money = r.getDouble("money", 0);
            if (money > 0) pl.eco().deposit(id, money);
            for (String it : r.getStringList("items")) {
                String[] sp = it.split(":");
                Material m = Material.matchMaterial(sp[0].trim());
                if (m == null || !m.isItem()) continue;
                int amt = 1;
                try { if (sp.length > 1) amt = Math.max(1, Integer.parseInt(sp[1].trim())); } catch (NumberFormatException ignored) { }
                de.power.eco.U.give(p, new ItemStack(m, amt));
            }
            for (String c : r.getStringList("commands")) {
                String cmd = c.replace("%player%", p.getName()).replace("%uuid%", id.toString());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            String m = r.getString("message", "");
            if (m != null && !m.isEmpty()) pl.msg(p, Power.color(m));
        }
        pl.msg(p, "§aDaily-Belohnung abgeholt! §7Streak: §e" + streak + " §7Tag(e)");
        if (Settings.isOn(id, "sounds")) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        return true;
    }
}
