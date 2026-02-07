package uk.co.xfour.furniture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles furniture item placement.
 */
public final class FurnitureListener implements Listener {
    private final JavaPlugin plugin;
    private final FurnitureItemFactory itemFactory;
    private final NamespacedKey placedKey;
    private final NamespacedKey moduleKey;

    /**
     * Creates a new furniture listener.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public FurnitureListener(JavaPlugin plugin, FurnitureItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.placedKey = new NamespacedKey(plugin, "furniture-placed");
        this.moduleKey = new NamespacedKey(plugin, "furniture-module");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
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
        event.setCancelled(true);
        FurnitureItemDefinition definition = itemFactory.getDefinition(itemKey);
        if (definition == null) {
            event.getPlayer().sendMessage("This furniture item is missing definition data.");
            return;
        }
        placeFurniture(event.getPlayer(), event.getClickedBlock(), event.getBlockFace(), item, definition);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        String itemKey = container.get(blockKey(block), PersistentDataType.STRING);
        if (itemKey == null) {
            return;
        }
        event.setDropItems(false);
        container.remove(blockKey(block));
        block.setType(Material.AIR);
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            dropFurnitureItem(block.getLocation(), itemKey);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event instanceof HangingBreakByEntityEvent breakEvent)) {
            return;
        }
        if (!(breakEvent.getRemover() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }
        PersistentDataContainer container = frame.getPersistentDataContainer();
        String itemKey = container.get(placedKey, PersistentDataType.STRING);
        if (itemKey == null) {
            return;
        }
        event.setCancelled(true);
        frame.remove();
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            dropFurnitureItem(frame.getLocation(), itemKey);
        }
    }

    private void placeFurniture(Player player, Block clickedBlock, org.bukkit.block.BlockFace face,
                                ItemStack item, FurnitureItemDefinition definition) {
        if (definition.placementStyle() == FurniturePlacementStyle.WALL_DECOR) {
            placeWallDecor(player, clickedBlock, face, item, definition);
            return;
        }
        Block target = clickedBlock.getRelative(face);
        if (target.getType() != Material.AIR) {
            player.sendMessage("You need empty space to place furniture.");
            return;
        }
        target.setType(definition.placedMaterial());
        applyBlockData(target, player);
        PersistentDataContainer container = target.getChunk().getPersistentDataContainer();
        container.set(blockKey(target), PersistentDataType.STRING, definition.key());

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void placeWallDecor(Player player, Block clickedBlock, org.bukkit.block.BlockFace face, ItemStack item,
                                FurnitureItemDefinition definition) {
        if (face == org.bukkit.block.BlockFace.UP || face == org.bukkit.block.BlockFace.DOWN) {
            player.sendMessage("Wall decor must be placed on the side of a block.");
            return;
        }
        Location location = clickedBlock.getLocation();
        ItemFrame frame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
        frame.setFacingDirection(face, true);
        frame.setVisible(false);
        frame.setItem(new ItemStack(definition.placedMaterial()));
        frame.getPersistentDataContainer().set(placedKey, PersistentDataType.STRING, definition.key());
        frame.getPersistentDataContainer().set(moduleKey, PersistentDataType.STRING, definition.module().getKey());
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void applyBlockData(Block block, Player player) {
        BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(player.getFacing());
            data = directional;
        }
        if (data instanceof Rotatable rotatable) {
            rotatable.setRotation(player.getFacing());
            data = rotatable;
        }
        if (data instanceof Stairs stairs) {
            stairs.setFacing(player.getFacing());
            stairs.setHalf(Stairs.Half.BOTTOM);
            data = stairs;
        }
        if (data instanceof Slab slab) {
            slab.setType(Slab.Type.BOTTOM);
            data = slab;
        }
        block.setBlockData(data);
    }

    private void dropFurnitureItem(Location location, String itemKey) {
        FurnitureItemDefinition definition = itemFactory.getDefinition(itemKey);
        if (definition == null || location.getWorld() == null) {
            return;
        }
        ItemStack stack = itemFactory.createItem(definition);
        location.getWorld().dropItemNaturally(location, stack);
    }

    private NamespacedKey blockKey(Block block) {
        return new NamespacedKey(plugin, "furniture-block-" + block.getX() + "-" + block.getY() + "-" + block.getZ());
    }
}
