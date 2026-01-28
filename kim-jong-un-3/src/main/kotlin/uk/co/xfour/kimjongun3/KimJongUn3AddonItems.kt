package uk.co.xfour.kimjongun3

import xyz.xenondevs.nova.world.item.NovaItem

/**
 * Stores the Nova items registered by the Kim Jong Un 3 addon.
 */
class KimJongUn3AddonItems(private val items: Map<KimJongUnItems.KimJongUnItem, NovaItem>) {
    /**
     * Returns the Nova item registered for the given Kim Jong Un item definition.
     *
     * @param item the Kim Jong Un item definition
     * @return the registered Nova item
     * @throws java.util.NoSuchElementException if the item is not registered
     */
    fun itemFor(item: KimJongUnItems.KimJongUnItem): NovaItem = items.getValue(item)
}
