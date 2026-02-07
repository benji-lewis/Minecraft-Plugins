package uk.co.xfour.transport.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.transport.TransportTuning;

/**
 * Controls train routing and vehicle settings.
 */
public final class TrainsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a trains module instance.
     *
     * @param plugin the owning plugin
     */
    public TrainsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.trains.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Trains module disabled via config.");
            return;
        }
        int maxCars = config.getInt("modules.trains.max-cars", 8);
        double defaultSpeed = TransportTuning.clampSpeed(config.getDouble("modules.trains.default-speed", 0.8));
        String whistleSound = config.getString("modules.trains.whistle-sound", "minecraft:entity.villager.yes");
        plugin.getLogger().info("Trains module enabled with max cars: " + maxCars + ", speed: " + defaultSpeed
                + ", whistle: " + whistleSound + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Trains module stopped.");
        }
    }
}
