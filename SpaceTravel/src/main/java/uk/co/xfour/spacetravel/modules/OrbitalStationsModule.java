package uk.co.xfour.spacetravel.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controls orbital station capacity and safety checks.
 */
public final class OrbitalStationsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates an orbital stations module instance.
     *
     * @param plugin the owning plugin
     */
    public OrbitalStationsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.orbital-stations.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Orbital stations module disabled via config.");
            return;
        }
        int maxDocks = config.getInt("modules.orbital-stations.max-docks", 6);
        boolean airlocksRequired = config.getBoolean("modules.orbital-stations.airlocks-required", true);
        plugin.getLogger().info("Orbital stations module enabled with docks: " + maxDocks
                + ", airlocks required: " + airlocksRequired + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Orbital stations module stopped.");
        }
    }
}
