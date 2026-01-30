package uk.co.xfour.juststopoil;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Main plugin entry point for Just Stop Oil.
 */
public class JustStopOilPlugin extends JavaPlugin implements Listener {
    private static final String CONFIG_SPAWN_CHANCE = "protest.spawn-chance";
    private static final String CONFIG_RADIUS = "protest.radius";
    private static final String CONFIG_PREFIX = "protest.prefix";
    private static final String CONFIG_MESSAGES = "protest.messages";
    private static final String CONFIG_SHOUT_MIN_TICKS = "protest.shout-interval-min-ticks";
    private static final String CONFIG_SHOUT_MAX_TICKS = "protest.shout-interval-max-ticks";
    private static final String CONFIG_SHOUT_CHANCE = "protest.shout-chance";
    private static final String CONFIG_DESPAWN_TICKS = "protest.despawn-delay-ticks";
    private static final String CONFIG_SPRAY_ENABLED = "protest.spray.enabled";
    private static final String CONFIG_SPRAY_RADIUS = "protest.spray.radius";
    private static final String CONFIG_SPRAY_INTERVAL_TICKS = "protest.spray.interval-ticks";
    private static final String CONFIG_SPRAY_DURATION_TICKS = "protest.spray.duration-ticks";
    private static final String CONFIG_SPRAY_BLOCKS_PER_BURST = "protest.spray.blocks-per-burst";
    private static final String CONFIG_SPRAY_PAINT_TICKS = "protest.spray.paint-ticks";
    private static final String CONFIG_COMMAND_MAX_SPAWN = "protest.command.max-spawn";

    private static final double FLEE_SPEED = 0.35;
    private static final double WALK_SPEED = 0.1;
    private static final double FLEE_VELOCITY = 0.6;
    private static final double SPRAY_PARTICLE_SPEED = 0.05;

    private final Random random = new Random();
    private List<String> messages;
    private NamespacedKey protesterKey;
    private NamespacedKey fleeingKey;
    private double spawnChance;
    private double shoutRadius;
    private String prefix;
    private int shoutMinTicks;
    private int shoutMaxTicks;
    private double shoutChance;
    private int despawnTicks;
    private boolean sprayEnabled;
    private int sprayRadius;
    private int sprayIntervalTicks;
    private int sprayDurationTicks;
    private int sprayBlocksPerBurst;
    private int sprayPaintTicks;
    private int commandMaxSpawn;

    /**
     * Loads configuration and registers event handlers.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        protesterKey = new NamespacedKey(this, "just_stop_oil_protester");
        fleeingKey = new NamespacedKey(this, "just_stop_oil_fleeing");
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
    }

    /**
     * Registers plugin commands with the server command map.
     */
    private void registerCommands() {
        org.bukkit.command.PluginCommand command = getCommand("juststopoil");
        if (command == null) {
            getLogger().warning("Command 'juststopoil' could not be registered. Check plugin.yml metadata.");
            return;
        }
        command.setExecutor(this);
    }

    private void loadConfigValues() {
        spawnChance = clampChance(getConfig().getDouble(CONFIG_SPAWN_CHANCE, 0.35));
        shoutRadius = Math.max(1.0, getConfig().getDouble(CONFIG_RADIUS, 18.0));
        prefix = getConfig().getString(CONFIG_PREFIX, "&2[Just Stop Oil]");
        messages = getConfig().getStringList(CONFIG_MESSAGES);
        shoutMinTicks = Math.max(20, getConfig().getInt(CONFIG_SHOUT_MIN_TICKS, 80));
        shoutMaxTicks = Math.max(shoutMinTicks, getConfig().getInt(CONFIG_SHOUT_MAX_TICKS, 260));
        shoutChance = clampChance(getConfig().getDouble(CONFIG_SHOUT_CHANCE, 0.45));
        despawnTicks = Math.max(20, getConfig().getInt(CONFIG_DESPAWN_TICKS, 400));
        sprayEnabled = getConfig().getBoolean(CONFIG_SPRAY_ENABLED, true);
        sprayRadius = Math.max(1, getConfig().getInt(CONFIG_SPRAY_RADIUS, 6));
        sprayIntervalTicks = Math.max(10, getConfig().getInt(CONFIG_SPRAY_INTERVAL_TICKS, 30));
        sprayDurationTicks = Math.max(40, getConfig().getInt(CONFIG_SPRAY_DURATION_TICKS, 200));
        sprayBlocksPerBurst = Math.max(1, getConfig().getInt(CONFIG_SPRAY_BLOCKS_PER_BURST, 4));
        sprayPaintTicks = Math.max(40, getConfig().getInt(CONFIG_SPRAY_PAINT_TICKS, 200));
        commandMaxSpawn = Math.max(1, getConfig().getInt(CONFIG_COMMAND_MAX_SPAWN, 5));

        if (messages.isEmpty()) {
            getLogger().warning("No protest messages configured. Protesters will remain silent.");
        }
    }

