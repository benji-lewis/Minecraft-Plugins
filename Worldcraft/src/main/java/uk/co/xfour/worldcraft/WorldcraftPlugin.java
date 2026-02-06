package uk.co.xfour.worldcraft;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.worldcraft.modules.CountriesModule;
import uk.co.xfour.worldcraft.modules.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the Worldcraft plugin.
 */
public class WorldcraftPlugin extends JavaPlugin {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerModules();
    }

    @Override
    public void onDisable() {
        modules.forEach(Module::stop);
        modules.clear();
    }

    private void registerModules() {
        if (getConfig().getBoolean("modules.countries.enabled", true)) {
            CountriesModule countriesModule = new CountriesModule(this);
            modules.add(countriesModule);
            countriesModule.start();
            PluginCommand command = getCommand("worldcraft");
            if (command != null) {
                command.setExecutor(countriesModule);
                command.setTabCompleter(countriesModule);
            }
        }
    }
}
