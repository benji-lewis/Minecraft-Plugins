package uk.co.xfour.kimjongun3;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.xenondevs.nova.world.item.NovaItem;

/**
 * Handles Nova item creation and identification for the Kim Jong Un 3 addon.
 */
public class KimJongUnItems {
    private final KimJongUn3Plugin plugin;
    private final KimJongUnKeys keys;
    private final Random random = new Random();
    private final KimJongUn3AddonItems addonItems;

    public KimJongUnItems(KimJongUn3Plugin plugin, KimJongUn3AddonItems addonItems) {
        this.plugin = plugin;
        this.keys = new KimJongUnKeys(plugin);
        this.addonItems = addonItems;
    }

    /**
     * Returns the persistent data keys used by the addon.
     *
     * @return the keys used by Kim Jong Un 3
     */
    public KimJongUnKeys keys() {
        return keys;
    }

    /**
     * Creates a configured Nova-backed ItemStack for the requested item.
     *
     * @param item the Kim Jong Un item definition
     * @return a matching ItemStack
     */
    public ItemStack createItem(KimJongUnItem item) {
        NovaItem novaItem = addonItems.itemFor(item);
        ItemStack stack = novaItem.createItemStack(1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(keys.itemKey, PersistentDataType.STRING, item.id());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Identifies a Kim Jong Un item from the provided ItemStack.
     *
     * @param stack the ItemStack to inspect
     * @return the matching item definition if present
     */
    public Optional<KimJongUnItem> identify(ItemStack stack) {
        if (stack == null) {
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

    /**
     * Selects a random missile or launchpad part.
     *
     * @return a random part item definition
     */
    public KimJongUnItem randomPart() {
        List<KimJongUnItem> parts = KimJongUnItem.partItems();
        return parts.get(random.nextInt(parts.size()));
    }

    /**
     * Registers crafting recipes for the assembled launchpad and missile.
     */
    public void registerRecipes() {
        registerMissileRecipe();
        registerLaunchpadRecipe();
        registerIcbmRecipe();
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

    private void registerIcbmRecipe() {
        ItemStack result = createItem(KimJongUnItem.ICBM);
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "icbm"), result);
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.MISSILE)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(createItem(KimJongUnItem.ICBM_CORE)));
        Bukkit.addRecipe(recipe);
    }

    public enum KimJongUnItem {
        MISSILE_NOSE("missile_nose", "Missile Nose Cone", List.of("A precision-guided nose cone.")),
        MISSILE_BODY("missile_body", "Missile Body", List.of("Reinforced fuselage plating.")),
        MISSILE_ENGINE("missile_engine", "Missile Engine", List.of("Thrust vectoring engine.")),
        LAUNCHPAD_BASE("launchpad_base", "Launchpad Base", List.of("Stabilized platform core.")),
        LAUNCHPAD_CONTROL("launchpad_control", "Launchpad Control", List.of("Guidance and ignition panel.")),
        LAUNCHPAD_SUPPORT("launchpad_support", "Launchpad Support", List.of("Hydraulic support struts.")),
        ICBM_CORE("icbm_core", "ICBM Core", List.of("Encrypted guidance matrix.")),
        MISSILE("missile", "Assembled Missile", List.of("Handle with care.")),
        ICBM("icbm", "Assembled ICBM", List.of("Strategic payload authorized.")),
        LAUNCHPAD("launchpad", "Assembled Launchpad", List.of("Place on flat ground."));

        private final String id;
        private final String displayName;
        private final List<String> lore;

        KimJongUnItem(String id, String displayName, List<String> lore) {
            this.id = id;
            this.displayName = displayName;
            this.lore = lore;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
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
