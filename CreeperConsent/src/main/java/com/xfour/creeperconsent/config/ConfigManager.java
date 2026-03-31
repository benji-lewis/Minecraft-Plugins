package com.xfour.creeperconsent.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.xfour.creeperconsent.CreeperConsent;

public class ConfigManager {

    private final CreeperConsent plugin;
    private FileConfiguration config;

    private boolean enabled;
    private String titleMessage;
    private String acceptMessage;
    private String denyMessage;

    public ConfigManager(CreeperConsent plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);
        this.titleMessage = config.getString("messages.title", "A creeper has asked for permission to explode");
        this.acceptMessage = config.getString("messages.accept", "Sure, let's go");
        this.denyMessage = config.getString("messages.deny", "Nah, not today");

        plugin.getLogger().info("Config loaded: enabled=" + enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTitleMessage() {
        return titleMessage;
    }

    public String getAcceptMessage() {
        return acceptMessage;
    }

    public String getDenyMessage() {
        return denyMessage;
    }
}
