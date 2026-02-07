package uk.co.xfour.furniture;

/**
 * Describes a craftable furniture item.
 */
public record FurnitureItemDefinition(
        String key,
        String displayName,
        org.bukkit.Material itemMaterial,
        org.bukkit.Material placedMaterial,
        FurniturePlacementStyle placementStyle,
        FurnitureModuleType module) {
}
