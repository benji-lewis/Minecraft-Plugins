package uk.co.xfour.kimjongun2;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class KimJongUnSpawner {
    private final KimJongUn2Plugin plugin;
    private final KimJongUnItems items;
    private BukkitRunnable task;
    private final Random random = new Random();

    public KimJongUnSpawner(KimJongUn2Plugin plugin, KimJongUnItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    public void start() {
        FileConfiguration config = plugin.getConfig();
        long intervalTicks = Math.max(20L, config.getLong("spawn.interval-seconds", 300) * 20L);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                attemptSpawn();
            }
        };
        task.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public void attemptSpawn() {
        List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        if (players.isEmpty()) {
            return;
        }
        FileConfiguration config = plugin.getConfig();
        double chance = config.getDouble("spawn.chance-per-interval", 0.35);
        if (random.nextDouble() > chance) {
            return;
        }
        int maxActive = config.getInt("spawn.max-active", 2);
        int active = countActiveMobs(players.get(0).getWorld());
        if (active >= maxActive) {
            return;
        }
        Player target = players.get(random.nextInt(players.size()));
        Location spawnLocation = pickSpawnLocation(target);
        if (spawnLocation == null) {
            return;
        }
        spawnMob(spawnLocation);
    }

    public LivingEntity spawnMob(Location location) {
        FileConfiguration config = plugin.getConfig();
        String typeName = config.getString("spawn.entity-type", "PIGLIN_BRUTE");
        EntityType type;
        try {
            type = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            type = EntityType.PIGLIN_BRUTE;
        }
        if (!type.isAlive()) {
            type = EntityType.PIGLIN_BRUTE;
        }
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        entity.setCustomName("Kim Jong Un 2");
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(items.keys().mobKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        entity.setRemoveWhenFarAway(false);
        return entity;
    }

    private int countActiveMobs(World world) {
        return (int) world.getLivingEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(items.keys().mobKey, org.bukkit.persistence.PersistentDataType.BYTE))
                .count();
    }

    private Location pickSpawnLocation(Player player) {
        FileConfiguration config = plugin.getConfig();
        int min = config.getInt("spawn.min-distance-from-player", 20);
        int max = config.getInt("spawn.max-distance-from-player", 60);
        World world = player.getWorld();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = min + random.nextDouble() * (max - min);
            int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location location = new Location(world, x + 0.5, y, z + 0.5);
            if (location.getBlock().getType().isAir()) {
                return location;
            }
        }
        return null;
    }
}
