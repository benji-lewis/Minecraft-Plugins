package uk.co.xfour.worldcraft.flags;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Builds a flag pole structure and mounts a flag map on it.
 */
public class FlagPoleBuilder {
    private final Material poleMaterial;
    private final Material mountMaterial;
    private final int poleHeight;

    /**
     * Creates a new builder for flag poles.
     *
     * @param poleMaterial  material used for the pole
     * @param mountMaterial material used for the mount block
     * @param poleHeight    number of fence blocks in the pole
     */
    public FlagPoleBuilder(Material poleMaterial, Material mountMaterial, int poleHeight) {
        this.poleMaterial = poleMaterial;
        this.mountMaterial = mountMaterial;
        this.poleHeight = poleHeight;
    }

    /**
     * Places a flag pole at the player's location.
     *
     * @param player  player placing the pole
     * @param mapItem map item to mount on the pole
     * @return true if the pole was placed
     */
    public boolean placeFlagPole(Player player, ItemStack mapItem) {
        Location baseLocation = player.getLocation().getBlock().getLocation().add(0, 1, 0);
        World world = baseLocation.getWorld();
        if (world == null) {
            return false;
        }
        BlockFace facing = player.getFacing().getOppositeFace();
        BlockFace mountDirection = facing.getOppositeFace();
        Block baseBlock = world.getBlockAt(baseLocation);
        if (!isSpaceClear(baseBlock, mountDirection)) {
            return false;
        }
        Block poleBlock = baseBlock;
        for (int i = 0; i < poleHeight; i++) {
            poleBlock.setType(poleMaterial, false);
            poleBlock = poleBlock.getRelative(BlockFace.UP);
        }
        Block mountBlock = poleBlock.getRelative(mountDirection);
        mountBlock.setType(mountMaterial, false);
        ItemFrame itemFrame = (ItemFrame) world.spawnEntity(mountBlock.getLocation().add(0.5, 0.5, 0.5), EntityType.ITEM_FRAME);
        itemFrame.setFacingDirection(facing, true);
        itemFrame.setItem(mapItem);
        return true;
    }

    private boolean isSpaceClear(Block baseBlock, BlockFace mountDirection) {
        Block current = baseBlock;
        for (int i = 0; i < poleHeight; i++) {
            if (!current.getType().isAir()) {
                return false;
            }
            current = current.getRelative(BlockFace.UP);
        }
        Block mountBlock = current.getRelative(mountDirection);
        return mountBlock.getType().isAir();
    }
}
