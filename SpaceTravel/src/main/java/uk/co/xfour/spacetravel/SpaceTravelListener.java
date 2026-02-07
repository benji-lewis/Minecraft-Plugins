package uk.co.xfour.spacetravel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles space travel item interactions.
 */
public final class SpaceTravelListener implements Listener {
    private static final List<Material> ASTEROID_ORES = List.of(
            Material.ANCIENT_DEBRIS,
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.AMETHYST_SHARD);

    private final JavaPlugin plugin;
    private final SpaceTravelItemFactory itemFactory;
    private final NamespacedKey launchPadKey;
    private final Map<UUID, Instant> launchCooldowns = new HashMap<>();
    private final Map<UUID, Instant> routeCooldowns = new HashMap<>();
    private final Map<UUID, Integer> routeUses = new HashMap<>();
    private final Map<String, List<Location>> dockLocations = new HashMap<>();
    private final Random random = new Random();

    /**
     * Creates a new space travel listener.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public SpaceTravelListener(JavaPlugin plugin, SpaceTravelItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.launchPadKey = new NamespacedKey(plugin, "launch-pad");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && isLaunchPad(event.getClickedBlock())) {
            event.setCancelled(true);
            activateLaunchPad(event.getPlayer());
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && handleSuitRepair(event, item)) {
            return;
        }
        SpaceTravelItemType type = itemFactory.getType(item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);

        switch (type) {
            case LAUNCH_PAD -> placeLaunchPad(event, item);
            case ORBITAL_PASS -> activateOrbitalPass(event.getPlayer(), item);
            case ROUTE_CHART -> activateRouteChart(event.getPlayer(), item);
            case SPACE_SUIT_MODULE -> activateSpaceSuit(event.getPlayer(), item);
            case ASTEROID_DRILL -> activateAsteroidDrill(event.getPlayer(), item);
            default -> {
            }
        }
    }

    private void placeLaunchPad(PlayerInteractEvent event, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.launch-pads.enabled", true)) {
            event.getPlayer().sendMessage("Launch pads module is disabled.");
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Block target = event.getClickedBlock().getRelative(event.getBlockFace());
        if (!target.getType().isAir()) {
            event.getPlayer().sendMessage("You need an empty block to place the launch pad.");
            return;
        }
        target.setType(SpaceTravelItemType.LAUNCH_PAD.getMaterial());
        BlockState state = target.getState();
        if (state instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(launchPadKey, PersistentDataType.BYTE, (byte) 1);
            tileState.update(true);
        }
        consumeItem(event.getPlayer(), item);
    }

    private void activateLaunchPad(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("modules.launch-pads.enabled", true)) {
            player.sendMessage("Launch pads module is disabled.");
            return;
        }
        int cooldownSeconds = plugin.getConfig().getInt("modules.launch-pads.cooldown-seconds", 45);
        Instant lastLaunch = launchCooldowns.get(player.getUniqueId());
        if (lastLaunch != null) {
            Duration since = Duration.between(lastLaunch, Instant.now());
            if (since.getSeconds() < cooldownSeconds) {
                player.sendMessage("Launch pad recharging. Please wait " + (cooldownSeconds - since.getSeconds())
                        + "s.");
                return;
            }
        }
        launchCooldowns.put(player.getUniqueId(), Instant.now());
        player.setVelocity(player.getLocation().getDirection().multiply(0.6).setY(1.8));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 12, 1));
        playConfiguredSound(player, "modules.launch-pads.launch-sound", Sound.ENTITY_ENDER_DRAGON_GROWL);
    }

    private void activateOrbitalPass(org.bukkit.entity.Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.orbital-stations.enabled", true)) {
            player.sendMessage("Orbital stations module is disabled.");
            return;
        }
        Location dock = getDockLocation(player.getWorld(), player.getUniqueId());
        player.teleport(dock.clone().add(0.5, 1.0, 0.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 12, 1));
        player.sendMessage("Docking pass accepted. Welcome to the orbital station.");
        consumeItem(player, item);
    }

    private void activateRouteChart(org.bukkit.entity.Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.planet-routes.enabled", true)) {
            player.sendMessage("Planet routes module is disabled.");
            return;
        }
        int routeLimit = plugin.getConfig().getInt("modules.planet-routes.route-limit", 12);
        int currentUses = routeUses.getOrDefault(player.getUniqueId(), 0);
        if (currentUses >= routeLimit) {
            player.sendMessage("Route charts are offline. Please wait for the next cycle.");
            return;
        }
        int windowHours = plugin.getConfig().getInt("modules.planet-routes.default-window-hours", 24);
        Instant nextWindow = routeCooldowns.get(player.getUniqueId());
        if (nextWindow != null && Instant.now().isBefore(nextWindow)) {
            long minutes = Duration.between(Instant.now(), nextWindow).toMinutes();
            player.sendMessage("Next travel window opens in " + minutes + " minutes.");
            return;
        }
        routeUses.put(player.getUniqueId(), currentUses + 1);
        routeCooldowns.put(player.getUniqueId(), Instant.now().plus(Duration.ofHours(windowHours)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 30, 0));
        player.sendMessage("Route chart calibrated. Travel window cleared.");
        consumeItem(player, item);
    }

    private void activateSpaceSuit(org.bukkit.entity.Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.space-suits.enabled", true)) {
            player.sendMessage("Space suits module is disabled.");
            return;
        }
        int oxygenSeconds = plugin.getConfig().getInt("modules.space-suits.oxygen-seconds", 300);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * oxygenSeconds, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * oxygenSeconds, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * oxygenSeconds, 0));
        player.sendMessage("Space suit systems online. Oxygen stabilised for " + oxygenSeconds + " seconds.");
        consumeItem(player, item);
    }

    private boolean handleSuitRepair(PlayerInteractEvent event, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.space-suits.enabled", true)) {
            return false;
        }
        String repairItem = plugin.getConfig().getString("modules.space-suits.repair-item", "minecraft:iron_ingot");
        Material material = Material.matchMaterial(repairItem);
        if (material == null || item.getType() != material) {
            return false;
        }
        if (!event.getPlayer().hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
            return false;
        }
        event.setCancelled(true);
        int oxygenSeconds = plugin.getConfig().getInt("modules.space-suits.oxygen-seconds", 300);
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * oxygenSeconds, 0));
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * oxygenSeconds, 0));
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * oxygenSeconds, 0));
        event.getPlayer().sendMessage("Space suit repairs complete. Oxygen refreshed.");
        consumeItem(event.getPlayer(), item);
        return true;
    }

    private void activateAsteroidDrill(org.bukkit.entity.Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.asteroid-mining.enabled", true)) {
            player.sendMessage("Asteroid mining module is disabled.");
            return;
        }
        Material reward = ASTEROID_ORES.get(random.nextInt(ASTEROID_ORES.size()));
        player.getInventory().addItem(new ItemStack(reward, 2));
        double bonusChance = plugin.getConfig().getDouble("modules.asteroid-mining.bonus-ore-chance", 0.35);
        if (random.nextDouble() < bonusChance) {
            player.getInventory().addItem(new ItemStack(reward, 1));
            player.sendMessage("Bonus yield secured from the asteroid.");
        }
        double hazardRate = plugin.getConfig().getDouble("modules.asteroid-mining.hazard-rate", 0.15);
        if (random.nextDouble() < hazardRate) {
            player.damage(4.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 5, 0));
            player.sendMessage("Asteroid debris destabilised your drill!");
        }
        player.sendMessage("Asteroid drill recovered " + reward.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        consumeItem(player, item);
    }

    private boolean isLaunchPad(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tileState) {
            return tileState.getPersistentDataContainer().has(launchPadKey, PersistentDataType.BYTE);
        }
        return false;
    }

    private Location getDockLocation(World world, UUID playerId) {
        String worldKey = world.getName().toLowerCase(Locale.ROOT);
        List<Location> docks = dockLocations.computeIfAbsent(worldKey, key -> buildDockLocations(world));
        int index = Math.floorMod(playerId.hashCode(), docks.size());
        Location dock = docks.get(index);
        boolean airlocksRequired = plugin.getConfig().getBoolean("modules.orbital-stations.airlocks-required", true);
        buildDockPlatform(dock, airlocksRequired);
        return dock;
    }

    private List<Location> buildDockLocations(World world) {
        int maxDocks = Math.max(1, plugin.getConfig().getInt("modules.orbital-stations.max-docks", 6));
        List<Location> docks = new ArrayList<>();
        Location base = world.getSpawnLocation().clone();
        double y = Math.max(base.getY(), 200);
        base.setY(y);
        for (int i = 0; i < maxDocks; i++) {
            double angle = (2 * Math.PI / maxDocks) * i;
            double x = base.getX() + Math.cos(angle) * 8;
            double z = base.getZ() + Math.sin(angle) * 8;
            docks.add(new Location(world, x, y, z));
        }
        return docks;
    }

    private void buildDockPlatform(Location location, boolean airlocksRequired) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = world.getBlockAt(baseX + x, baseY, baseZ + z);
                if (block.getType().isAir()) {
                    block.setType(Material.SMOOTH_QUARTZ);
                }
            }
        }
        Block beacon = world.getBlockAt(baseX, baseY + 1, baseZ);
        if (beacon.getType().isAir()) {
            beacon.setType(Material.SEA_LANTERN);
        }
        if (airlocksRequired) {
            Block airlock = world.getBlockAt(baseX, baseY + 1, baseZ - 2);
            if (airlock.getType().isAir()) {
                airlock.setType(Material.IRON_BARS);
            }
        }
    }

    private void consumeItem(org.bukkit.entity.Player player, ItemStack item) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }

    private void playConfiguredSound(org.bukkit.entity.Player player, String path, Sound fallback) {
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
