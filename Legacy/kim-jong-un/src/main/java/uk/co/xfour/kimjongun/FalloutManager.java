package uk.co.xfour.kimjongun;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tracks active fallout zones and applies radiation damage to nearby players.
 */
public class FalloutManager {
    private final KimJongUnPlugin plugin;
    private final RadiationSuit radiationSuit;
    private final List<FalloutZone> zones = new ArrayList<>();
    private BukkitRunnable task;

    public FalloutManager(KimJongUnPlugin plugin, RadiationSuit radiationSuit) {
        this.plugin = plugin;
        this.radiationSuit = radiationSuit;
    }

    /**
     * Starts the repeating fallout check task.
     */
    public void start() {
        if (task != null) {
            return;
        }
        int interval = plugin.getConfig().getInt("icbm.fallout.tick-interval", 20);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickFallout();
            }
        };
        task.runTaskTimer(plugin, 0L, Math.max(1, interval));
    }

    /**
     * Stops the fallout task and clears any active zones.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        zones.clear();
    }

    /**
     * Adds a fallout zone at the given location.
     *
     * @param center the center of the fallout zone
     * @param radius the radius of the fallout zone
     * @param duration the duration of the fallout
     */
    public void addZone(Location center, double radius, Duration duration) {
        zones.add(new FalloutZone(center, radius, System.currentTimeMillis() + duration.toMillis()));
    }

    private void tickFallout() {
        if (zones.isEmpty()) {
            return;
        }
        Iterator<FalloutZone> iterator = zones.iterator();
        long now = System.currentTimeMillis();
        while (iterator.hasNext()) {
            FalloutZone zone = iterator.next();
            if (now >= zone.expiresAt) {
                iterator.remove();
                continue;
            }
            World world = zone.center.getWorld();
            if (world == null) {
                continue;
            }
            double radiusSquared = zone.radius * zone.radius;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(zone.center) > radiusSquared) {
                    continue;
                }
                if (radiationSuit.isFullyProtected(player)) {
                    spawnSafeParticles(player.getLocation());
                    continue;
                }
                applyRadiation(player);
            }
        }
    }

    private void applyRadiation(Player player) {
        double damage = plugin.getConfig().getDouble("icbm.fallout.damage-per-tick", 1.5);
        int durationTicks = plugin.getConfig().getInt("icbm.fallout.effect-duration-ticks", 100);
        int witherAmplifier = plugin.getConfig().getInt("icbm.fallout.wither-amplifier", 1);
        int nauseaAmplifier = plugin.getConfig().getInt("icbm.fallout.nausea-amplifier", 0);
        if (damage > 0) {
            player.damage(damage);
        }
        if (durationTicks > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, durationTicks, witherAmplifier, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, nauseaAmplifier, true, true));
        }
        Location particleLocation = player.getLocation().clone().add(0, 1.2, 0);
        player.spawnParticle(Particle.ASH, particleLocation, 18, 0.3, 0.5, 0.3, 0.01);
    }

    private void spawnSafeParticles(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        Location particleLocation = location.clone().add(0, 1.2, 0);
        location.getWorld().spawnParticle(Particle.END_ROD, particleLocation, 4, 0.2, 0.2, 0.2, 0.01);
    }

    private static final class FalloutZone {
        private final Location center;
        private final double radius;
        private final long expiresAt;

        private FalloutZone(Location center, double radius, long expiresAt) {
            this.center = center.clone();
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }
}
