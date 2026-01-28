package uk.co.xfour.kimjongun3

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * Nova addon entry point for Kim Jong Un 3.
 */
object KimJongUn3Addon : Addon() {
    private var registeredItems: KimJongUn3AddonItems? = null

    /**
     * Registers the Nova items used by this addon if they are not registered yet.
     *
     * @return the registered item holder
     */
    fun registerItems(): KimJongUn3AddonItems {
        if (!isReady()) {
            error("Kim Jong Un 3 addon metadata is not initialized yet.")
        }
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
     * Returns whether Nova has initialized addon metadata for this addon.
     *
     * @return true if the addon metadata is ready
     */
    fun isReady(): Boolean {
        val field = Addon::class.java.getDeclaredField("pluginMeta")
        field.isAccessible = true
        return field.get(this) != null
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
