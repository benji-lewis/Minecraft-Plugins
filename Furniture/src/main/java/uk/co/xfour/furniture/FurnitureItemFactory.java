package uk.co.xfour.furniture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private static final String DEFAULT_ITEM_SECTION = "items";

    private static final Map<String, Material> DEFAULT_MATERIALS = Map.ofEntries(
            Map.entry("oak-armchair", Material.OAK_STAIRS),
            Map.entry("spruce-sofa", Material.SPRUCE_STAIRS),
            Map.entry("crimson-bench", Material.CRIMSON_STAIRS),
            Map.entry("leather-recliner", Material.BROWN_WOOL),
            Map.entry("oak-dining-table", Material.OAK_SLAB),
            Map.entry("birch-coffee-table", Material.BIRCH_SLAB),
            Map.entry("granite-side-table", Material.POLISHED_GRANITE_SLAB),
            Map.entry("glass-console-table", Material.GLASS),
            Map.entry("wardrobe", Material.BARREL),
            Map.entry("cabinet", Material.CHEST),
            Map.entry("dresser", Material.BROWN_SHULKER_BOX),
            Map.entry("bookshelf", Material.BOOKSHELF),
            Map.entry("chandelier", Material.SOUL_LANTERN),
            Map.entry("floor-lamp", Material.LANTERN),
            Map.entry("wall-sconce", Material.LANTERN),
            Map.entry("lantern-stand", Material.LANTERN),
            Map.entry("rug", Material.RED_CARPET),
            Map.entry("wall-clock", Material.CLOCK),
            Map.entry("vase", Material.FLOWER_POT),
            Map.entry("painting-set", Material.PAINTING),
            Map.entry("patio-chair", Material.OAK_STAIRS),
            Map.entry("garden-bench", Material.OAK_STAIRS),
            Map.entry("pergola", Material.OAK_FENCE),
            Map.entry("firepit", Material.CAMPFIRE),
            Map.entry("oven-range", Material.BLAST_FURNACE),
            Map.entry("kitchen-island", Material.SMOOTH_QUARTZ),
            Map.entry("pantry-shelf", Material.BARREL),
            Map.entry("sink-unit", Material.CAULDRON),
            Map.entry("canopy-bed", Material.WHITE_WOOL),
            Map.entry("bunk-bed", Material.LIGHT_GRAY_WOOL),
            Map.entry("bedside-table", Material.OAK_SLAB),
            Map.entry("vanity", Material.CARTOGRAPHY_TABLE),
            Map.entry("bathtub", Material.QUARTZ_STAIRS),
            Map.entry("sink-basin", Material.CAULDRON),
            Map.entry("towel-rack", Material.WHITE_BANNER),
            Map.entry("mirror", Material.GLASS_PANE));

    private static final Map<String, FurniturePlacementStyle> DEFAULT_PLACEMENTS = Map.ofEntries(
            Map.entry("wall-clock", FurniturePlacementStyle.WALL_DECOR),
            Map.entry("wall-sconce", FurniturePlacementStyle.WALL_DECOR),
            Map.entry("painting-set", FurniturePlacementStyle.WALL_DECOR),
            Map.entry("towel-rack", FurniturePlacementStyle.WALL_DECOR),
            Map.entry("mirror", FurniturePlacementStyle.WALL_DECOR));

    private final NamespacedKey namespacedKey;
    private final JavaPlugin plugin;
    private final Map<String, FurnitureItemDefinition> definitions = new HashMap<>();

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
        this.definitions.clear();
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
                FurnitureItemDefinition definition = buildDefinition(itemKey, moduleType);
                definitions.add(definition);
                this.definitions.put(itemKey, definition);
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
        ItemStack item = new ItemStack(definition.itemMaterial());
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
     * Returns a furniture definition by key.
     *
     * @param key the furniture key
     * @return the definition or null
     */
    public FurnitureItemDefinition getDefinition(String key) {
        return definitions.get(key);
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

    private FurnitureItemDefinition buildDefinition(String itemKey, FurnitureModuleType moduleType) {
        String displayName = toDisplayName(itemKey);
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection(DEFAULT_ITEM_SECTION + "." + itemKey);
        Material itemMaterial = resolveMaterial(itemSection, "item-material",
                DEFAULT_MATERIALS.getOrDefault(itemKey, moduleType.getDisplayMaterial()));
        Material placedMaterial = resolveMaterial(itemSection, "place-material", itemMaterial);
        FurniturePlacementStyle placementStyle = resolvePlacementStyle(itemKey, itemSection);
        return new FurnitureItemDefinition(itemKey, displayName, itemMaterial, placedMaterial, placementStyle, moduleType);
    }

    private FurniturePlacementStyle resolvePlacementStyle(String itemKey, ConfigurationSection section) {
        if (section != null) {
            String placement = section.getString("placement");
            if (placement != null) {
                try {
                    return FurniturePlacementStyle.valueOf(placement.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Unknown placement style '" + placement + "' for furniture item "
                            + itemKey + ". Using default placement.");
                }
            }
        }
        return DEFAULT_PLACEMENTS.getOrDefault(itemKey, FurniturePlacementStyle.BLOCK);
    }

    private Material resolveMaterial(ConfigurationSection section, String key, Material fallback) {
        if (section == null) {
            return fallback;
        }
        String materialName = section.getString(key);
        if (materialName == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + materialName + "' in furniture item configuration.");
            return fallback;
        }
        return material;
    }
}
