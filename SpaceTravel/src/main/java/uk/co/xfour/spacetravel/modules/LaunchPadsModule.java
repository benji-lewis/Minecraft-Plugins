package uk.co.xfour.spacetravel.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controls launch pad behaviour and cooldowns.
 */
public final class LaunchPadsModule implements Module {
    private final JavaPlugin plugin;
    private boolean enabled;

    /**
     * Creates a launch pads module instance.
     *
     * @param plugin the owning plugin
     */
    public LaunchPadsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("modules.launch-pads.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Launch pads module disabled via config.");
            return;
        }
        int cooldownSeconds = config.getInt("modules.launch-pads.cooldown-seconds", 45);
        String launchSound = config.getString("modules.launch-pads.launch-sound",
                "minecraft:entity.ender_dragon.growl");
        plugin.getLogger().info("Launch pads module enabled with cooldown: " + cooldownSeconds
                + ", sound: " + launchSound + ".");
    }

    @Override
    public void stop() {
        if (enabled) {
            plugin.getLogger().info("Launch pads module stopped.");
        }
    }
}
