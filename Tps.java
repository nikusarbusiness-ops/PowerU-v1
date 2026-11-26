
package de.power;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.lang.management.ManagementFactory;

/** /tps - Server-Status mit TPS-Balken, RAM, Chunks und Entities. */
public final class Tps {

    private Tps() { }

    private static double[] tps() {
        try {
            Object result = Bukkit.getServer()
                    .getClass()
                    .getMethod("getTPS")
                    .invoke(Bukkit.getServer());

            if (result instanceof double[] values && values.length >= 3) {
                return values;
            }
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // TPS-Methode nicht verfügbar
        }

        return new double[]{20.0, 20.0, 20.0};
    }

    private static String col(double t) {
        if (t >= 19.0) return "§a";
        if (t >= 17.0) return "§e";
        if (t >= 14.0) return "§6";
        return "§c";
    }

    private static String shown(double t) {
        double v = Math.min(20.0, t);
        return col(v) + String.format(java.util.Locale.US, "%.1f", v)
                + (t > 20.0 ? "*" : "");
    }

    private static String bar(double t) {
        int total = 20;
        int filled = (int) Math.round(
                Math.min(20.0, Math.max(0, t)) / 20.0 * total
        );

        return col(t) + "█".repeat(filled)
                + "§8" + "█".repeat(total - filled);
    }

    private static String state(double t) {
        if (t >= 19.5) return "§aAusgezeichnet";
        if (t >= 18.0) return "§aGut";
        if (t >= 15.0) return "§eMäßig";
        return "§cSchlecht";
    }

    public static void show(Power pl, CommandSender s) {
        double[] t = tps();

        Runtime rt = Runtime.getRuntime();

        long max = rt.maxMemory() / 1048576L;
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;

        int pct = max <= 0 ? 0 : (int) (used * 100 / max);

        String ramCol = pct >= 90 ? "§c" : pct >= 75 ? "§e" : "§a";

        long up = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        int chunks = 0;
        int entities = 0;

        for (World w : Bukkit.getWorlds()) {
            chunks += w.getLoadedChunks().length;
            entities += w.getEntities().size();
        }

        send(pl, s, "§8§m          §r §b§lServer-Status §8§m          ");
        send(pl, s, "§7Zustand: " + state(t[0]));

        send(pl, s,
                "§7TPS §8(1m §7| §85m §7| §815m§8)§7: "
                        + shown(t[0]) + " §8| "
                        + shown(t[1]) + " §8| "
                        + shown(t[2])
        );

        send(pl, s, "§7" + bar(t[0]));

        send(pl, s,
                "§7RAM: " + ramCol + used
                        + " MB §8/ §f" + max
                        + " MB §8(" + ramCol + pct + "%§8)"
        );

        send(pl, s,
                "§7Spieler: §f" + Bukkit.getOnlinePlayers().size()
                        + "§8/§f" + Bukkit.getMaxPlayers()
                        + " §8| §7Chunks: §f" + chunks
                        + " §8| §7Entities: §f" + entities
        );

        send(pl, s, "§7Laufzeit: §f" + pl.fmtTime(up));
        send(pl, s, "§8§m                                        ");
    }

    private static void send(Power pl, CommandSender s, String line) {
        s.sendMessage(Lang.tr(s, line));
    }
}
