package uk.co.xfour.kimjongun3

import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * Nova addon entry point for Kim Jong Un 3.
 */
object KimJongUn3Addon : Addon() {
    private var registeredItems: KimJongUn3AddonItems? = null
    private var registeredBlocks: KimJongUn3AddonBlocks? = null
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
        val blocks = registerBlocks()
        val items = mapOf(
            KimJongUnItems.KimJongUnItem.MISSILE_NOSE to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_NOSE),
            KimJongUnItems.KimJongUnItem.MISSILE_BODY to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_BODY),
            KimJongUnItems.KimJongUnItem.MISSILE_ENGINE to buildItem(KimJongUnItems.KimJongUnItem.MISSILE_ENGINE),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_BASE to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_BASE),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_CONTROL to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_CONTROL),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD_SUPPORT to buildItem(KimJongUnItems.KimJongUnItem.LAUNCHPAD_SUPPORT),
            KimJongUnItems.KimJongUnItem.ICBM_CORE to buildItem(KimJongUnItems.KimJongUnItem.ICBM_CORE),
            KimJongUnItems.KimJongUnItem.MISSILE to buildBlockItem(blocks.missile, KimJongUnItems.KimJongUnItem.MISSILE),
            KimJongUnItems.KimJongUnItem.ICBM to buildBlockItem(blocks.icbm, KimJongUnItems.KimJongUnItem.ICBM),
            KimJongUnItems.KimJongUnItem.LAUNCHPAD to buildBlockItem(blocks.launchpad, KimJongUnItems.KimJongUnItem.LAUNCHPAD)
        )
        registeredItems = KimJongUn3AddonItems(items)
        return registeredItems as KimJongUn3AddonItems
    }

    /**
     * Registers the Nova blocks used by this addon if they are not registered yet.
     *
     * @return the registered block holder
     */
    fun registerBlocks(): KimJongUn3AddonBlocks {
        if (registeredBlocks != null) {
            return registeredBlocks as KimJongUn3AddonBlocks
        }
        val launchpad = buildBlock(KimJongUnItems.KimJongUnItem.LAUNCHPAD, "block/launchpad")
        val missile = buildBlock(KimJongUnItems.KimJongUnItem.MISSILE, "block/missile")
        val icbm = buildBlock(KimJongUnItems.KimJongUnItem.ICBM, "block/icbm")
        registeredBlocks = KimJongUn3AddonBlocks(launchpad, missile, icbm)
        return registeredBlocks as KimJongUn3AddonBlocks
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
        registerWithNova()
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

    private fun registerWithNova() {
        val bootstrapperClass = Class.forName("xyz.xenondevs.nova.addon.AddonBootstrapper")
        val addonsField = bootstrapperClass.getDeclaredField("_addons").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val addons = addonsField.get(null) as MutableList<Any>
        val exists = addons.any { it === this }
        if (!exists) {
            addons.add(this)
        }
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

    /**
     * Provides access to the registered block holder.
     *
     * @return the registered block holder
     * @throws IllegalStateException if blocks were not registered yet
     */
    fun getBlocks(): KimJongUn3AddonBlocks {
        return registeredBlocks ?: error("Kim Jong Un 3 blocks have not been registered yet.")
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

    private fun buildBlock(item: KimJongUnItems.KimJongUnItem, modelPath: String): NovaBlock {
        return block(item.id()) {
            name(Component.text(item.displayName()))
            stateBacked(BackingStateCategory.NOTE_BLOCK) {
                getModel(modelPath)
            }
        }
    }

    private fun buildBlockItem(block: NovaBlock, definition: KimJongUnItems.KimJongUnItem): NovaItem {
        return item(block, definition.id()) {
            name(Component.text(definition.displayName()))
            val loreComponents = definition.lore().map { Component.text(it) }
            if (loreComponents.isNotEmpty()) {
                lore(*loreComponents.toTypedArray())
            }
        }
    }
}
