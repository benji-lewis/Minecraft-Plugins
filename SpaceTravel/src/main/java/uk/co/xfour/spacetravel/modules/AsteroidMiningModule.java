package uk.co.xfour.spacetravel.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controls asteroid mining rewards and hazards.
 */
public final class AsteroidMiningModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates an asteroid mining module instance.
     *
     * @param plugin the owning plugin
     */
    public AsteroidMiningModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.asteroid-mining.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Asteroid mining module disabled via config.");
            return;
        }
        double bonusOreChance = config.getDouble("modules.asteroid-mining.bonus-ore-chance", 0.35);
        double hazardRate = config.getDouble("modules.asteroid-mining.hazard-rate", 0.15);
        plugin.getLogger().info("Asteroid mining module enabled with bonus chance: " + bonusOreChance
                + ", hazard rate: " + hazardRate + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Asteroid mining module stopped.");
        }
    }
}
