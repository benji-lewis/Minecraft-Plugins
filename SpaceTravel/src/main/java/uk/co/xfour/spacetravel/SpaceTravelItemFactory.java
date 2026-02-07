package uk.co.xfour.spacetravel;

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
 * Builds custom space travel items and keys.
 */
public final class SpaceTravelItemFactory {
    private static final String DATA_KEY = "spacetravel-item";

    private final NamespacedKey namespacedKey;

    /**
     * Creates a new space travel item factory.
     *
     * @param plugin the owning plugin
     */
    public SpaceTravelItemFactory(JavaPlugin plugin) {
        this.namespacedKey = new NamespacedKey(plugin, DATA_KEY);
    }

    /**
     * Builds a custom space travel item.
     *
     * @param type the item type
     * @return the item stack
     */
    public ItemStack createItem(SpaceTravelItemType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + type.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, type.getKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Reads the space travel item type from an item stack.
     *
     * @param item the item stack
     * @return the item type, or null if none
     */
    public SpaceTravelItemType getType(ItemStack item) {
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
        for (SpaceTravelItemType type : SpaceTravelItemType.values()) {
            if (type.getKey().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
