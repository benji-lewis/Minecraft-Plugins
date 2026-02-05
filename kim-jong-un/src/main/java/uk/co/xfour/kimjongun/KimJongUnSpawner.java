package uk.co.xfour.kimjongun;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages periodic spawning of the Kim Jong Un mob.
 */
public class KimJongUnSpawner {
    private final KimJongUnPlugin plugin;
    private final KimJongUnItems items;
    private BukkitRunnable task;
    private final Random random = new Random();

    public KimJongUnSpawner(KimJongUnPlugin plugin, KimJongUnItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    /**
     * Starts the timed spawn task based on configuration.
     */
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

    /**
     * Stops the timed spawn task if it is running.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Attempts a spawn using the configured chance and player proximity rules.
     */
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
        Player target = players.get(random.nextInt(players.size()));
        int maxActive = config.getInt("spawn.max-active", 2);
        int active = countActiveMobs(target.getWorld());
        if (active >= maxActive) {
            return;
        }
        Location spawnLocation = pickSpawnLocation(target);
        if (spawnLocation == null) {
            return;
        }
        spawnMob(spawnLocation);
    }

    /**
     * Spawns a configured mob at the provided location.
     *
     * @param location the spawn location
     * @return the spawned entity
     */
    public LivingEntity spawnMob(Location location) {
        FileConfiguration config = plugin.getConfig();
        String typeName = config.getString("spawn.entity-type", "VILLAGER");
        EntityType type;
        try {
            type = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            type = EntityType.VILLAGER;
        }
        if (!type.isAlive()) {
            type = EntityType.VILLAGER;
        }
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        entity.setCustomName("Kim Jong Un");
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(items.keys().mobKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        entity.setRemoveWhenFarAway(false);
        entity.setAI(true);
        entity.setInvisible(false);
        entity.setInvulnerable(false);
        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(createSkinDisplay());
            entity.getEquipment().setChestplate(null);
            entity.getEquipment().setLeggings(null);
            entity.getEquipment().setBoots(null);
            entity.getEquipment().setItemInMainHand(null);
            entity.getEquipment().setItemInOffHand(null);
        }
        return entity;
    }

    private ItemStack createSkinDisplay() {
        FileConfiguration config = plugin.getConfig();
        String skinPlayer = config.getString("skin.player-name", "kimyou12345");
        int modelData = config.getInt("skin.custom-model-data", 5012);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(skinPlayer));
            meta.setCustomModelData(modelData);
            head.setItemMeta(meta);
        }
        return head;
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
        if (max < min) {
            int swap = max;
            max = min;
            min = swap;
        }
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
