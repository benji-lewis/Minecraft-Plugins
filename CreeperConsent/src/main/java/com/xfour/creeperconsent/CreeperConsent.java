package com.xfour.creeperconsent;

import org.bukkit.plugin.java.JavaPlugin;
import com.xfour.creeperconsent.config.ConfigManager;
import com.xfour.creeperconsent.listener.ExplosionListener;
import com.xfour.creeperconsent.listener.InventoryClickListener;
import com.xfour.creeperconsent.listener.DamageListener;
import com.xfour.creeperconsent.util.CreeperManager;

public class CreeperConsent extends JavaPlugin {

    private ConfigManager configManager;
    private CreeperManager creeperManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.creeperManager = new CreeperManager();
        configManager.loadConfig();

        if (!configManager.isEnabled()) {
            getLogger().info("CreeperConsent is disabled in config. Plugin loaded but inactive.");
            return;
        }

        // Register all listeners
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this, creeperManager), this);
        getServer().getPluginManager().registerEvents(new DamageListener(creeperManager), this);

        getLogger().info("CreeperConsent enabled! Creepers will now ask for permission.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CreeperConsent disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CreeperManager getCreeperManager() {
        return creeperManager;
    }
}
