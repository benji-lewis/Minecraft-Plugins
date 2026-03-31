package com.xfour.creeperconsent.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.xfour.creeperconsent.CreeperConsent;
import com.xfour.creeperconsent.config.ConfigManager;

public class ExplosionPromptGUI {

    private static final String GUI_TITLE_PREFIX = "§6§lCreeper Consent";
    private static final int ACCEPT_SLOT = 3;  // Left side
    private static final int DENY_SLOT = 5;    // Right side

    private final CreeperConsent plugin;
    private final ConfigManager config;

    public ExplosionPromptGUI(CreeperConsent plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * Opens an explosion prompt GUI for a player regarding a specific creeper
     */
    public void openPrompt(Player player, Creeper creeper) {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE_PREFIX);

        // Accept button (green wool)
        ItemStack acceptItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName("§a" + config.getAcceptMessage());
            acceptItem.setItemMeta(acceptMeta);
        }
        inv.setItem(ACCEPT_SLOT, acceptItem);

        // Deny button (red wool)
        ItemStack denyItem = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = denyItem.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName("§c" + config.getDenyMessage());
            denyItem.setItemMeta(denyMeta);
        }
        inv.setItem(DENY_SLOT, denyItem);

        // Filler (gray wool)
        ItemStack filler = new ItemStack(Material.GRAY_WOOL);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName("§7");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 9; i++) {
            if (i != ACCEPT_SLOT && i != DENY_SLOT) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    /**
     * Check if this is a CreeperConsent GUI (for click detection)
     */
    public static boolean isCreeperConsentGUI(Inventory inv) {
        return inv.getSize() == 9 && inv.getName().contains("Creeper Consent");
    }

    /**
     * Get the slot constant for the accept button
     */
    public static int getAcceptSlot() {
        return ACCEPT_SLOT;
    }

    /**
     * Get the slot constant for the deny button
     */
    public static int getDenySlot() {
        return DENY_SLOT;
    }
}
