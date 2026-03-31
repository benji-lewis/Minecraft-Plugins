package com.xfour.creeperconsent.listener;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import com.xfour.creeperconsent.util.CreeperManager;

public class DamageListener implements Listener {

    private final CreeperManager creeperManager;

    public DamageListener(CreeperManager creeperManager) {
        this.creeperManager = creeperManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity damagedEntity = event.getEntity();

        // Track who damaged creepers (for identifying the triggerer)
        if (damagedEntity instanceof Creeper creeper) {
            Entity damager = event.getDamager();
            if (damager != null) {
                creeperManager.setDamager(creeper, damager);
            }
        }
    }
}
