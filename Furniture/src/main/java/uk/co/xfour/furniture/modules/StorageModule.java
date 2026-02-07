package uk.co.xfour.furniture.modules;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.FurnitureRegistry;

/**
 * Controls storage furniture definitions.
 */
public final class StorageModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a storage module instance.
     *
     * @param plugin the owning plugin
     */
    public StorageModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.storage.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Storage module disabled via config.");
            return;
        }
        List<String> items = config.getStringList("modules.storage.items");
        int limit = FurnitureRegistry.normaliseLimit(config.getInt("modules.tuning.max-items-per-module", 24));
        int count = Math.min(items.size(), limit);
        plugin.getLogger().info("Storage module enabled with " + count + " items (limit " + limit + ").");
        if (items.size() > limit) {
            plugin.getLogger().warning("Storage module has more items than the configured limit.");
        }
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Storage module stopped.");
        }
    }
}
