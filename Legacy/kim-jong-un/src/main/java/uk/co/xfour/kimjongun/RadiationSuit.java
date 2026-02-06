package uk.co.xfour.kimjongun;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Creates and validates radiation suit equipment that protects players from nuclear fallout.
 */
public class RadiationSuit {
    private static final Color SUIT_COLOR = Color.fromRGB(242, 214, 57);
    private static final List<String> BASE_LORE = List.of("Protects against nuclear fallout.");

    private final KimJongUnPlugin plugin;
    private final KimJongUnKeys keys;
    private final Map<Piece, ItemStack> cachedPieces = new EnumMap<>(Piece.class);

    public RadiationSuit(KimJongUnPlugin plugin, KimJongUnKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    /**
     * Creates a radiation suit piece with a custom display name and persistent data tag.
     *
     * @param piece the radiation suit piece to create
     * @return the constructed ItemStack
     */
    public ItemStack createPiece(Piece piece) {
        return cachedPieces.computeIfAbsent(piece, entry -> {
            ItemStack stack = new ItemStack(entry.material());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(entry.displayName()));
                meta.lore(BASE_LORE.stream().map(Component::text).toList());
                meta.getPersistentDataContainer()
                    .set(keys.radiationSuitKey, PersistentDataType.STRING, entry.id());
                if (meta instanceof LeatherArmorMeta leatherMeta) {
                    leatherMeta.setColor(SUIT_COLOR);
                }
                stack.setItemMeta(meta);
            }
            return stack;
        }).clone();
    }

    /**
     * Returns {@code true} when the provided item is the requested radiation suit piece.
     *
     * @param stack the item to inspect
     * @param piece the expected radiation suit piece
     * @return {@code true} when the item matches the radiation suit piece
     */
    public boolean isPiece(ItemStack stack, Piece piece) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        String id = meta.getPersistentDataContainer().get(keys.radiationSuitKey, PersistentDataType.STRING);
        return piece.id().equals(id);
    }

    /**
     * Returns {@code true} if the player is wearing the full radiation suit.
     *
     * @param player the player to check
     * @return {@code true} when all armor slots contain the radiation suit pieces
     */
    public boolean isFullyProtected(Player player) {
        ItemStack helmet = player.getInventory().getItem(EquipmentSlot.HEAD);
        ItemStack chest = player.getInventory().getItem(EquipmentSlot.CHEST);
        ItemStack legs = player.getInventory().getItem(EquipmentSlot.LEGS);
        ItemStack boots = player.getInventory().getItem(EquipmentSlot.FEET);
        return isPiece(helmet, Piece.HELMET)
            && isPiece(chest, Piece.CHESTPLATE)
            && isPiece(legs, Piece.LEGGINGS)
            && isPiece(boots, Piece.BOOTS);
    }

    /**
     * Registers crafting recipes for the radiation suit pieces.
     */
    public void registerRecipes() {
        registerHelmetRecipe();
        registerChestplateRecipe();
        registerLeggingsRecipe();
        registerBootsRecipe();
    }

    private void registerHelmetRecipe() {
        ItemStack result = createPiece(Piece.HELMET);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "radiation_helmet"), result);
        recipe.shape("LGL", "L L", "   ");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('G', Material.GLOWSTONE_DUST);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerChestplateRecipe() {
        ItemStack result = createPiece(Piece.CHESTPLATE);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "radiation_chestplate"), result);
        recipe.shape("LGL", "LLL", "LLL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('G', Material.GLOWSTONE_DUST);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerLeggingsRecipe() {
        ItemStack result = createPiece(Piece.LEGGINGS);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "radiation_leggings"), result);
        recipe.shape("LLL", "LGL", "L L");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('G', Material.GLOWSTONE_DUST);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerBootsRecipe() {
        ItemStack result = createPiece(Piece.BOOTS);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "radiation_boots"), result);
        recipe.shape("   ", "L L", "LGL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('G', Material.GLOWSTONE_DUST);
        plugin.getServer().addRecipe(recipe);
    }

    /**
     * Attempts to match a radiation suit piece by identifier.
     *
     * @param id the identifier to parse
     * @return the matching radiation suit piece, if any
     */
    public Optional<Piece> pieceFromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (Piece piece : Piece.values()) {
            if (piece.id().equals(normalized)) {
                return Optional.of(piece);
            }
        }
        return Optional.empty();
    }

    /**
     * Represents the supported radiation suit pieces.
     */
    public enum Piece {
        HELMET("radiation_helmet", "Radiation Suit Helmet", Material.LEATHER_HELMET),
        CHESTPLATE("radiation_chestplate", "Radiation Suit Chestplate", Material.LEATHER_CHESTPLATE),
        LEGGINGS("radiation_leggings", "Radiation Suit Leggings", Material.LEATHER_LEGGINGS),
        BOOTS("radiation_boots", "Radiation Suit Boots", Material.LEATHER_BOOTS);

        private final String id;
        private final String displayName;
        private final Material material;

        Piece(String id, String displayName, Material material) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
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
    }
}
