package uk.co.xfour.furniture;

import org.bukkit.Material;

/**
 * Describes a craftable furniture item.
 */
public record FurnitureItemDefinition(String key, String displayName, Material material, FurnitureModuleType module) {
}
