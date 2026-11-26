package de.power;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lag-armer Random Teleport:
 * - pro Server-Tick wird hoechstens EIN Versuch (= ein Chunk) fuer alle Suchen zusammen ausgefuehrt
 * - ungeeignete Biome (Ozean, Fluss ...) werden verworfen, BEVOR ein Chunk geladen wird
 * - Chunks um das Ziel werden vor dem Teleport nach und nach vorgeladen
 * - Cooldown pro Spieler (rtp.cooldown-seconds)
 */
public final class RtpManager {

    private static final class Search {
        World w;
        int min, max, attempts;
        UUID owner;
        Consumer<Location> ok;
        Runnable fail;
    }

    private final Power pl;
    private final Random rnd = new Random();
    private final Deque<Search> queue = new ArrayDeque<>();
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private BukkitTask task;

    public RtpManager(Power pl) { this.pl = pl; }

    // ------------------------------------------------------------------ Cooldown

    /** Verbleibende Sekunden bis /rtp wieder erlaubt ist (0 = erlaubt). */
    public long cooldownLeft(UUID id) {
        Long until = cooldown.get(id);
        if (until == null) return 0;
        long left = (until - System.currentTimeMillis() + 999) / 1000;
        return Math.max(0, left);
    }

    public void startCooldown(UUID id) {
        int sec = pl.getConfig().getInt("rtp.cooldown-seconds", 10);
        if (sec > 0) cooldown.put(id, System.currentTimeMillis() + sec * 1000L);
    }

    public void clearCooldown(UUID id) { cooldown.remove(id); }

    public boolean isSearching(UUID id) {
        for (Search s : queue) if (id.equals(s.owner)) return true;
        return false;
    }

    // ------------------------------------------------------------------ Suche

    public void find(World w, int min, int max, UUID owner, Consumer<Location> ok, Runnable fail) {
        Search s = new Search();
        s.w = w;
        s.min = Math.max(0, min);
        s.max = Math.max(s.min + 1, max);
        s.owner = owner;
        s.ok = ok;
        s.fail = fail;
        queue.addLast(s);
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(pl, this::tick, 1L, 1L);
    }

    private void tick() {
        Search s = queue.pollFirst();
        if (s == null) {
            if (task != null) { task.cancel(); task = null; }
            return;
        }
        if (s.owner != null && Bukkit.getPlayer(s.owner) == null) return; // Spieler weg -> Suche verwerfen
        Location found = null;
        try {
            found = attempt(s);
        } catch (Exception ex) {
            pl.getLogger().warning("RTP-Versuch fehlgeschlagen: " + ex.getMessage());
        }
        if (found != null) {
            hold(found);
            preload(found);
            s.ok.accept(found);
            return;
        }
        int maxAttempts = Math.max(1, pl.getConfig().getInt("rtp.max-attempts", 30));
        if (s.attempts >= maxAttempts) {
            s.fail.run();
        } else {
            queue.addLast(s); // hinten anstellen -> faire Verteilung
        }
    }

    private Location attempt(Search s) {
        s.attempts++;
        World w = s.w;
        WorldBorder b = w.getWorldBorder();
        Location c = b.getCenter();
        double half = b.getSize() / 2.0 - 20;
        if (half < 1) return null;
        double maxR = Math.min(s.max, half);
        double minR = Math.min(s.min, maxR - 1);
        double ang = rnd.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(rnd.nextDouble() * (maxR * maxR - minR * minR) + minR * minR);
        int x = (int) Math.floor(c.getX() + Math.cos(ang) * r);
        int z = (int) Math.floor(c.getZ() + Math.sin(ang) * r);

        if (w.getEnvironment() == World.Environment.NORMAL && badBiome(w, x, z)) return null;

        w.getChunkAt(x >> 4, z >> 4); // einziger Chunk-Load pro Versuch
        int y;
        if (w.getEnvironment() == World.Environment.NETHER) {
            y = netherY(w, x, z);
            if (y < 0) return null;
        } else {
            y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        }
        Block ground = w.getBlockAt(x, y, z);
        if (!safe(ground)) return null;
        return new Location(w, x + 0.5, y + 1, z + 0.5, rnd.nextFloat() * 360f, 0f);
    }

    private boolean badBiome(World w, int x, int z) {
        List<String> avoid = pl.getConfig().getStringList("rtp.avoid-biomes");
        if (avoid.isEmpty()) return false;
        try {
            Biome bi = w.getBiome(x, 64, z);
            String key = bi.getKey().getKey().toLowerCase();
            for (String a : avoid) if (key.contains(a.toLowerCase())) return true;
        } catch (Exception ignored) { }
        return false;
    }

    private int netherY(World w, int x, int z) {
        for (int y = 120; y > 32; y--) {
            Block g = w.getBlockAt(x, y, z);
            if (g.getType().isSolid() && w.getBlockAt(x, y + 1, z).isPassable() && w.getBlockAt(x, y + 2, z).isPassable()
                    && !w.getBlockAt(x, y + 1, z).isLiquid() && !w.getBlockAt(x, y + 2, z).isLiquid()) return y;
        }
        return -1;
    }

    private boolean safe(Block ground) {
        Material g = ground.getType();
        if (!g.isSolid() || Tag.LEAVES.isTagged(g)) return false;
        switch (g) {
            case MAGMA_BLOCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE, POWDER_SNOW, SWEET_BERRY_BUSH, FIRE, SOUL_FIRE,
                 WITHER_ROSE, BEDROCK -> { return false; }
            default -> { }
        }
        Block f = ground.getRelative(0, 1, 0);
        Block h = ground.getRelative(0, 2, 0);
        return f.isPassable() && h.isPassable() && !f.isLiquid() && !h.isLiquid()
                && f.getType() != Material.FIRE && f.getType() != Material.SOUL_FIRE
                && f.getType() != Material.SWEET_BERRY_BUSH && f.getType() != Material.WITHER_ROSE;
    }

    // ------------------------------------------------------------------ Chunks halten / vorladen

    private void hold(Location loc) {
        try {
            Chunk ch = loc.getChunk();
            ch.addPluginChunkTicket(pl);
            Bukkit.getScheduler().runTaskLater(pl, () -> ch.removePluginChunkTicket(pl), 20L * 20);
        } catch (Throwable ignored) { }
    }

    private void preload(Location loc) {
        final int radius = Math.max(0, pl.getConfig().getInt("rtp.preload-radius", 2));
        if (radius == 0) return;
        final World w = loc.getWorld();
        final int cx = loc.getBlockX() >> 4;
        final int cz = loc.getBlockZ() >> 4;
        final Deque<int[]> todo = new ArrayDeque<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                todo.add(new int[]{cx + dx, cz + dz});
            }
        }
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int[] c = todo.pollFirst();
                if (c == null) { cancel(); return; }
                if (!w.isChunkLoaded(c[0], c[1])) {
                    try {
                        Chunk ch = w.getChunkAt(c[0], c[1]);
                        ch.addPluginChunkTicket(pl);
                        Bukkit.getScheduler().runTaskLater(pl, () -> ch.removePluginChunkTicket(pl), 20L * 20);
                    } catch (Throwable ignored) { }
                }
            }
        }.runTaskTimer(pl, 1L, 1L);
    }

    public void clear(Player p) {
        cooldown.remove(p.getUniqueId());
        queue.removeIf(s -> p.getUniqueId().equals(s.owner));
    }
}
