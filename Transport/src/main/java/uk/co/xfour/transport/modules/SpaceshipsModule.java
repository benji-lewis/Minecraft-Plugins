package uk.co.xfour.transport.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.transport.TransportTuning;

/**
 * Controls spaceship travel and optional SpaceTravel integration.
 */
public final class SpaceshipsModule implements Module {
    private final JavaPlugin plugin;
    private final boolean spaceTravelAvailable;
    private boolean enabled;

    /**
     * Creates a spaceships module instance.
     *
     * @param plugin the owning plugin
     * @param spaceTravelAvailable whether the SpaceTravel plugin is loaded
     */
    public SpaceshipsModule(JavaPlugin plugin, boolean spaceTravelAvailable) {
        this.plugin = plugin;
        this.spaceTravelAvailable = spaceTravelAvailable;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.spaceships.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Spaceships module disabled via config.");
            return;
        }
        double maxSpeed = TransportTuning.clampSpeed(config.getDouble("modules.spaceships.max-speed", 3.2));
        boolean autoLaunch = config.getBoolean("modules.spaceships.auto-launch", true);
        boolean integrationEnabled = config.getBoolean("modules.spaceships.integration.enable-space-travel", true)
                && spaceTravelAvailable;
        boolean autoDock = config.getBoolean("modules.spaceships.integration.auto-dock", true);
        plugin.getLogger().info("Spaceships module enabled with speed: " + maxSpeed + ", auto launch: "
                + autoLaunch + ".");
        if (integrationEnabled) {
            plugin.getLogger().info("SpaceTravel integration active. Auto dock: " + autoDock + ".");
        } else if (!spaceTravelAvailable) {
            plugin.getLogger().info("SpaceTravel plugin not detected; spaceship integration disabled.");
        }
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Spaceships module stopped.");
        }
    }
}
