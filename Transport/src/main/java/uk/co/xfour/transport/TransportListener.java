package uk.co.xfour.transport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles transport item interactions.
 */
public final class TransportListener implements Listener {
    private final JavaPlugin plugin;
    private final TransportItemFactory itemFactory;

    /**
     * Creates a new transport listener.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public TransportListener(JavaPlugin plugin, TransportItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
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
        Location location = player.getLocation();
        switch (type) {
            case TRAIN_ENGINE -> spawnTrain(player, location, item);
            case PLANE_FRAME -> spawnPlane(player, location, item);
            case CAR_CHASSIS -> spawnCar(player, location, item);
            case RAIL_KIT -> placeRail(player, event, item);
            case SPACESHIP_CORE -> spawnSpaceship(player, location, item);
            default -> {
            }
        }
    }

    private void spawnTrain(Player player, Location location, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.trains.enabled", true)) {
            player.sendMessage("Trains module is disabled.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Minecart minecart = world.spawn(location, Minecart.class);
        minecart.setCustomName("Train Engine");
        mount(player, minecart);
        consumeItem(player, item);
    }

    private void spawnPlane(Player player, Location location, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.planes.enabled", true)) {
            player.sendMessage("Planes module is disabled.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Boat boat = world.spawn(location, Boat.class);
        boat.setCustomName("Plane Frame");
        boat.setGravity(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        mount(player, boat);
        consumeItem(player, item);
    }

    private void spawnCar(Player player, Location location, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.cars.enabled", true)) {
            player.sendMessage("Cars module is disabled.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Pig pig = world.spawn(location, Pig.class);
        pig.setSaddle(true);
        pig.setCustomName("Car Chassis");
        mount(player, pig);
        consumeItem(player, item);
    }

    private void spawnSpaceship(Player player, Location location, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.spaceships.enabled", true)) {
            player.sendMessage("Spaceships module is disabled.");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Boat boat = world.spawn(location, Boat.class);
        boat.setCustomName("Spaceship Core");
        boat.setGravity(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        mount(player, boat);
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
        target.getBlock().setType(Material.RAIL);
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
}
