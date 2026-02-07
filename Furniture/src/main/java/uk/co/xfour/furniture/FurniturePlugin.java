package uk.co.xfour.furniture;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.modules.BathroomModule;
import uk.co.xfour.furniture.modules.BedroomModule;
import uk.co.xfour.furniture.modules.DecorModule;
import uk.co.xfour.furniture.modules.KitchenModule;
import uk.co.xfour.furniture.modules.LightingModule;
import uk.co.xfour.furniture.modules.Module;
import uk.co.xfour.furniture.modules.OutdoorModule;
import uk.co.xfour.furniture.modules.SeatingModule;
import uk.co.xfour.furniture.modules.StorageModule;
import uk.co.xfour.furniture.modules.TablesModule;

/**
 * Main plugin entrypoint for Furniture.
 */
public final class FurniturePlugin extends JavaPlugin {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FurnitureItemFactory itemFactory = new FurnitureItemFactory(this);
        new FurnitureRecipeRegistry(this, itemFactory).registerRecipes(itemFactory.loadDefinitions());
        getServer().getPluginManager().registerEvents(new FurnitureListener(this, itemFactory), this);

        modules.add(new SeatingModule(this));
        modules.add(new TablesModule(this));
        modules.add(new StorageModule(this));
        modules.add(new LightingModule(this));
        modules.add(new DecorModule(this));
        modules.add(new OutdoorModule(this));
        modules.add(new KitchenModule(this));
        modules.add(new BedroomModule(this));
        modules.add(new BathroomModule(this));

        PluginCommand command = getCommand("furniture");
        if (command != null) {
            FurnitureCommand furnitureCommand = new FurnitureCommand(this, modules, itemFactory);
            command.setExecutor(furnitureCommand);
            command.setTabCompleter(furnitureCommand);
        }

        for (Module module : modules) {
            module.start();
        }

        getLogger().info("Furniture modules initialised.");
    }

    @Override
    public void onDisable() {
        for (Module module : modules) {
            module.stop();
        }
        getLogger().info("Furniture modules stopped.");
    }
}
