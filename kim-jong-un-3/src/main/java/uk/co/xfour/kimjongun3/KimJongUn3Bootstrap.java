package uk.co.xfour.kimjongun3;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.nova.addon.AddonBootstrapper;

/**
 * Bootstraps the Kim Jong Un 3 Nova addon so Nova can initialize addon metadata.
 */
public class KimJongUn3Bootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        AddonBootstrapper.bootstrap(context, getClass().getClassLoader());
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        KimJongUn3Plugin plugin = new KimJongUn3Plugin();
        AddonBootstrapper.handleJavaPluginCreated(plugin, context, getClass().getClassLoader());
        return plugin;
    }
}
