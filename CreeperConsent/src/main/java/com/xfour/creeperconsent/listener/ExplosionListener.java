package com.xfour.creeperconsent.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import com.xfour.creeperconsent.CreeperConsent;
import com.xfour.creeperconsent.gui.ExplosionPromptGUI;
import com.xfour.creeperconsent.util.CreeperManager;

public class ExplosionListener implements Listener {

    private final CreeperConsent plugin;
    private final ExplosionPromptGUI gui;
    private final CreeperManager creeperManager;

    public ExplosionListener(CreeperConsent plugin) {
        this.plugin = plugin;
        this.gui = new ExplosionPromptGUI(plugin);
        this.creeperManager = plugin.getCreeperManager();
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        // Only handle creepers
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }

        // Get the entity that damaged the creeper (triggering the fuse)
        Entity damager = null;

        // Try to get damager from the creeper
        // Note: In Paper 1.21.1, we need to check if there's a way to get the damager
        // If not directly available, we'll need to track it manually via damage listener
        // For now, we'll attempt to get the last damager if available via custom metadata

        // Check if we have a tracked damager for this creeper
        damager = creeperManager.getDamager(creeper);

        if (damager == null || !(damager instanceof Player player)) {
            // No player triggered this, let it explode naturally
            return;
        }

        // Cancel the explosion and show the GUI
        event.setCancelled(true);
        gui.openPrompt(player, creeper);

        // Track this creeper as pending response
        creeperManager.setPending(creeper, player);

        plugin.getLogger().fine("Creeper explosion prompt shown to " + player.getName());
    }
}
