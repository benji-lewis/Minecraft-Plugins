package uk.co.xfour.furniture;

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers crafting recipes for furniture items.
 */
public final class FurnitureRecipeRegistry {
    private final JavaPlugin plugin;
    private final FurnitureItemFactory itemFactory;

    /**
     * Creates a new recipe registry.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public FurnitureRecipeRegistry(JavaPlugin plugin, FurnitureItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    /**
     * Registers furniture crafting recipes.
     *
     * @param definitions the furniture definitions
     */
    public void registerRecipes(List<FurnitureItemDefinition> definitions) {
        for (FurnitureItemDefinition definition : definitions) {
            registerRecipe(definition);
        }
    }

    private void registerRecipe(FurnitureItemDefinition definition) {
        FurnitureModuleType module = definition.module();
        NamespacedKey key = new NamespacedKey(plugin,
                "furniture-" + module.getKey() + "-" + definition.key());
        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createItem(definition));
        recipe.shape(module.getRecipeShape());
        for (Map.Entry<Character, org.bukkit.Material> entry : module.getIngredients().entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        Bukkit.addRecipe(recipe);
    }
}
