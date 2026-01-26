package uk.co.xfour.petavolunteers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class PetaVolunteersPlugin extends JavaPlugin implements Listener {
    private static final double SPAWN_CHANCE = 0.3;
    private static final int SHOUT_INTERVAL_TICKS = 120;
    private static final int DESPAWN_DELAY_TICKS = 100;
    private static final double WALK_SPEED = 0.06;
    private static final double FLEE_SPEED = 0.35;
    private static final double FLEE_VELOCITY = 0.6;

    private static final List<String> SHOUTS = List.of(
            "Shame on you for hurting animals!",
            "Every creature deserves compassion!",
            "Violence against animals is not okay!",
            "Think about the innocent lives you take!",
            "You should be ashamed of that cruelty!"
    );

    private final Random random = new Random();
    private NamespacedKey volunteerKey;
    private NamespacedKey fleeingKey;

    @Override
    public void onEnable() {
        volunteerKey = new NamespacedKey(this, "peta_volunteer");
        fleeingKey = new NamespacedKey(this, "peta_fleeing");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals)) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (random.nextDouble() > SPAWN_CHANCE) {
            return;
        }

        spawnVolunteer(entity.getLocation());
    }

    @EventHandler
    public void onVolunteerAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (!isVolunteer(villager)) {
            return;
        }

        if (isFleeing(villager)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        markFleeing(villager);
        playScream(villager.getLocation());
        fleeFromAttacker(villager, event.getDamager());
        scheduleDespawn(villager);
    }

    private void spawnVolunteer(Location location) {
        if (location.getWorld() == null) {
            return;
        }

        Location spawnLocation = location.clone().add(randomOffset(), 0, randomOffset());
        Villager villager = location.getWorld().spawn(spawnLocation, Villager.class, spawned -> {
            spawned.setCustomName(ChatColor.LIGHT_PURPLE + "PETA Volunteer");
            spawned.setCustomNameVisible(true);
            spawned.setAdult();
            spawned.setSilent(false);
            spawned.setRemoveWhenFarAway(true);
        });

        setVolunteerData(villager);
        setMovementSpeed(villager, WALK_SPEED);
        startShouting(villager);
    }

    private double randomOffset() {
        return random.nextInt(7) - 3;
    }

    private void setVolunteerData(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(volunteerKey, PersistentDataType.BYTE, (byte) 1);
        container.set(fleeingKey, PersistentDataType.BYTE, (byte) 0);
    }

    private boolean isVolunteer(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        Byte value = container.get(volunteerKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private boolean isFleeing(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        Byte value = container.get(fleeingKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private void markFleeing(Villager villager) {
        villager.getPersistentDataContainer().set(fleeingKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void setMovementSpeed(Villager villager, double speed) {
        AttributeInstance attribute = villager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(speed);
        }
    }

    private void startShouting(Villager villager) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!villager.isValid() || villager.isDead()) {
                    cancel();
                    return;
                }
                if (isFleeing(villager)) {
                    return;
                }
                String shout = SHOUTS.get(random.nextInt(SHOUTS.size()));
                Bukkit.broadcastMessage(ChatColor.RED + "[PETA] " + ChatColor.YELLOW + shout);
            }
        }.runTaskTimer(this, SHOUT_INTERVAL_TICKS, SHOUT_INTERVAL_TICKS);
    }

    private void fleeFromAttacker(Villager villager, Entity damager) {
        LivingEntity attacker = resolveAttacker(damager);
        if (attacker != null) {
            Vector direction = villager.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
            villager.setVelocity(direction.multiply(FLEE_VELOCITY));
        }
        setMovementSpeed(villager, FLEE_SPEED);
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private void scheduleDespawn(Villager villager) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (villager.isValid()) {
                    villager.remove();
                }
            }
        }.runTaskLater(this, DESPAWN_DELAY_TICKS);
    }

    private void playScream(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().playSound(location, Sound.ENTITY_GHAST_SCREAM, 1.0f, 1.2f);
    }
}
