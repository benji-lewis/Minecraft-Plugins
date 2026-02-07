package uk.co.xfour.transport.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.transport.TransportTuning;

/**
 * Controls car handling and fuel settings.
 */
public final class CarsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a cars module instance.
     *
     * @param plugin the owning plugin
     */
    public CarsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.cars.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Cars module disabled via config.");
            return;
        }
        double maxSpeed = TransportTuning.clampSpeed(config.getDouble("modules.cars.max-speed", 1.2));
        int defaultFuel = config.getInt("modules.cars.default-fuel", 100);
        String hornSound = config.getString("modules.cars.horn-sound", "minecraft:block.note_block.pling");
        plugin.getLogger().info("Cars module enabled with speed: " + maxSpeed + ", fuel: " + defaultFuel
                + ", horn: " + hornSound + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Cars module stopped.");
        }
    }
}
