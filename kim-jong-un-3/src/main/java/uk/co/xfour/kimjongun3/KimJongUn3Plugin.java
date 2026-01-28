package uk.co.xfour.kimjongun3;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin entry point for the Kim Jong Un 3 Nova addon.
 */
public class KimJongUn3Plugin extends JavaPlugin {
    private KimJongUnItems items;
    private KimJongUnSpawner spawner;
    private LaunchManager launchManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        KimJongUn3Addon.INSTANCE.initializeFrom(this);
        initializeAddon();
    }

    @Override
    public void onDisable() {
        if (spawner != null) {
            spawner.stop();
        }
        if (launchManager != null) {
            launchManager.shutdown();
        }
    }

    private void initializeAddon() {
        KimJongUn3AddonItems addonItems = KimJongUn3Addon.INSTANCE.registerItems();
        items = new KimJongUnItems(this, addonItems);
        launchManager = new LaunchManager(this, items);
        spawner = new KimJongUnSpawner(this, items);

        Bukkit.getPluginManager().registerEvents(new KimJongUnListener(this, items, launchManager), this);

        registerCommand();
        items.registerRecipes();
        spawner.start();
    }

    private void registerCommand() {
        KimJongUnCommand command = new KimJongUnCommand(this, items, spawner);
        registerCommand("kimjongun3", "Kim Jong Un 3 admin command.", command);
    }
}
