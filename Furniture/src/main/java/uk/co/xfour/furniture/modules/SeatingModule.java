package uk.co.xfour.furniture.modules;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.FurnitureRegistry;

/**
 * Controls seating furniture definitions.
 */
public final class SeatingModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a seating module instance.
     *
     * @param plugin the owning plugin
     */
    public SeatingModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.seating.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Seating module disabled via config.");
            return;
        }
        List<String> items = config.getStringList("modules.seating.items");
        int limit = FurnitureRegistry.normaliseLimit(config.getInt("modules.tuning.max-items-per-module", 24));
        int count = Math.min(items.size(), limit);
        plugin.getLogger().info("Seating module enabled with " + count + " items (limit " + limit + ").");
        if (items.size() > limit) {
            plugin.getLogger().warning("Seating module has more items than the configured limit.");
        }
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Seating module stopped.");
        }
    }
}
