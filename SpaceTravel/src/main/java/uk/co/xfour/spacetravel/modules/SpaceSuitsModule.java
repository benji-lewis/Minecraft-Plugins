package uk.co.xfour.spacetravel.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controls space suit oxygen and repair items.
 */
public final class SpaceSuitsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a space suits module instance.
     *
     * @param plugin the owning plugin
     */
    public SpaceSuitsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.space-suits.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Space suits module disabled via config.");
            return;
        }
        int oxygenSeconds = config.getInt("modules.space-suits.oxygen-seconds", 300);
        String repairItem = config.getString("modules.space-suits.repair-item", "minecraft:iron_ingot");
        plugin.getLogger().info("Space suits module enabled with oxygen: " + oxygenSeconds
                + "s, repair item: " + repairItem + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Space suits module stopped.");
        }
    }
}
