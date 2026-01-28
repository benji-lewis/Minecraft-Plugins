package uk.co.xfour.kimjongun3

import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * Nova addon entry point for Kim Jong Un 3.
 */
object KimJongUn3Addon : Addon() {
    private var registeredItems: KimJongUn3AddonItems? = null
    private var metadataInitialized = false

    /**
     * Registers the Nova items used by this addon if they are not registered yet.
     *
     * @return the registered item holder
     */
    fun registerItems(): KimJongUn3AddonItems {
        if (registeredItems != null) {
            return registeredItems as KimJongUn3AddonItems
        }
        val items = mapOf(
            KimJongUnItems.KimJongUnItem.MISSILE_NOSE to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_NOSE),
            KimJongUnItems.KimJongUnItem.MISSILE_BODY to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_BODY),
            KimJongUnItems.KimJongUnItem.MISSILE_ENGINE to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_ENGINE),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_BASE to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_BASE),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_CONTROL to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_CONTROL),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_SUPPORT to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_SUPPORT),
            KimJongUnItems.KimJongUnItem.MISSILE to buildItem(KimJongUnItems.KimJongUnItem.MISSILE),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD)
        )
        registeredItems = KimJongUn3AddonItems(items)
        return registeredItems as KimJongUn3AddonItems
    }

    /**
     * Initializes Nova addon metadata from the owning JavaPlugin if needed.
     *
     * @param plugin the owning JavaPlugin instance
     */
    fun initializeFrom(plugin: JavaPlugin) {
        if (metadataInitialized) {
            return
        }
        val addonClass = Addon::class.java
        val pluginMetaClass = Class.forName("io.papermc.paper.plugin.configuration.PluginMeta")
        addonClass.findSetter("setPluginMeta\$nova", pluginMetaClass)
            .invoke(this, plugin.pluginMeta)
        val fileField = JavaPlugin::class.java.getDeclaredField("file").apply { isAccessible = true }
        val pluginFile = fileField.get(plugin) as java.io.File
        addonClass.findSetter("setFile\$nova", java.nio.file.Path::class.java)
            .invoke(this, pluginFile.toPath())
        addonClass.findSetter("setDataFolder\$nova", java.nio.file.Path::class.java)
            .invoke(this, plugin.dataFolder.toPath())
        addonClass.findSetter("setLogger\$nova", plugin.componentLogger.javaClass)
            .invoke(this, plugin.componentLogger)
        addonClass.findSetter("setPlugin\$nova", JavaPlugin::class.java)
            .invoke(this, plugin)
        metadataInitialized = true
    }

    private fun Class<*>.findSetter(name: String, paramType: Class<*>): java.lang.reflect.Method {
        return declaredMethods.firstOrNull { method ->
            method.name == name
                && method.parameterCount == 1
                && method.parameterTypes[0].isAssignableFrom(paramType)
        }?.apply { isAccessible = true }
            ?: throw NoSuchMethodException("$name(${paramType.name})")
    }

    /**
     * Provides access to the registered item holder.
     *
     * @return the registered item holder
     * @throws IllegalStateException if items were not registered yet
     */
    fun getItems(): KimJongUn3AddonItems {
        return registeredItems ?: error("Kim Jong Un 3 items have not been registered yet.")
    }

    private fun buildItem(item: KimJongUnItems.KimJongUnItem): NovaItem {
        return item(item.id()) {
            name(Component.text(item.displayName()))
            val loreComponents = item.lore().map { Component.text(it) }
            if (loreComponents.isNotEmpty()) {
                lore(*loreComponents.toTypedArray())
            }
        }
    }
}
