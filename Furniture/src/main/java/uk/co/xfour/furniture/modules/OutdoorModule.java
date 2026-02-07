package uk.co.xfour.furniture.modules;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.FurnitureRegistry;

/**
 * Controls outdoor furniture definitions.
 */
public final class OutdoorModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates an outdoor module instance.
     *
     * @param plugin the owning plugin
     */
    public OutdoorModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.outdoor.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Outdoor module disabled via config.");
            return;
        }
        List<String> items = config.getStringList("modules.outdoor.items");
        int limit = FurnitureRegistry.normaliseLimit(config.getInt("modules.tuning.max-items-per-module", 24));
        int count = Math.min(items.size(), limit);
        plugin.getLogger().info("Outdoor module enabled with " + count + " items (limit " + limit + ").");
        if (items.size() > limit) {
            plugin.getLogger().warning("Outdoor module has more items than the configured limit.");
        }
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Outdoor module stopped.");
        }
    }
}
