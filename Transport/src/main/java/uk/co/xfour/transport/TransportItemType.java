package uk.co.xfour.transport;

import org.bukkit.Material;

/**
 * Defines the craftable transport item types.
 */
public enum TransportItemType {
    TRAIN_ENGINE("train-engine", "Train Engine", Material.MINECART),
    PLANE_FRAME("plane-frame", "Plane Frame", Material.OAK_BOAT),
    CAR_CHASSIS("car-chassis", "Car Chassis", Material.SADDLE),
    RAIL_KIT("rail-kit", "Rail Kit", Material.RAIL),
    SPACESHIP_CORE("spaceship-core", "Spaceship Core", Material.ENDER_EYE);

    private final String key;
    private final String displayName;
    private final Material material;

    TransportItemType(String key, String displayName, Material material) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
    }

    /**
     * Returns the persistent key value for this item.
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
