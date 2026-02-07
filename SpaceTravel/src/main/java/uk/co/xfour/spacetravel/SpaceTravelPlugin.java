package uk.co.xfour.spacetravel;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.spacetravel.modules.AsteroidMiningModule;
import uk.co.xfour.spacetravel.modules.LaunchPadsModule;
import uk.co.xfour.spacetravel.modules.Module;
import uk.co.xfour.spacetravel.modules.OrbitalStationsModule;
import uk.co.xfour.spacetravel.modules.PlanetRoutesModule;
import uk.co.xfour.spacetravel.modules.SpaceSuitsModule;

/**
 * Main plugin entrypoint for SpaceTravel.
 */
public final class SpaceTravelPlugin extends JavaPlugin {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        modules.add(new LaunchPadsModule(this));
        modules.add(new OrbitalStationsModule(this));
        modules.add(new PlanetRoutesModule(this));
        modules.add(new SpaceSuitsModule(this));
        modules.add(new AsteroidMiningModule(this));

        PluginCommand command = getCommand("spacetravel");
        if (command != null) {
            SpaceTravelCommand spaceTravelCommand = new SpaceTravelCommand(this, modules);
            command.setExecutor(spaceTravelCommand);
            command.setTabCompleter(spaceTravelCommand);
        }

        for (Module module : modules) {
            module.start();
        }

        getLogger().info("SpaceTravel modules initialised.");
    }

    @Override
    public void onDisable() {
        for (Module module : modules) {
            module.stop();
        }
        getLogger().info("SpaceTravel modules stopped.");
    }
}
