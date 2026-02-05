package uk.co.xfour.kimjongun;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LaunchManager {
    private final KimJongUnPlugin plugin;
    private final KimJongUnItems items;
    private final FalloutManager falloutManager;
    private final Set<ArmorStand> activeMissiles = new HashSet<>();

    public LaunchManager(KimJongUnPlugin plugin, KimJongUnItems items, FalloutManager falloutManager) {
        this.plugin = plugin;
        this.items = items;
        this.falloutManager = falloutManager;
    }

    public void launchMissile(Location launchLocation, Vector direction) {
        World world = launchLocation.getWorld();
        if (world == null) {
            return;
        }
        Location start = launchLocation.clone().add(0, 0.9, 0);
        ArmorStand missile = (ArmorStand) world.spawnEntity(start, EntityType.ARMOR_STAND);
        missile.setInvisible(true);
        missile.setMarker(true);
        missile.setGravity(false);
        missile.setSmall(true);
        missile.setSilent(true);
        missile.setInvulnerable(true);
        missile.getEquipment().setHelmet(items.createItem(KimJongUnItems.KimJongUnItem.ICBM));
        missile.getPersistentDataContainer().set(items.keys().missileKey, PersistentDataType.BYTE, (byte) 1);
        activeMissiles.add(missile);

        int ascentTicks = plugin.getConfig().getInt("launch.ascent-ticks", 80);
        double step = plugin.getConfig().getDouble("launch.ascent-step", 0.35);
        double explosionPower = plugin.getConfig().getDouble("launch.explosion-power", 6.0);
        boolean showFirework = plugin.getConfig().getBoolean("launch.show-firework", true);

        Vector upward = new Vector(0, step, 0).add(direction.clone().multiply(0.05));
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!missile.isValid() || !activeMissiles.contains(missile)) {
                    cancel();
                    return;
                }
                if (ticks == 0) {
                    world.playSound(start, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.6f, 0.7f);
                }
                Location current = missile.getLocation().add(upward);
                missile.teleport(current);
                world.spawnParticle(Particle.FLAME, current, 8, 0.15, 0.1, 0.15, 0.02);
                world.spawnParticle(Particle.SMOKE, current, 6, 0.2, 0.1, 0.2, 0.01);
                ticks++;
                if (ticks >= ascentTicks) {
                    missile.remove();
                    activeMissiles.remove(missile);
                    if (showFirework) {
                        spawnFirework(current);
                    }
                    world.createExplosion(current, (float) explosionPower, false, false);
                    world.playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Launches an ICBM from the given launchpad location to a target location.
     *
     * @param launchLocation the launchpad launch position
     * @param targetLocation the target impact location
     */
    public void launchIcbm(Location launchLocation, Location targetLocation) {
        World world = launchLocation.getWorld();
        if (world == null || targetLocation.getWorld() == null) {
            return;
        }
        Location start = launchLocation.clone().add(0, 0.9, 0);
        ArmorStand missile = (ArmorStand) world.spawnEntity(start, EntityType.ARMOR_STAND);
        missile.setInvisible(true);
        missile.setMarker(true);
        missile.setGravity(false);
        missile.setSmall(true);
        missile.setSilent(true);
        missile.setInvulnerable(true);
        missile.getEquipment().setHelmet(items.createItem(KimJongUnItems.KimJongUnItem.MISSILE));
        missile.getPersistentDataContainer().set(items.keys().missileKey, PersistentDataType.BYTE, (byte) 1);
        activeMissiles.add(missile);

        int ascentTicks = plugin.getConfig().getInt("icbm.ascent-ticks", 100);
        double ascentStep = plugin.getConfig().getDouble("icbm.ascent-step", 0.8);
        double cruiseHeight = plugin.getConfig().getDouble("icbm.cruise-height", 170.0);
        double cruiseSpeed = plugin.getConfig().getDouble("icbm.cruise-speed", 2.4);
        double descentSpeed = plugin.getConfig().getDouble("icbm.descent-speed", 3.0);
        boolean showFirework = plugin.getConfig().getBoolean("icbm.show-firework", true);

        new BukkitRunnable() {
            int ticks = 0;
            Phase phase = Phase.ASCENT;

            @Override
            public void run() {
                if (!missile.isValid() || !activeMissiles.contains(missile)) {
                    cancel();
                    return;
                }
                Location current = missile.getLocation();
                switch (phase) {
                    case ASCENT -> {
                        if (ticks == 0) {
                            world.playSound(start, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
                        }
                        double nextY = Math.min(current.getY() + ascentStep, cruiseHeight);
                        Location ascentLocation = current.clone();
                        ascentLocation.setY(nextY);
                        missile.teleport(ascentLocation);
                        ticks++;
                        if (ticks >= ascentTicks || nextY >= cruiseHeight) {
                            phase = Phase.CRUISE;
                        }
                    }
                    case CRUISE -> {
                        Vector horizontal = new Vector(
                            targetLocation.getX() - current.getX(),
                            0,
                            targetLocation.getZ() - current.getZ()
                        );
                        double distance = horizontal.length();
                        if (distance <= cruiseSpeed) {
                            phase = Phase.DESCENT;
                        } else {
                            horizontal.normalize().multiply(cruiseSpeed);
                            Location cruiseLocation = current.clone().add(horizontal);
                            cruiseLocation.setY(cruiseHeight);
                            missile.teleport(cruiseLocation);
                        }
                    }
                    case DESCENT -> {
                        Vector toTarget = targetLocation.toVector().subtract(current.toVector());
                        double distance = toTarget.length();
                        if (distance <= descentSpeed) {
                            missile.remove();
                            activeMissiles.remove(missile);
                            if (showFirework) {
                                spawnFirework(targetLocation);
                            }
                            createNuclearBlast(targetLocation);
                            cancel();
                            return;
                        }
                        missile.teleport(current.clone().add(toTarget.normalize().multiply(descentSpeed)));
                    }
                    default -> {
                    }
                }
                world.spawnParticle(Particle.FLAME, missile.getLocation(), 10, 0.18, 0.18, 0.18, 0.02);
                world.spawnParticle(Particle.SMOKE, missile.getLocation(), 8, 0.22, 0.22, 0.22, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnFirework(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Firework firework = (Firework) world.spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.RED, Color.ORANGE)
                .withFade(Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.BURST)
                .trail(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

    private void createNuclearBlast(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        float power = (float) plugin.getConfig().getDouble("icbm.impact-explosion-power", 18.0);
        boolean createFire = plugin.getConfig().getBoolean("icbm.impact-fire", true);
        world.createExplosion(location, power, createFire, false);
        world.spawnParticle(Particle.EXPLOSION, location, 1);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.4f);
        spawnMushroomCloud(location);
        double radius = plugin.getConfig().getDouble("icbm.fallout.radius", 40.0);
        int durationTicks = plugin.getConfig().getInt("icbm.fallout.duration-ticks", 1200);
        falloutManager.addZone(location, radius, java.time.Duration.ofMillis(durationTicks * 50L));
    }

    private void spawnMushroomCloud(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int durationTicks = plugin.getConfig().getInt("icbm.mushroom-cloud.duration-ticks", 200);
        double maxRadius = plugin.getConfig().getDouble("icbm.mushroom-cloud.max-radius", 18.0);
        double maxHeight = plugin.getConfig().getDouble("icbm.mushroom-cloud.max-height", 32.0);
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= durationTicks) {
                    cancel();
                    return;
                }
                double progress = tick / (double) durationTicks;
                double radius = maxRadius * progress;
                double height = maxHeight * progress;
                Location plumeBase = location.clone().add(0, height, 0);
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2) * i / 16.0;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, plumeBase.clone().add(x, 0, z),
                        6, 0.2, 0.2, 0.2, 0.01);
                }
                world.spawnParticle(Particle.CLOUD, plumeBase, 12, radius * 0.3, 0.4, radius * 0.3, 0.02);
                tick += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    public void shutdown() {
        for (ArmorStand stand : activeMissiles) {
            if (stand.isValid()) {
                stand.remove();
            }
        }
        activeMissiles.clear();
    }

    private enum Phase {
        ASCENT,
        CRUISE,
        DESCENT
    }
}
