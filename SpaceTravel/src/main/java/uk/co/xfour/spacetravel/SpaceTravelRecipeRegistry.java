package uk.co.xfour.spacetravel;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers crafting recipes for space travel items.
 */
public final class SpaceTravelRecipeRegistry {
    private final JavaPlugin plugin;
    private final SpaceTravelItemFactory itemFactory;

    /**
     * Creates a new recipe registry.
     *
     * @param plugin the owning plugin
     * @param itemFactory the item factory
     */
    public SpaceTravelRecipeRegistry(JavaPlugin plugin, SpaceTravelItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    /**
     * Registers space travel crafting recipes.
     */
    public void registerRecipes() {
        registerLaunchPadRecipe();
        registerOrbitalPassRecipe();
        registerRouteChartRecipe();
        registerSpaceSuitRecipe();
        registerAsteroidDrillRecipe();
    }

    private void registerLaunchPadRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "launch-pad"),
                itemFactory.createItem(SpaceTravelItemType.LAUNCH_PAD));
        recipe.shape("IFI", "ECE", "IFI");
        recipe.setIngredient('I', org.bukkit.Material.IRON_BLOCK);
        recipe.setIngredient('F', org.bukkit.Material.FIRE_CHARGE);
        recipe.setIngredient('E', org.bukkit.Material.ENDER_PEARL);
        recipe.setIngredient('C', org.bukkit.Material.COPPER_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void registerOrbitalPassRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "orbital-pass"),
                itemFactory.createItem(SpaceTravelItemType.ORBITAL_PASS));
        recipe.shape("GEG", "ESE", "GEG");
        recipe.setIngredient('G', org.bukkit.Material.GOLD_INGOT);
        recipe.setIngredient('E', org.bukkit.Material.ENDER_EYE);
        recipe.setIngredient('S', org.bukkit.Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }

    private void registerRouteChartRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "route-chart"),
                itemFactory.createItem(SpaceTravelItemType.ROUTE_CHART));
        recipe.shape("PPP", "MEM", "PPP");
        recipe.setIngredient('P', org.bukkit.Material.PAPER);
        recipe.setIngredient('M', org.bukkit.Material.MAP);
        recipe.setIngredient('E', org.bukkit.Material.ENDER_PEARL);
        Bukkit.addRecipe(recipe);
    }

    private void registerSpaceSuitRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "space-suit"),
                itemFactory.createItem(SpaceTravelItemType.SPACE_SUIT_MODULE));
        recipe.shape("GGG", "DID", "GGG");
        recipe.setIngredient('G', org.bukkit.Material.GLASS_PANE);
        recipe.setIngredient('D', org.bukkit.Material.DIAMOND);
        recipe.setIngredient('I', org.bukkit.Material.IRON_CHESTPLATE);
        Bukkit.addRecipe(recipe);
    }

    private void registerAsteroidDrillRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "asteroid-drill"),
                itemFactory.createItem(SpaceTravelItemType.ASTEROID_DRILL));
        recipe.shape("DED", "DPD", " S ");
        recipe.setIngredient('D', org.bukkit.Material.DIAMOND);
        recipe.setIngredient('E', org.bukkit.Material.EMERALD);
        recipe.setIngredient('P', org.bukkit.Material.DIAMOND_PICKAXE);
        recipe.setIngredient('S', org.bukkit.Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(recipe);
    }
}
