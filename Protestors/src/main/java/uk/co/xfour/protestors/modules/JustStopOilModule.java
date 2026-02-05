package uk.co.xfour.protestors.modules;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Just Stop Oil module logic.
 */
public class JustStopOilModule implements Module, Listener {
    private static final double FLEE_SPEED = 0.35;
    private static final double FLEE_VELOCITY = 0.6;
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private NamespacedKey protesterKey;
    private NamespacedKey fleeingKey;

    public JustStopOilModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        protesterKey = new NamespacedKey(plugin, "jso_protester");
        fleeingKey = new NamespacedKey(plugin, "jso_fleeing");
    }

    @Override
    public void stop() {
    }

    @EventHandler
    public void onLavaPlaced(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET) return;
        double chance = plugin.getConfig().getDouble("modules.just-stop-oil.spawn-chance", 0.35);
        if (random.nextDouble() > chance) return;
        Location base = event.getBlockClicked() == null ? event.getPlayer().getLocation()
            : event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        Villager villager = base.getWorld().spawn(base, Villager.class);
        villager.setCustomName(ChatColor.GOLD + "Just Stop Oil Protester");
        villager.getPersistentDataContainer().set(protesterKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(fleeingKey, PersistentDataType.BYTE, (byte) 0);
        shout(villager);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager villager && isProtester(villager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !isProtester(villager) || isFleeing(villager)) return;
        villager.getPersistentDataContainer().set(fleeingKey, PersistentDataType.BYTE, (byte) 1);
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker != null) {
            Vector direction = villager.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
            villager.setVelocity(direction.multiply(FLEE_VELOCITY));
        }
        AttributeInstance speed = villager.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(FLEE_SPEED);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (villager.isValid()) villager.remove();
            }
        }.runTaskLater(plugin, plugin.getConfig().getInt("modules.just-stop-oil.despawn-ticks", 300));
    }

    private boolean isProtester(Villager villager) {
        Byte value = villager.getPersistentDataContainer().get(protesterKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private boolean isFleeing(Villager villager) {
        Byte value = villager.getPersistentDataContainer().get(fleeingKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) return living;
        return null;
    }

    private void shout(Villager villager) {
        List<String> messages = plugin.getConfig().getStringList("modules.just-stop-oil.messages");
        if (messages.isEmpty()) return;
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("modules.just-stop-oil.prefix", "&6[JSO]"));
        String msg = ChatColor.translateAlternateColorCodes('&', messages.get(random.nextInt(messages.size())));
        for (Player player : villager.getLocation().getNearbyPlayers(plugin.getConfig().getDouble("modules.just-stop-oil.radius", 16.0))) {
            player.sendMessage(prefix + " " + msg);
        }
    }
}
