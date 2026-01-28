package uk.co.xfour.kimjongun2;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LaunchManager {
    private final KimJongUn2Plugin plugin;
    private final KimJongUnItems items;
    private final Set<ArmorStand> activeMissiles = new HashSet<>();

    public LaunchManager(KimJongUn2Plugin plugin, KimJongUnItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    public ArmorStand spawnLaunchpad(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setSilent(true);
        stand.setInvulnerable(true);
        stand.getEquipment().setHelmet(items.createItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD));
        stand.getPersistentDataContainer().set(items.keys().launchpadKey, PersistentDataType.BYTE, (byte) 1);
        return stand;
    }

    public boolean isLaunchpad(ArmorStand stand) {
        return stand.getPersistentDataContainer().has(items.keys().launchpadKey, PersistentDataType.BYTE);
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
        missile.getEquipment().setHelmet(items.createItem(KimJongUnItems.KimJongUnItem.MISSILE));
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

    public void shutdown() {
        for (ArmorStand stand : activeMissiles) {
            if (stand.isValid()) {
                stand.remove();
            }
        }
        activeMissiles.clear();
    }
}
