package uk.co.xfour.kimjongun2;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class KimJongUnItems {
    private final KimJongUn2Plugin plugin;
    private final KimJongUnKeys keys;
    private final Random random = new Random();
    private final FileConfiguration config;

    public KimJongUnItems(KimJongUn2Plugin plugin) {
        this.plugin = plugin;
        this.keys = new KimJongUnKeys(plugin);
        this.config = plugin.getConfig();
    }

    public KimJongUnKeys keys() {
        return keys;
    }

    public ItemStack createItem(KimJongUnItem item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(item.displayName());
            meta.setLore(item.lore());
            meta.getPersistentDataContainer().set(keys.itemKey, PersistentDataType.STRING, item.id());
            int modelData = modelData(item);
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Optional<KimJongUnItem> identify(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(keys.itemKey, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        return KimJongUnItem.fromId(id);
    }

    public KimJongUnItem randomPart() {
        List<KimJongUnItem> parts = KimJongUnItem.partItems();
        return parts.get(random.nextInt(parts.size()));
    }

    public void registerRecipes() {
        registerMissileRecipe();
        registerLaunchpadRecipe();
    }

    private void registerMissileRecipe() {
        ItemStack result = createItem(KimJongUnItem.MISSILE);
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "missile"), result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.MISSILE_NOSE)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.MISSILE_BODY)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.MISSILE_ENGINE)));
        Bukkit.addRecipe(recipe);
    }

    private void registerLaunchpadRecipe() {
        ItemStack result = createItem(KimJongUnItem.LAUNCHPAD);
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "launchpad"), result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.LAUNCHPAD_BASE)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.LAUNCHPAD_CONTROL)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.LAUNCHPAD_SUPPORT)));
        Bukkit.addRecipe(recipe);
    }

    private int modelData(KimJongUnItem item) {
        String path = "models." + item.configKey();
        if (config.contains(path)) {
            return config.getInt(path, 0);
        }
        return item.defaultModelData();
    }

    public enum KimJongUnItem {
        MISSILE_NOSE("missile_nose", "Missile Nose Cone", Material.IRON_INGOT,
                "missile-nose-custom-model-data", 5001, List.of("A precision-guided nose cone.")),
        MISSILE_BODY("missile_body", "Missile Body", Material.IRON_INGOT,
                "missile-body-custom-model-data", 5002, List.of("Reinforced fuselage plating.")),
        MISSILE_ENGINE("missile_engine", "Missile Engine", Material.IRON_INGOT,
                "missile-engine-custom-model-data", 5003, List.of("Thrust vectoring engine.")),
        LAUNCHPAD_BASE("launchpad_base", "Launchpad Base", Material.NETHERITE_SCRAP,
                "launchpad-base-custom-model-data", 5004, List.of("Stabilized platform core.")),
        LAUNCHPAD_CONTROL("launchpad_control", "Launchpad Control", Material.NETHERITE_SCRAP,
                "launchpad-control-custom-model-data", 5005, List.of("Guidance and ignition panel.")),
        LAUNCHPAD_SUPPORT("launchpad_support", "Launchpad Support", Material.NETHERITE_SCRAP,
                "launchpad-support-custom-model-data", 5006, List.of("Hydraulic support struts.")),
        MISSILE("missile", "Assembled Missile", Material.NETHER_STAR,
                "missile-custom-model-data", 5010, List.of("Handle with care.")),
        LAUNCHPAD("launchpad", "Assembled Launchpad", Material.NETHER_STAR,
                "launchpad-custom-model-data", 5011, List.of("Place on flat ground."));

        private final String id;
        private final String displayName;
        private final Material material;
        private final String configKey;
        private final int defaultModelData;
        private final List<String> lore;

        KimJongUnItem(String id, String displayName, Material material, String configKey,
                int defaultModelData, List<String> lore) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
            this.configKey = configKey;
            this.defaultModelData = defaultModelData;
            this.lore = lore;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public Material material() {
            return material;
        }

        public String configKey() {
            return configKey;
        }

        public int defaultModelData() {
            return defaultModelData;
        }

        public List<String> lore() {
            return lore;
        }

        public static Optional<KimJongUnItem> fromId(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            String normal = id.toLowerCase(Locale.ROOT);
            return Arrays.stream(values())
                    .filter(item -> item.id.equals(normal))
                    .findFirst();
        }

        public static List<KimJongUnItem> partItems() {
            return EnumSet.of(MISSILE_NOSE, MISSILE_BODY, MISSILE_ENGINE,
                    LAUNCHPAD_BASE, LAUNCHPAD_CONTROL, LAUNCHPAD_SUPPORT)
                .stream().collect(Collectors.toList());
        }
    }
}
