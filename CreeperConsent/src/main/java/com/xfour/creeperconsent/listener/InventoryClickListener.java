package com.xfour.creeperconsent.listener;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import com.xfour.creeperconsent.CreeperConsent;
import com.xfour.creeperconsent.gui.ExplosionPromptGUI;
import com.xfour.creeperconsent.util.CreeperManager;

public class InventoryClickListener implements Listener {

    private final CreeperConsent plugin;
    private final CreeperManager creeperManager;

    public InventoryClickListener(CreeperConsent plugin, CreeperManager creeperManager) {
        this.plugin = plugin;
        this.creeperManager = creeperManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle CreeperConsent GUIs
        if (!ExplosionPromptGUI.isCreeperConsentGUI(event.getInventory())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == ExplosionPromptGUI.getAcceptSlot()) {
            // Player clicked "Sure, let's go" - allow explosion
            handleAccept(player);
        } else if (slot == ExplosionPromptGUI.getDenySlot()) {
            // Player clicked "Nah, not today" - despawn creeper
            handleDeny(player);
        }

        player.closeInventory();
    }

    /**
     * Handle accept: find the pending creeper and allow it to explode
     */
    private void handleAccept(Player player) {
        Creeper pendingCreeper = findPendingCreeper(player);
        if (pendingCreeper != null && pendingCreeper.isValid()) {
            // Ignite the creeper to explode
            pendingCreeper.setFuseTicks(0);
            creeperManager.removePending(pendingCreeper);
            plugin.getLogger().fine(player.getName() + " allowed creeper to explode.");
        }
    }

    /**
     * Handle deny: find the pending creeper and despawn it
     */
    private void handleDeny(Player player) {
        Creeper pendingCreeper = findPendingCreeper(player);
        if (pendingCreeper != null && pendingCreeper.isValid()) {
            // Remove the creeper
            pendingCreeper.remove();
            creeperManager.removePending(pendingCreeper);
            plugin.getLogger().fine(player.getName() + " denied creeper explosion. Creeper despawned.");
        }
    }

    /**
     * Find the pending creeper for this player
     */
    private Creeper findPendingCreeper(Player player) {
        for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof Creeper creeper && creeperManager.isPending(creeper)) {
                if (creeperManager.getPendingPlayer(creeper) == player) {
                    return creeper;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // If player closes the GUI without clicking, creeper stays pending indefinitely
        // This is the desired behavior per spec: "If player doesn't respond, creeper hangs indefinitely"
        if (ExplosionPromptGUI.isCreeperConsentGUI(event.getInventory())) {
            plugin.getLogger().fine(event.getPlayer().getName() + " closed the creeper consent GUI without responding.");
        }
    }
}
