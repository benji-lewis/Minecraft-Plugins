package uk.co.xfour.transport;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers crafting recipes for transport items.
 */
public final class TransportRecipeRegistry {
    private final JavaPlugin plugin;
    private final TransportItemFactory itemFactory;

    /**
     * Creates a new recipe registry.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public TransportRecipeRegistry(JavaPlugin plugin, TransportItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    /**
     * Registers transport crafting recipes.
     */
    public void registerRecipes() {
        registerTrainRecipe();
        registerPlaneRecipe();
        registerCarRecipe();
        registerRailRecipe();
        registerSpaceshipRecipe();
    }

    private void registerTrainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "train-engine"),
                itemFactory.createItem(TransportItemType.TRAIN_ENGINE));
        recipe.shape("IRI", "CMC", "IRI");
        recipe.setIngredient('I', org.bukkit.Material.IRON_INGOT);
        recipe.setIngredient('R', org.bukkit.Material.REDSTONE);
        recipe.setIngredient('C', org.bukkit.Material.COAL);
        recipe.setIngredient('M', org.bukkit.Material.MINECART);
        Bukkit.addRecipe(recipe);
    }

    private void registerPlaneRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "plane-frame"),
                itemFactory.createItem(TransportItemType.PLANE_FRAME));
        recipe.shape("FEF", "PPP", " F ");
        recipe.setIngredient('F', org.bukkit.Material.FEATHER);
        recipe.setIngredient('E', org.bukkit.Material.ELYTRA);
        recipe.setIngredient('P', org.bukkit.Material.PHANTOM_MEMBRANE);
        Bukkit.addRecipe(recipe);
    }

    private void registerCarRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "car-chassis"),
                itemFactory.createItem(TransportItemType.CAR_CHASSIS));
        recipe.shape("ILI", "LGL", " I ");
        recipe.setIngredient('I', org.bukkit.Material.IRON_INGOT);
        recipe.setIngredient('L', org.bukkit.Material.LEATHER);
        recipe.setIngredient('G', org.bukkit.Material.GOLD_INGOT);
        Bukkit.addRecipe(recipe);
    }

    private void registerRailRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "rail-kit"),
                itemFactory.createItem(TransportItemType.RAIL_KIT));
        recipe.shape("IRI", "RSR", "IRI");
        recipe.setIngredient('I', org.bukkit.Material.IRON_INGOT);
        recipe.setIngredient('R', org.bukkit.Material.RAIL);
        recipe.setIngredient('S', org.bukkit.Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void registerSpaceshipRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "spaceship-core"),
                itemFactory.createItem(TransportItemType.SPACESHIP_CORE));
        recipe.shape("ENE", "NSN", "ENE");
        recipe.setIngredient('E', org.bukkit.Material.ENDER_PEARL);
        recipe.setIngredient('N', org.bukkit.Material.NETHERITE_INGOT);
        recipe.setIngredient('S', org.bukkit.Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }
}
