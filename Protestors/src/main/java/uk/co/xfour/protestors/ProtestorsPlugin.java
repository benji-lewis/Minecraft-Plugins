package uk.co.xfour.protestors;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.protestors.modules.AsbestosHazardModule;
import uk.co.xfour.protestors.modules.DebtCollectorModule;
import uk.co.xfour.protestors.modules.JustStopOilModule;
import uk.co.xfour.protestors.modules.KimJongUnModule;
import uk.co.xfour.protestors.modules.Module;
import uk.co.xfour.protestors.modules.PetaVolunteersModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Monoplugin that hosts configurable protest-themed gameplay modules.
 */
public class ProtestorsPlugin extends JavaPlugin {
    private final List<Module> enabledModules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startModule("modules.just-stop-oil.enabled", new JustStopOilModule(this));
        startModule("modules.peta-volunteers.enabled", new PetaVolunteersModule(this));
        startModule("modules.debt-collector.enabled", new DebtCollectorModule(this));
        startModule("modules.asbestos-hazard.enabled", new AsbestosHazardModule(this));
        startModule("modules.kim-jong-un.enabled", new KimJongUnModule(this));
        getLogger().info("Enabled " + enabledModules.size() + " Protestors modules.");
    }

    @Override
    public void onDisable() {
        for (Module module : enabledModules) {
            module.stop();
            if (module instanceof Listener listener) {
                HandlerList.unregisterAll(listener);
            }
        }
        enabledModules.clear();
    }

    private void startModule(String configPath, Module module) {
        if (!getConfig().getBoolean(configPath, true)) {
            return;
        }
        module.start();
        enabledModules.add(module);
        if (module instanceof Listener listener) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
