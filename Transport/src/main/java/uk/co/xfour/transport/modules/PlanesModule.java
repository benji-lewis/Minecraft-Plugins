package uk.co.xfour.transport.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.transport.TransportTuning;

/**
 * Controls plane flight behaviour and runway tuning.
 */
public final class PlanesModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a planes module instance.
     *
     * @param plugin the owning plugin
     */
    public PlanesModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.planes.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Planes module disabled via config.");
            return;
        }
        int cruiseAltitude = config.getInt("modules.planes.cruise-altitude", 120);
        double maxSpeed = TransportTuning.clampSpeed(config.getDouble("modules.planes.max-speed", 1.6));
        int runwayLength = config.getInt("modules.planes.runway-length", 64);
        plugin.getLogger().info("Planes module enabled with altitude: " + cruiseAltitude + ", speed: " + maxSpeed
                + ", runway length: " + runwayLength + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Planes module stopped.");
        }
    }
}
