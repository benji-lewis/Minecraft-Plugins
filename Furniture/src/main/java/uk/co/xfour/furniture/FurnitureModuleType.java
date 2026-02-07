package uk.co.xfour.furniture;

import java.util.Map;
import org.bukkit.Material;

/**
 * Defines furniture module crafting data.
 */
public enum FurnitureModuleType {
    SEATING("seating", Material.OAK_STAIRS, new String[] {"WWW", "PPP", " W "},
            Map.of('W', Material.WHITE_WOOL, 'P', Material.OAK_PLANKS)),
    TABLES("tables", Material.OAK_SLAB, new String[] {"PPP", " S ", " S "},
            Map.of('P', Material.OAK_PLANKS, 'S', Material.STICK)),
    STORAGE("storage", Material.BARREL, new String[] {"PPP", "PCP", "PPP"},
            Map.of('P', Material.OAK_PLANKS, 'C', Material.CHEST)),
    LIGHTING("lighting", Material.LANTERN, new String[] {" T ", "TGT", " T "},
            Map.of('T', Material.TORCH, 'G', Material.GLOWSTONE)),
    DECOR("decor", Material.FLOWER_POT, new String[] {" P ", "PFP", " P "},
            Map.of('P', Material.PAPER, 'F', Material.FLOWER_POT)),
    OUTDOOR("outdoor", Material.OAK_LOG, new String[] {"LLL", "P P", "P P"},
            Map.of('L', Material.OAK_LEAVES, 'P', Material.OAK_PLANKS)),
    KITCHEN("kitchen", Material.SMOKER, new String[] {"III", "SFS", "III"},
            Map.of('I', Material.IRON_INGOT, 'S', Material.SMOOTH_STONE, 'F', Material.FURNACE)),
    BEDROOM("bedroom", Material.WHITE_BED, new String[] {"WWW", "PPP", " P "},
            Map.of('W', Material.WHITE_WOOL, 'P', Material.OAK_PLANKS)),
    BATHROOM("bathroom", Material.QUARTZ_BLOCK, new String[] {"QQQ", "BWB", "QQQ"},
            Map.of('Q', Material.QUARTZ_BLOCK, 'B', Material.BUCKET, 'W', Material.WATER_BUCKET));

    private final String key;
    private final Material displayMaterial;
    private final String[] recipeShape;
    private final Map<Character, Material> ingredients;

    FurnitureModuleType(String key, Material displayMaterial, String[] recipeShape,
                        Map<Character, Material> ingredients) {
        this.key = key;
        this.displayMaterial = displayMaterial;
        this.recipeShape = recipeShape;
        this.ingredients = ingredients;
    }

    /**
     * Returns the config key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the display material.
     *
     * @return the material
     */
    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    /**
     * Returns the recipe shape.
     *
     * @return the recipe shape
     */
    public String[] getRecipeShape() {
        return recipeShape.clone();
    }

    /**
     * Returns the recipe ingredients.
     *
     * @return the ingredients
     */
    public Map<Character, Material> getIngredients() {
        return ingredients;
    }

    /**
     * Returns the module type for a key.
     *
     * @param key the module key
     * @return the module type or null
     */
    public static FurnitureModuleType fromKey(String key) {
        for (FurnitureModuleType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
