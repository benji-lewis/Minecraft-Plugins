package uk.co.xfour.kimjongun;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin entry point for Kim Jong Un.
 */
public class KimJongUnPlugin extends JavaPlugin {
    private KimJongUnItems items;
    private KimJongUnBlocks blocks;
    private KimJongUnSpawner spawner;
    private LaunchManager launchManager;
    private RadiationSuit radiationSuit;
    private FalloutManager falloutManager;
    private IcbmTargetingManager targetingManager;
    private AutoUpdater autoUpdater;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializePlugin();
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

    private void initializePlugin() {
        items = new KimJongUnItems(this);
        blocks = new KimJongUnBlocks();
        radiationSuit = new RadiationSuit(this, items.keys());
        falloutManager = new FalloutManager(this, radiationSuit);
        launchManager = new LaunchManager(this, items, falloutManager);
        spawner = new KimJongUnSpawner(this, items);
        targetingManager = new IcbmTargetingManager(this, items, blocks, launchManager);

        Bukkit.getPluginManager().registerEvents(
            new KimJongUnListener(this, items, blocks, launchManager, targetingManager),
            this
        );
        Bukkit.getPluginManager().registerEvents(targetingManager, this);
        Bukkit.getPluginManager().registerEvents(new KimJongUnCraftingListener(items), this);

        registerCommand();
        radiationSuit.registerRecipes();
        spawner.start();
        falloutManager.start();
        autoUpdater = new AutoUpdater(this);
        autoUpdater.checkForUpdatesAsync();
    }

    private void registerCommand() {
        KimJongUnCommand command = new KimJongUnCommand(this, items, spawner, radiationSuit);
        registerCommand("kimjongun", "Kim Jong Un admin command.", command);
    }
}
