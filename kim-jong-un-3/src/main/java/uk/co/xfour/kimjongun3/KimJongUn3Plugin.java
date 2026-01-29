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
    private RadiationSuit radiationSuit;
    private FalloutManager falloutManager;
    private IcbmTargetingManager targetingManager;
    private AutoUpdater autoUpdater;

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
        if (falloutManager != null) {
            falloutManager.stop();
        }
        if (targetingManager != null) {
            targetingManager.shutdown();
        }
    }

    private void initializeAddon() {
        KimJongUn3AddonItems addonItems = KimJongUn3Addon.INSTANCE.registerItems();
        items = new KimJongUnItems(this, addonItems);
        radiationSuit = new RadiationSuit(this, items.keys());
        falloutManager = new FalloutManager(this, radiationSuit);
        launchManager = new LaunchManager(this, items, falloutManager);
        spawner = new KimJongUnSpawner(this, items);
        targetingManager = new IcbmTargetingManager(this, items, launchManager);

        Bukkit.getPluginManager().registerEvents(new KimJongUnListener(this, items, launchManager, targetingManager), this);
        Bukkit.getPluginManager().registerEvents(targetingManager, this);

        registerCommand();
        items.registerRecipes();
        radiationSuit.registerRecipes();
        spawner.start();
        falloutManager.start();
        autoUpdater = new AutoUpdater(this);
        autoUpdater.checkForUpdatesAsync();
    }

    private void registerCommand() {
        KimJongUnCommand command = new KimJongUnCommand(this, items, spawner, radiationSuit);
        registerCommand("kimjongun3", "Kim Jong Un 3 admin command.", command);
    }
}