    private double clampChance(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Handles lava placement to potentially spawn a protester.
     *
     * @param event the bucket empty event
     */
    @EventHandler
    public void onLavaPlaced(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET) {
            return;
        }
        if (random.nextDouble() > spawnChance) {
            return;
        }

        Location baseLocation = event.getBlockClicked() == null
                ? event.getPlayer().getLocation()
                : event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        spawnProtester(baseLocation, event.getPlayer());
    }

    /**
     * Prevents players from trading with protester villagers.
     *
     * @param event the interaction event
     */
    @EventHandler
    public void onProtesterInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!isProtester(villager)) {
            return;
        }
        event.setCancelled(true);
    }

    /**
     * Makes protesters flee when attacked.
     *
     * @param event the damage event
     */
    @EventHandler
    public void onProtesterAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (!isProtester(villager) || isFleeing(villager)) {
            return;
        }

        markFleeing(villager);
        shoutAtNearbyPlayers(villager);
        playProtestSound(villager.getLocation());
        fleeFromAttacker(villager, event.getDamager());
        scheduleDespawn(villager);
    }

    /**
     * Handles the /juststopoil command.
     *
     * @param sender the command sender
     * @param command the command
     * @param label the command label
     * @param args command arguments
     * @return true if handled
     */
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender,
                             org.bukkit.command.Command command,
                             String label,
                             String[] args) {
        if (!command.getName().equalsIgnoreCase("juststopoil")) {
            return false;
        }
        if (!sender.hasPermission("juststopoil.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("spawn")) {
            sender.sendMessage(ChatColor.RED + "Usage: /juststopoil spawn <count>");
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Count must be a number.");
            return true;
        }
        if (count < 1) {
            sender.sendMessage(ChatColor.RED + "Count must be at least 1.");
            return true;
        }
        if (count > commandMaxSpawn) {
            sender.sendMessage(ChatColor.RED + "Count must be " + commandMaxSpawn + " or less.");
            return true;
        }
        Location baseLocation;
        if (sender instanceof Player player) {
            baseLocation = player.getLocation();
        } else if (sender instanceof org.bukkit.command.BlockCommandSender blockSender) {
            baseLocation = blockSender.getBlock().getLocation();
        } else {
            sender.sendMessage(ChatColor.RED + "Console must specify a command block location.");
            return true;
        }
        for (int i = 0; i < count; i++) {
            spawnProtester(baseLocation, null);
        }
        sender.sendMessage(ChatColor.GREEN + "Spawned " + count + " protesters.");
        return true;
    }

    private void spawnProtester(Location location, Player trigger) {
        if (location.getWorld() == null) {
            return;
        }

        Location spawnLocation = location.clone().add(randomOffset(), 0, randomOffset());
        Villager villager = location.getWorld().spawn(spawnLocation, Villager.class, spawned -> {
            spawned.setCustomName(ChatColor.GOLD + "Just Stop Oil Protester");
            spawned.setCustomNameVisible(true);
            spawned.setAdult();
            spawned.setSilent(false);
            spawned.setRemoveWhenFarAway(true);
            setMovementSpeed(spawned, WALK_SPEED);
        });
        setProtesterData(villager);
        spawnSmokeEffect(villager.getLocation());
        shoutAtNearbyPlayers(villager);
        scheduleNextShout(villager);
        scheduleSpraySequence(villager);
        scheduleDespawn(villager);
        if (trigger != null) {
            playProtestSound(trigger.getLocation());
        }
    }

    private double randomOffset() {
        return random.nextInt(5) - 2;
    }

    private void setProtesterData(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(protesterKey, PersistentDataType.BYTE, (byte) 1);
        container.set(fleeingKey, PersistentDataType.BYTE, (byte) 0);
    }

    private boolean isProtester(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        Byte value = container.get(protesterKey, PersistentDataType.BYTE);
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
        AttributeInstance attribute = villager.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(speed);
        }
    }

    private void scheduleNextShout(Villager villager) {
        int delay = shoutMinTicks + random.nextInt(shoutMaxTicks - shoutMinTicks + 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!villager.isValid() || villager.isDead()) {
                    return;
                }
                if (!isFleeing(villager) && random.nextDouble() <= shoutChance) {
                    shoutAtNearbyPlayers(villager);
                }
                scheduleNextShout(villager);
            }
        }.runTaskLater(this, delay);
    }

    /**
     * Starts a scheduled animation where protesters spray orange wool onto nearby buildings.
     *
     * @param villager the protester villager
     */
    private void scheduleSpraySequence(Villager villager) {
        if (!sprayEnabled) {
            return;
        }
        new BukkitRunnable() {
            private int ticksElapsed = 0;

            @Override
            public void run() {
                if (!villager.isValid() || villager.isDead()) {
                    cancel();
                    return;
                }
                sprayOrangeWool(villager);
                ticksElapsed += sprayIntervalTicks;
                if (ticksElapsed >= sprayDurationTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, sprayIntervalTicks);
    }

    /**
     * Paints nearby blocks orange for a short duration while emitting spray particles.
     *
     * @param villager the protester villager
     */
    private void sprayOrangeWool(Villager villager) {
        if (villager.getWorld() == null) {
            return;
        }
        for (int i = 0; i < sprayBlocksPerBurst; i++) {
            org.bukkit.block.Block target = findSprayTarget(villager.getLocation());
            if (target == null) {
                continue;
            }
            org.bukkit.Material original = target.getType();
            if (original == org.bukkit.Material.ORANGE_WOOL) {
                continue;
            }
            target.setType(org.bukkit.Material.ORANGE_WOOL);
            spawnSprayParticles(target.getLocation().add(0.5, 0.5, 0.5));
            schedulePaintRevert(target, original);
        }
    }

    private org.bukkit.block.Block findSprayTarget(Location origin) {
        if (origin.getWorld() == null) {
            return null;
        }
        int attempts = 12;
        for (int i = 0; i < attempts; i++) {
            int x = origin.getBlockX() + random.nextInt(sprayRadius * 2 + 1) - sprayRadius;
            int y = origin.getBlockY() + random.nextInt(5) - 2;
            int z = origin.getBlockZ() + random.nextInt(sprayRadius * 2 + 1) - sprayRadius;
            org.bukkit.block.Block candidate = origin.getWorld().getBlockAt(x, y, z);
            if (!isSprayable(candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    /**
     * Checks if a block can be sprayed without breaking containers or liquids.
     *
     * @param block the candidate block
     * @return true if the block should be painted
     */
    private boolean isSprayable(org.bukkit.block.Block block) {
        org.bukkit.Material type = block.getType();
        if (!type.isSolid() || type.isAir()) {
            return false;
        }
        return !type.name().endsWith("_CHEST") && !type.name().contains("SHULKER");
    }

    private void schedulePaintRevert(org.bukkit.block.Block block, org.bukkit.Material original) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == org.bukkit.Material.ORANGE_WOOL) {
                    block.setType(original);
                }
            }
        }.runTaskLater(this, sprayPaintTicks);
    }

    private void spawnSprayParticles(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().spawnParticle(Particle.ITEM, location, 12, 0.2, 0.2, 0.2, SPRAY_PARTICLE_SPEED,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_DYE));
    }

    private void shoutAtNearbyPlayers(Villager villager) {
        if (messages.isEmpty()) {
            return;
        }
        String message = messages.get(random.nextInt(messages.size()));
        String formatted = ProtestMessageFormatter.buildMessage(
                ChatColor.translateAlternateColorCodes('&', prefix),
                ChatColor.translateAlternateColorCodes('&', message));
        for (Player player : villager.getLocation().getNearbyPlayers(shoutRadius)) {
            player.sendMessage(formatted);
        }
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
        }.runTaskLater(this, despawnTicks);
    }

    private void spawnSmokeEffect(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 18, 0.4, 0.6, 0.4, 0.01);
    }

    private void playProtestSound(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
    }
}
