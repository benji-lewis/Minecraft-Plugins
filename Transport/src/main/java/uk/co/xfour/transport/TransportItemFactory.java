package uk.co.xfour.transport;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds custom transport items and keys.
 */
public final class TransportItemFactory {
    private static final String DATA_KEY = "transport-item";

    private final NamespacedKey namespacedKey;

    /**
     * Creates a new transport item factory.
     *
     * @param plugin the owning plugin
     */
    public TransportItemFactory(JavaPlugin plugin) {
        this.namespacedKey = new NamespacedKey(plugin, DATA_KEY);
    }

    /**
     * Builds a custom transport item.
     *
     * @param type the transport item type
     * @return the item stack
     */
    public ItemStack createItem(TransportItemType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + type.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, type.getKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Reads the transport item type from an item stack.
     *
     * @param item the item stack
     * @return the transport item type, or null if none
     */
    public TransportItemType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String value = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (TransportItemType type : TransportItemType.values()) {
            if (type.getKey().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
