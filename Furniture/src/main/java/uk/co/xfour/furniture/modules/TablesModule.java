package uk.co.xfour.furniture.modules;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.FurnitureRegistry;

/**
 * Controls table furniture definitions.
 */
public final class TablesModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a tables module instance.
     *
     * @param plugin the owning plugin
     */
    public TablesModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.tables.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Tables module disabled via config.");
            return;
        }
        List<String> items = config.getStringList("modules.tables.items");
        int limit = FurnitureRegistry.normaliseLimit(config.getInt("modules.tuning.max-items-per-module", 24));
        int count = Math.min(items.size(), limit);
        plugin.getLogger().info("Tables module enabled with " + count + " items (limit " + limit + ").");
        if (items.size() > limit) {
            plugin.getLogger().warning("Tables module has more items than the configured limit.");
        }
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Tables module stopped.");
        }
    }
}
