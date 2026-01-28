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
    private boolean initialized;
    private org.bukkit.scheduler.BukkitRunnable addonInitTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (KimJongUn3Addon.INSTANCE.isReady()) {
            initializeAddon();
            return;
        }
        getLogger().info("Waiting for Nova to initialize addon metadata.");
        addonInitTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (initialized) {
                    cancel();
                    return;
                }
                if (KimJongUn3Addon.INSTANCE.isReady()) {
                    initializeAddon();
                    cancel();
                }
            }
        };
        addonInitTask.runTaskTimer(this, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (addonInitTask != null) {
            addonInitTask.cancel();
            addonInitTask = null;
        }
        if (spawner != null) {
            spawner.stop();
        }
        if (launchManager != null) {
            launchManager.shutdown();
        }
    }

    private void initializeAddon() {
        if (initialized) {
            return;
        }
        initialized = true;
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
