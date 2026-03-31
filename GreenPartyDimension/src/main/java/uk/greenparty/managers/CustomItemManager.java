package uk.greenparty.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * CustomItemManager — the sacred artefacts of the Green Party.
 *
 * Three items of power, forged in the fires of bureaucratic passion:
 * 1. The Manifesto — contains policy. Too much policy. Pages and pages.
 * 2. The Green Party Badge — a diamond, dyed green in spirit if not in chemistry.
 * 3. The Compost Ceremonial Spoon — forged in compost. Enchanted. We don't ask why.
 *
 * These items serve no mechanical purpose. That is intentional.
 * They serve ideological purposes. Far more important.
 */
public class CustomItemManager {

    private final GreenPartyPlugin plugin;

    // Persistent data keys for item identification
    private final NamespacedKey keyManifesto;
    private final NamespacedKey keyBadge;
    private final NamespacedKey keySpoon;

    public CustomItemManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        keyManifesto = new NamespacedKey(plugin, "green_manifesto");
        keyBadge     = new NamespacedKey(plugin, "green_badge");
        keySpoon     = new NamespacedKey(plugin, "compost_spoon");

        registerCraftingRecipes();
    }

    // ─── Item Creators ────────────────────────────────────────────────────────

    /**
     * Creates the Green Party Manifesto — a written book filled with policy.
     * Reading it is optional. Carrying it is mandatory.
     */
    public ItemStack createManifesto() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();

        meta.setTitle("§2The Green Party Manifesto");
        meta.setAuthor("The Green Council (17 signatories)");

        // Page 1
        meta.addPage(
            "§2§lTHE GREEN PARTY\nDIMENSION MANIFESTO\n\n§r§7Edition 14\n(Revised)\n(Revised again)\n(We added more policies)"
        );

        // Page 2
        meta.addPage(
            "§2§lPolicy 1:§r\n§7Everything shall be green.\n\nAll blocks not already green shall submit a colouring proposal to the Aesthetic Committee within 30 days."
        );

        // Page 3
        meta.addPage(
            "§2§lPolicy 2:§r\n§7Coal is hereby banned.\n\nAlternatives:\n- Emeralds (symbolic)\n- Leaves (vibe)\n- Nothing (preferred)"
        );

        // Page 4
        meta.addPage(
            "§2§lPolicy 3:§r\n§7All meat blocks shall be renamed 'Legacy Protein'.\n\nThis motion passed 8-7. Councillor Fern abstained on composting grounds."
        );

        // Page 5
        meta.addPage(
            "§2§lPolicy 4:§r\n§7Creepers are a protected species.\n\n§cWe accept no liability for what happens if you stand next to one."
        );

        // Page 6
        meta.addPage(
            "§2§lPolicy 5:§r\n§7TNT requires a full environmental impact assessment.\n\nForms available from Councillor Wheatgrass. 47 pages. Minimum 6 weeks processing."
        );

        // Page 7
        meta.addPage(
            "§2§lPolicy 6:§r\n§7The Nether is banned.\n\nToo hot. Environmentally reckless. Smells of sulphur. The motion passed unanimously."
        );

        // Page 8
        meta.addPage(
            "§2§lClosing Statement:§r\n\n§7We believe in a greener world. A more composted world. A world where every block has value and every tree has a committee.\n\n§2Vote Green. Always."
        );

        // Lore
        meta.setLore(Arrays.asList(
            "§8The sacred texts of the Green Council",
            "§8Contains 47 policies. Possibly 48.",
            "§8The ink is eco-certified.",
            "§8Do not lose this. We have a tracking system."
        ));

        // Tag as custom item
        meta.getPersistentDataContainer().set(keyManifesto, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the Green Party Badge — a diamond imbued with the spirit of green.
     * Not actually green. The dye didn't take. We're working on it.
     */
    public ItemStack createGreenPartyBadge() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§2§l★ Green Party Badge ★");
        meta.setLore(Arrays.asList(
            "§8Awarded to distinguished members",
            "§8of the Green Party Dimension Council.",
            "§7Wear with pride. Or don't.",
            "§7The council will notice either way.",
            "",
            "§2Eco-Certified §8| §aAuthentic Green™",
            "§8Serial No: GP-" + (int)(Math.random() * 9000 + 1000)
        ));

        // Make it glow because why not
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Tag
        meta.getPersistentDataContainer().set(keyBadge, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the Compost Ceremonial Spoon — the most powerful item in the dimension.
     * (It isn't. It's a wooden shovel. But it has MEANING.)
     */
    public ItemStack createCompostCeremonialSpoon() {
        ItemStack item = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§2§l⚗ Compost Ceremonial Spoon ⚗");
        meta.setLore(Arrays.asList(
            "§8Blessed by the Elder Composting Sage",
            "§8during the Seventh Annual Compost Festival.",
            "",
            "§7Used in all official ceremonies.",
            "§7Do not eat with this spoon.",
            "§7(It has been composted. Twice.)",
            "",
            "§6§lLegendary Compost Artefact",
            "§8Enchanted: Efficiency, Unbreaking, Silk Touch"
        ));

        // Ceremonially enchanted
        meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 5, true);
        meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);

        // Tag
        meta.getPersistentDataContainer().set(keySpoon, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    // ─── Identification ───────────────────────────────────────────────────────

    public boolean isManifesto(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyManifesto, PersistentDataType.BYTE);
    }

    public boolean isGreenPartyBadge(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyBadge, PersistentDataType.BYTE);
    }

    public boolean isCompostSpoon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keySpoon, PersistentDataType.BYTE);
    }

    public boolean isCustomItem(ItemStack item) {
        return isManifesto(item) || isGreenPartyBadge(item) || isCompostSpoon(item);
    }

    // ─── Crafting Recipes ─────────────────────────────────────────────────────

    private void registerCraftingRecipes() {
        int registered = 0;

        // ── Manifesto ──────────────────────────────────────────────────────────
        // P = paper, I = ink sac
        //  P
        // PIP
        //  P
        NamespacedKey manifestoKey = new NamespacedKey(plugin, "crafting_manifesto");
        ShapedRecipe manifestoRecipe = new ShapedRecipe(manifestoKey, createManifesto());
        manifestoRecipe.shape(
            " P ",
            "PIP",
            " P "
        );
        manifestoRecipe.setIngredient('P', Material.PAPER);
        manifestoRecipe.setIngredient('I', Material.INK_SAC);

        try {
            plugin.getServer().addRecipe(manifestoRecipe);
            plugin.getLogger().info("[CustomItems] ✔ Registered crafting recipe: Green Party Manifesto");
            plugin.getLogger().info("[CustomItems]   Shape:  P  /  PIP  /  P  (P=Paper, I=Ink Sac)");
            registered++;
        } catch (Exception e) {
            plugin.getLogger().warning("[CustomItems] ✘ Could not register Manifesto recipe: " + e.getMessage());
        }

        // ── Green Party Badge ──────────────────────────────────────────────────
        // G = green dye, D = diamond
        // GGG
        // GDG
        // GGG
        NamespacedKey badgeKey = new NamespacedKey(plugin, "crafting_badge");
        ShapedRecipe badgeRecipe = new ShapedRecipe(badgeKey, createGreenPartyBadge());
        badgeRecipe.shape(
            "GGG",
            "GDG",
            "GGG"
        );
        badgeRecipe.setIngredient('G', Material.GREEN_DYE);
        badgeRecipe.setIngredient('D', Material.DIAMOND);

        try {
            plugin.getServer().addRecipe(badgeRecipe);
            plugin.getLogger().info("[CustomItems] ✔ Registered crafting recipe: Green Party Badge");
            plugin.getLogger().info("[CustomItems]   Shape: GGG / GDG / GGG  (G=Green Dye, D=Diamond)");
            registered++;
        } catch (Exception e) {
            plugin.getLogger().warning("[CustomItems] ✘ Could not register Badge recipe: " + e.getMessage());
        }

        // ── Compost Ceremonial Spoon ───────────────────────────────────────────
        // P = oak planks (bowl), S = stick (handle)
        // PPP
        //  S
        //  S
        NamespacedKey spoonKey = new NamespacedKey(plugin, "crafting_spoon");
        ShapedRecipe spoonRecipe = new ShapedRecipe(spoonKey, createCompostCeremonialSpoon());
        spoonRecipe.shape(
            "PPP",
            " S ",
            " S "
        );
        spoonRecipe.setIngredient('P', Material.OAK_PLANKS);
        spoonRecipe.setIngredient('S', Material.STICK);

        try {
            plugin.getServer().addRecipe(spoonRecipe);
            plugin.getLogger().info("[CustomItems] ✔ Registered crafting recipe: Compost Ceremonial Spoon");
            plugin.getLogger().info("[CustomItems]   Shape: PPP /  S  /  S   (P=Oak Planks, S=Stick)");
            registered++;
        } catch (Exception e) {
            plugin.getLogger().warning("[CustomItems] ✘ Could not register Spoon recipe: " + e.getMessage());
        }

        plugin.getLogger().info("[CustomItems] Recipe registration complete: " + registered + "/3 recipes registered.");
        if (registered < 3) {
            plugin.getLogger().warning("[CustomItems] WARNING: Some recipes failed to register! Custom items may not be craftable.");
        }
    }
}
