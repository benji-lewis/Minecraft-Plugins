package uk.co.xfour.kimjongun3;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.xenondevs.nova.world.item.NovaItem;

/**
 * Handles Nova item creation and identification for the Kim Jong Un 3 addon.
 */
public class KimJongUnItems {
    private final KimJongUnKeys keys;
    private final Random random = new Random();
    private final KimJongUn3AddonItems addonItems;

    /**
     * Creates a new Kim Jong Un item helper.
     *
     * @param plugin owning plugin instance
     * @param addonItems registered Nova item mappings
     */
    public KimJongUnItems(KimJongUn3Plugin plugin, KimJongUn3AddonItems addonItems) {
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
     * Resolves custom craft outputs based on item IDs in the crafting matrix.
     *
     * @param matrix the current crafting matrix
     * @return the crafted Kim Jong Un item if a recipe matches
     */
    public Optional<KimJongUnItem> resolveCustomCraft(ItemStack[] matrix) {
        Map<KimJongUnItem, Integer> counts = new HashMap<>();
        int ingredientSlots = 0;
        for (ItemStack stack : matrix) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            ingredientSlots++;
            Optional<KimJongUnItem> identified = identify(stack);
            if (identified.isEmpty()) {
                return Optional.empty();
            }
            KimJongUnItem item = identified.get();
            counts.put(item, counts.getOrDefault(item, 0) + 1);
        }

        if (ingredientSlots == 3
            && hasCounts(counts, Map.of(
                KimJongUnItem.MISSILE_NOSE, 1,
                KimJongUnItem.MISSILE_BODY, 1,
                KimJongUnItem.MISSILE_ENGINE, 1))) {
            return Optional.of(KimJongUnItem.MISSILE);
        }

        if (ingredientSlots == 3
            && hasCounts(counts, Map.of(
                KimJongUnItem.LAUNCHPAD_BASE, 1,
                KimJongUnItem.LAUNCHPAD_CONTROL, 1,
                KimJongUnItem.LAUNCHPAD_SUPPORT, 1))) {
            return Optional.of(KimJongUnItem.LAUNCHPAD);
        }

        if (ingredientSlots == 2
            && hasCounts(counts, Map.of(
                KimJongUnItem.MISSILE, 1,
                KimJongUnItem.ICBM_CORE, 1))) {
            return Optional.of(KimJongUnItem.ICBM);
        }

        return Optional.empty();
    }

    private boolean hasCounts(Map<KimJongUnItem, Integer> actual, Map<KimJongUnItem, Integer> expected) {
        if (actual.size() != expected.size()) {
            return false;
        }
        for (Map.Entry<KimJongUnItem, Integer> entry : expected.entrySet()) {
            if (!entry.getValue().equals(actual.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
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
