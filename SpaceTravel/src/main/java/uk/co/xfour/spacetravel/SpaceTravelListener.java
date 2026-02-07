package uk.co.xfour.spacetravel;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        SpaceTravelItemType type = itemFactory.getType(item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        switch (type) {
            case LAUNCH_PAD -> activateLaunchPad(player, item);
            case ORBITAL_PASS -> activateOrbitalPass(player, item);
            case ROUTE_CHART -> activateRouteChart(player, item);
            case SPACE_SUIT_MODULE -> activateSpaceSuit(player, item);
            case ASTEROID_DRILL -> activateAsteroidDrill(player, item);
            default -> {
            }
        }
    }

    private void activateLaunchPad(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.launch-pads.enabled", true)) {
            player.sendMessage("Launch pads module is disabled.");
            return;
        }
        player.setVelocity(player.getLocation().getDirection().multiply(0.5).setY(1.8));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        consumeItem(player, item);
    }

    private void activateOrbitalPass(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.orbital-stations.enabled", true)) {
            player.sendMessage("Orbital stations module is disabled.");
            return;
        }
        World world = player.getWorld();
        Location target = world.getSpawnLocation().clone();
        target.setY(Math.max(target.getY(), 200));
        player.teleport(target);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        consumeItem(player, item);
    }

    private void activateRouteChart(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.planet-routes.enabled", true)) {
            player.sendMessage("Planet routes module is disabled.");
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1));
        player.sendMessage("Route chart calibrated. You're cleared for the next travel window.");
        consumeItem(player, item);
    }

    private void activateSpaceSuit(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.space-suits.enabled", true)) {
            player.sendMessage("Space suits module is disabled.");
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 300, 0));
        player.sendMessage("Space suit systems online. Oxygen stabilised.");
        consumeItem(player, item);
    }

    private void activateAsteroidDrill(Player player, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.asteroid-mining.enabled", true)) {
            player.sendMessage("Asteroid mining module is disabled.");
            return;
        }
        Material reward = ASTEROID_ORES.get(random.nextInt(ASTEROID_ORES.size()));
        player.getInventory().addItem(new ItemStack(reward, 2));
        player.sendMessage("Asteroid drill recovered " + reward.name().toLowerCase().replace('_', ' ') + ".");
        consumeItem(player, item);
    }

    private void consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }
}
