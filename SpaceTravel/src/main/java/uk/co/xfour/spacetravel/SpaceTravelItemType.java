package uk.co.xfour.spacetravel;

import org.bukkit.Material;

/**
 * Defines craftable space travel items.
 */
public enum SpaceTravelItemType {
    LAUNCH_PAD("launch-pad", "Launch Pad Beacon", Material.LODESTONE),
    ORBITAL_PASS("orbital-pass", "Orbital Docking Pass", Material.ENDER_PEARL),
    ROUTE_CHART("route-chart", "Planet Route Chart", Material.MAP),
    SPACE_SUIT_MODULE("space-suit", "Space Suit Module", Material.GLASS),
    ASTEROID_DRILL("asteroid-drill", "Asteroid Drill", Material.DIAMOND_PICKAXE);

    private final String key;
    private final String displayName;
    private final Material material;

    SpaceTravelItemType(String key, String displayName, Material material) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
    }

    /**
     * Returns the persistent key value.
     *
     * @return the key value
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the base material.
     *
     * @return the material
     */
    public Material getMaterial() {
        return material;
    }
}
