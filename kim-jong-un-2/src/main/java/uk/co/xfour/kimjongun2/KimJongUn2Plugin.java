package uk.co.xfour.kimjongun2;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KimJongUn2Plugin extends JavaPlugin {
    private KimJongUnItems items;
    private KimJongUnSpawner spawner;
    private LaunchManager launchManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new KimJongUnItems(this);
        launchManager = new LaunchManager(this, items);
        spawner = new KimJongUnSpawner(this, items);

        Bukkit.getPluginManager().registerEvents(new KimJongUnListener(this, items, launchManager), this);

        registerCommand();
        items.registerRecipes();
        spawner.start();
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

    private void registerCommand() {
        KimJongUnCommand command = new KimJongUnCommand(this, items, spawner);
        registerCommand("kimjongun2", "Kim Jong Un 2 admin command.", command);
    }
}
