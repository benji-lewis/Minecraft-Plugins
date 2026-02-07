package uk.co.xfour.furniture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds custom furniture items from configuration.
 */
public final class FurnitureItemFactory {
    private static final String DATA_KEY = "furniture-item";

    private final NamespacedKey namespacedKey;
    private final JavaPlugin plugin;

    /**
     * Creates a new furniture item factory.
     *
     * @param plugin the owning plugin
     */
    public FurnitureItemFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        this.namespacedKey = new NamespacedKey(plugin, DATA_KEY);
    }

    /**
     * Loads all furniture items defined in config.
     *
     * @return the item definitions
     */
    public List<FurnitureItemDefinition> loadDefinitions() {
        List<FurnitureItemDefinition> definitions = new ArrayList<>();
        ConfigurationSection modules = plugin.getConfig().getConfigurationSection("modules");
        if (modules == null) {
            return definitions;
        }
        for (String moduleKey : modules.getKeys(false)) {
            if ("tuning".equals(moduleKey)) {
                continue;
            }
            FurnitureModuleType moduleType = FurnitureModuleType.fromKey(moduleKey);
            if (moduleType == null) {
                continue;
            }
            List<String> items = plugin.getConfig().getStringList("modules." + moduleKey + ".items");
            for (String itemKey : items) {
                String displayName = toDisplayName(itemKey);
                definitions.add(new FurnitureItemDefinition(itemKey, displayName, moduleType.getDisplayMaterial(),
                        moduleType));
            }
        }
        return definitions;
    }

    /**
     * Builds a custom furniture item.
     *
     * @param definition the item definition
     * @return the item stack
     */
    public ItemStack createItem(FurnitureItemDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + definition.displayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, definition.key());
            container.set(new NamespacedKey(plugin, "furniture-module"), PersistentDataType.STRING,
                    definition.module().getKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Extracts the furniture item key from a stack.
     *
     * @param item the item stack
     * @return the key or null
     */
    public String getItemKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    /**
     * Extracts the module key from a stack.
     *
     * @param item the item stack
     * @return the module key or null
     */
    public String getModuleKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "furniture-module"),
                PersistentDataType.STRING);
    }

    private String toDisplayName(String key) {
        String[] parts = key.split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }
}
