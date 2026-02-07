package uk.co.xfour.transport.modules;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controls railway networks and signal spacing.
 */
public final class RailSystemsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a rail systems module instance.
     *
     * @param plugin the owning plugin
     */
    public RailSystemsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.rail-systems.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Rail systems module disabled via config.");
            return;
        }
        List<String> railTypes = config.getStringList("modules.rail-systems.rail-types");
        int signalInterval = config.getInt("modules.rail-systems.signal-interval", 24);
        plugin.getLogger().info("Rail systems module enabled with rail types: " + railTypes
                + ", signal interval: " + signalInterval + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Rail systems module stopped.");
        }
    }
}
