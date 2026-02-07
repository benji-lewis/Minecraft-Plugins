package uk.co.xfour.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.transport.modules.CarsModule;
import uk.co.xfour.transport.modules.Module;
import uk.co.xfour.transport.modules.PlanesModule;
import uk.co.xfour.transport.modules.RailSystemsModule;
import uk.co.xfour.transport.modules.SpaceshipsModule;
import uk.co.xfour.transport.modules.TrainsModule;

/**
 * Main plugin entrypoint for Transport.
 */
public final class TransportPlugin extends JavaPlugin {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Logger logger = getLogger();

        boolean spaceTravelAvailable = Bukkit.getPluginManager().isPluginEnabled("SpaceTravel");

        modules.add(new TrainsModule(this));
        modules.add(new PlanesModule(this));
        modules.add(new CarsModule(this));
        modules.add(new RailSystemsModule(this));
        modules.add(new SpaceshipsModule(this, spaceTravelAvailable));

        PluginCommand command = getCommand("transport");
        if (command != null) {
            TransportCommand transportCommand = new TransportCommand(this, modules);
            command.setExecutor(transportCommand);
            command.setTabCompleter(transportCommand);
        }

        for (Module module : modules) {
            module.start();
        }

        logger.info("Transport modules initialised.");
    }

    @Override
    public void onDisable() {
        for (Module module : modules) {
            module.stop();
        }
        getLogger().info("Transport modules stopped.");
    }
}
