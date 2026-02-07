package uk.co.xfour.furniture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles furniture item placement.
 */
public final class FurnitureListener implements Listener {
    private final JavaPlugin plugin;
    private final FurnitureItemFactory itemFactory;

    /**
     * Creates a new furniture listener.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public FurnitureListener(JavaPlugin plugin, FurnitureItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        String itemKey = itemFactory.getItemKey(item);
        if (itemKey == null) {
            return;
        }
        String moduleKey = itemFactory.getModuleKey(item);
        if (moduleKey == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("modules." + moduleKey + ".enabled", true)) {
            event.getPlayer().sendMessage("That furniture module is disabled.");
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Location location = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        if (location.getBlock().getType() != Material.AIR) {
            event.getPlayer().sendMessage("You need empty space to place furniture.");
            return;
        }
        event.setCancelled(true);
        placeFurniture(event.getPlayer(), location, item);
    }

    private void placeFurniture(Player player, Location location, ItemStack item) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName(item.getItemMeta() != null ? item.getItemMeta().getDisplayName() : "Furniture");
        stand.setCustomNameVisible(true);
        stand.getEquipment().setHelmet(new ItemStack(item.getType()));

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
