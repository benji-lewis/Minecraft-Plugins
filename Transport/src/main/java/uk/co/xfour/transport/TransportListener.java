package uk.co.xfour.transport;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Handles transport item interactions.
 */
public final class TransportListener implements Listener {
    private final JavaPlugin plugin;
    private final TransportItemFactory itemFactory;
    private final NamespacedKey vehicleKey;
    private final Map<UUID, BukkitTask> flightTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> fuelTasks = new HashMap<>();

    /**
     * Creates a new transport listener.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public TransportListener(JavaPlugin plugin, TransportItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.vehicleKey = new NamespacedKey(plugin, "transport-vehicle");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        TransportItemType type = itemFactory.getType(item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        switch (type) {
            case TRAIN_ENGINE -> spawnTrain(player, event, item);
            case PLANE_FRAME -> spawnPlane(player, item);
            case CAR_CHASSIS -> spawnCar(player, item);
            case RAIL_KIT -> placeRail(player, event, item);
            case SPACESHIP_CORE -> spawnSpaceship(player, item);
            default -> {
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        UUID vehicleId = event.getVehicle().getUniqueId();
        BukkitTask task = flightTasks.remove(vehicleId);
        if (task != null) {
            task.cancel();
        }
        BukkitTask fuelTask = fuelTasks.remove(vehicleId);
        if (fuelTask != null) {
            fuelTask.cancel();
        }
    }

    private void spawnTrain(Player player, PlayerInteractEvent event, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.trains.enabled", true)) {
            player.sendMessage("Trains module is disabled.");
            return;
        }
        Location location = resolveRailLocation(player, event);
        if (location == null) {
            player.sendMessage("Train engines must be placed on rails.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Minecart minecart = world.spawn(location, Minecart.class);
        minecart.setCustomName("Train Engine");
        minecart.setMaxSpeed(plugin.getConfig().getDouble("modules.trains.default-speed", 0.8));
        minecart.setSlowWhenEmpty(false);
        storeVehicleType(minecart, "train");
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        minecart.setVelocity(direction.multiply(0.3));
        playConfiguredSound(player, "modules.trains.whistle-sound", Sound.ENTITY_VILLAGER_YES);
        mount(player, minecart);
        consumeItem(player, item);
    }

    private void spawnPlane(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.planes.enabled", true)) {
            player.sendMessage("Planes module is disabled.");
            return;
        }
        Location location = player.getLocation();
        if (!isRunwayClear(location, plugin.getConfig().getInt("modules.planes.runway-length", 64))) {
            player.sendMessage("You need a clear runway to deploy the plane frame.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Entity spawned = world.spawnEntity(location, EntityType.OAK_BOAT);
        if (!(spawned instanceof Boat boat)) {
            player.sendMessage("Unable to deploy the plane frame right now.");
            return;
        }
        boat.setCustomName("Plane Frame");
        boat.setGravity(false);
        boat.setInvulnerable(true);
        storeVehicleType(boat, "plane");
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        mount(player, boat);
        startFlightControl(boat, player,
                plugin.getConfig().getDouble("modules.planes.max-speed", 1.6),
                plugin.getConfig().getDouble("modules.planes.cruise-altitude", 120),
                false,
                false);
        consumeItem(player, item);
    }

    private void spawnCar(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.cars.enabled", true)) {
            player.sendMessage("Cars module is disabled.");
            return;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Horse horse = world.spawn(location, Horse.class);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setAdult();
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setCustomName("Car Chassis");
        horse.setCustomNameVisible(true);
        horse.setInvulnerable(true);
        if (horse.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double maxSpeed = plugin.getConfig().getDouble("modules.cars.max-speed", 1.2);
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.2 * maxSpeed);
        }
        storeVehicleType(horse, "car");
        startFuelTracking(horse, player);
        mount(player, horse);
        consumeItem(player, item);
    }

    private void spawnSpaceship(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.spaceships.enabled", true)) {
            player.sendMessage("Spaceships module is disabled.");
            return;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Entity spawned = world.spawnEntity(location, EntityType.OAK_BOAT);
        if (!(spawned instanceof Boat boat)) {
            player.sendMessage("Unable to deploy the spaceship right now.");
            return;
        }
        boat.setCustomName("Spaceship Core");
        boat.setGravity(false);
        boat.setInvulnerable(true);
        storeVehicleType(boat, "spaceship");
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        mount(player, boat);
        double maxSpeed = plugin.getConfig().getDouble("modules.spaceships.max-speed", 3.2);
        boolean autoLaunch = plugin.getConfig().getBoolean("modules.spaceships.auto-launch", true);
        boolean integrationEnabled = plugin.getConfig().getBoolean("modules.spaceships.integration.enable-space-travel",
                true);
        boolean autoDock = plugin.getConfig().getBoolean("modules.spaceships.integration.auto-dock", true);
        double targetAltitude = Math.min(world.getMaxHeight() - 16, 240);
        startFlightControl(boat, player, maxSpeed, targetAltitude, autoLaunch, integrationEnabled && autoDock);
        consumeItem(player, item);
    }

    private void placeRail(Player player, PlayerInteractEvent event, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.rail-systems.enabled", true)) {
            player.sendMessage("Rail systems module is disabled.");
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Location target = event.getClickedBlock().getLocation().add(0, 1, 0);
        if (target.getBlock().getType() != Material.AIR) {
            player.sendMessage("You need an empty block above to place the rail kit.");
            return;
        }
        Material railMaterial = Material.RAIL;
        List<String> railTypes = plugin.getConfig().getStringList("modules.rail-systems.rail-types");
        if (player.isSneaking() && railTypes.stream().anyMatch(type -> type.equalsIgnoreCase("high-speed"))) {
            railMaterial = Material.POWERED_RAIL;
        }
        target.getBlock().setType(railMaterial);
        consumeItem(player, item);
    }

    private void mount(Player player, Entity vehicle) {
        if (vehicle instanceof Vehicle) {
            vehicle.addPassenger(player);
        }
    }

    private void consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }

    private Location resolveRailLocation(Player player, PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null && isRail(clicked.getType())) {
            return clicked.getLocation().add(0.5, 0.1, 0.5);
        }
        Block block = player.getLocation().getBlock();
        if (isRail(block.getType())) {
            return block.getLocation().add(0.5, 0.1, 0.5);
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (isRail(below.getType())) {
            return below.getLocation().add(0.5, 0.1, 0.5);
        }
        return null;
    }

    private boolean isRail(Material material) {
        return material == Material.RAIL
                || material == Material.POWERED_RAIL
                || material == Material.DETECTOR_RAIL
                || material == Material.ACTIVATOR_RAIL;
    }

    private boolean isRunwayClear(Location location, int length) {
        Vector direction = location.getDirection().setY(0).normalize();
        if (direction.lengthSquared() < 0.01) {
            direction = new Vector(1, 0, 0);
        }
        for (int i = 1; i <= length; i++) {
            Location check = location.clone().add(direction.clone().multiply(i));
            if (!check.getBlock().getType().isAir() || !check.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    private void startFlightControl(Vehicle vehicle, Player pilot, double maxSpeed, double targetAltitude,
                                    boolean autoLaunch, boolean autoDock) {
        UUID vehicleId = vehicle.getUniqueId();
        BukkitTask existing = flightTasks.remove(vehicleId);
        if (existing != null) {
            existing.cancel();
        }
        double speedPerTick = maxSpeed / 10.0;
        BukkitTask task = new BukkitRunnable() {
            private boolean dockingTriggered;

            @Override
            public void run() {
                if (!vehicle.isValid()) {
                    cancel();
                    return;
                }
                if (vehicle.getPassengers().isEmpty() || !(vehicle.getPassengers().get(0) instanceof Player player)) {
                    vehicle.remove();
                    cancel();
                    return;
                }
                Location location = vehicle.getLocation();
                Vector direction = player.getLocation().getDirection();
                Vector horizontal = direction.clone().setY(0).normalize();
                if (horizontal.lengthSquared() < 0.01) {
                    horizontal = location.getDirection().setY(0).normalize();
                }
                double vertical = -Math.sin(Math.toRadians(player.getLocation().getPitch())) * (speedPerTick / 2);
                if (autoLaunch) {
                    double altitudeBias = (targetAltitude - location.getY()) * 0.02;
                    vertical += Math.max(-0.4, Math.min(0.4, altitudeBias));
                }
                vehicle.setVelocity(horizontal.multiply(speedPerTick).setY(vertical));
                if (autoDock && !dockingTriggered && location.getY() >= targetAltitude - 2) {
                    dockingTriggered = true;
                    teleportToOrbitalDock(player);
                    vehicle.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        flightTasks.put(vehicleId, task);
    }

    private void teleportToOrbitalDock(Player player) {
        Location target = player.getWorld().getSpawnLocation().clone();
        target.setY(Math.max(target.getY(), 200));
        player.teleport(target);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        player.sendMessage("Spaceship auto-docked with the nearest orbital station.");
    }

    private void startFuelTracking(Horse horse, Player driver) {
        UUID vehicleId = horse.getUniqueId();
        BukkitTask existing = fuelTasks.remove(vehicleId);
        if (existing != null) {
            existing.cancel();
        }
        int defaultFuel = plugin.getConfig().getInt("modules.cars.default-fuel", 100);
        BukkitTask task = new BukkitRunnable() {
            private int fuel = defaultFuel;

            @Override
            public void run() {
                if (!horse.isValid()) {
                    cancel();
                    return;
                }
                if (horse.getPassengers().isEmpty()) {
                    return;
                }
                if (horse.getVelocity().lengthSquared() < 0.01) {
                    return;
                }
                fuel = Math.max(0, fuel - 1);
                if (fuel == 0) {
                    horse.remove();
                    driver.sendMessage("Your car is out of fuel and shuts down.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        fuelTasks.put(vehicleId, task);
    }

    private void storeVehicleType(Entity entity, String type) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(vehicleKey, PersistentDataType.STRING, type.toLowerCase(Locale.ROOT));
    }

    private void playConfiguredSound(Player player, String path, Sound fallback) {
        String raw = plugin.getConfig().getString(path);
        Sound sound = fallback;
        if (raw != null) {
            String normalized = raw.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "").replace('.', '_');
            normalized = normalized.replace(':', '_');
            try {
                sound = Sound.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                sound = fallback;
            }
        }
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}
