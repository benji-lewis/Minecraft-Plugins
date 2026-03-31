package com.xfour.creeperconsent.util;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import com.xfour.creeperconsent.CreeperConsent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreeperManager {

    // Track pending creepers: UUID (creeper) -> UUID (player)
    private final Map<UUID, UUID> pendingCreepers = new HashMap<>();

    // Track last damager: UUID (creeper) -> Entity
    private final Map<UUID, Entity> damagerMap = new HashMap<>();

    public void setPending(Creeper creeper, Player player) {
        pendingCreepers.put(creeper.getUniqueId(), player.getUniqueId());
    }

    public void removePending(Creeper creeper) {
        pendingCreepers.remove(creeper.getUniqueId());
        damagerMap.remove(creeper.getUniqueId());
    }

    public boolean isPending(Creeper creeper) {
        return pendingCreepers.containsKey(creeper.getUniqueId());
    }

    public Player getPendingPlayer(Creeper creeper) {
        UUID playerUUID = pendingCreepers.get(creeper.getUniqueId());
        if (playerUUID != null) {
            return org.bukkit.Bukkit.getPlayer(playerUUID);
        }
        return null;
    }

    public void setDamager(Creeper creeper, Entity damager) {
        damagerMap.put(creeper.getUniqueId(), damager);
    }

    public Entity getDamager(Creeper creeper) {
        return damagerMap.get(creeper.getUniqueId());
    }

    public void cleanup(Creeper creeper) {
        removePending(creeper);
    }
}
