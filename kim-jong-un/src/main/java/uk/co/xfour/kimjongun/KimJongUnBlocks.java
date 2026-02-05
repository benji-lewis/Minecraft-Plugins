package uk.co.xfour.kimjongun;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Provides helpers for interacting with Kim Jong Un placed blocks.
 */
public class KimJongUnBlocks {
    private static final Material LAUNCHPAD_BLOCK = Material.RESPAWN_ANCHOR;
    private static final Material MISSILE_BLOCK = Material.IRON_BLOCK;
    private static final Material ICBM_BLOCK = Material.NETHERITE_BLOCK;

    public boolean isLaunchpad(Location location) {
        return blockTypeAt(location) == LAUNCHPAD_BLOCK;
    }

    public boolean isMissile(Location location) {
        return blockTypeAt(location) == MISSILE_BLOCK;
    }

    public boolean isIcbm(Location location) {
        return blockTypeAt(location) == ICBM_BLOCK;
    }

    public boolean placeLaunchpad(Location location, Object source) {
        return place(location, LAUNCHPAD_BLOCK);
    }

    /**
     * Attempts to place a missile block at the given location.
     *
     * @param location the target location
     * @param source the placement source (player or plugin)
     * @return true when the block is placed successfully
     */
    public boolean placeMissile(Location location, Object source) {
        return place(location, MISSILE_BLOCK);
    }

    /**
     * Attempts to place an ICBM block at the given location.
     *
     * @param location the target location
     * @param source the placement source (player or plugin)
     * @return true when the block is placed successfully
     */
    public boolean placeIcbm(Location location, Object source) {
        return place(location, ICBM_BLOCK);
    }

    /**
     * Checks whether any Kim Jong Un placed block is present at the given location.
     *
     * @param location the location to inspect
     * @return true if a plugin block is present
     */
    public boolean hasPlacedBlock(Location location) {
        Material type = blockTypeAt(location);
        return type == LAUNCHPAD_BLOCK || type == MISSILE_BLOCK || type == ICBM_BLOCK;
    }

    private boolean place(Location location, Material material) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Block block = location.getBlock();
        if (!block.isReplaceable()) {
            return false;
        }
        block.setType(material, false);
        return true;
    }

    private Material blockTypeAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Material.AIR;
        }
        return location.getBlock().getType();
    }
}
