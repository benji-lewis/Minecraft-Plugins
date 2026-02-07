package uk.co.xfour.spacetravel.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.spacetravel.LaunchWindow;

/**
 * Controls planet-to-planet travel routing.
 */
public final class PlanetRoutesModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a planet routes module instance.
     *
     * @param plugin the owning plugin
     */
    public PlanetRoutesModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.planet-routes.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Planet routes module disabled via config.");
            return;
        }
        int defaultWindowHours = LaunchWindow.normaliseWindowHours(
                config.getInt("modules.planet-routes.default-window-hours", 24));
        int routeLimit = config.getInt("modules.planet-routes.route-limit", 12);
        plugin.getLogger().info("Planet routes module enabled with window: " + defaultWindowHours
                + "h, route limit: " + routeLimit + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Planet routes module stopped.");
        }
    }
}
