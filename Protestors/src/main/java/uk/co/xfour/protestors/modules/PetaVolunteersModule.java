package uk.co.xfour.protestors.modules;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * PETA volunteers module logic.
 */
public class PetaVolunteersModule implements Module, Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private NamespacedKey volunteerKey;

    public PetaVolunteersModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        volunteerKey = new NamespacedKey(plugin, "peta_volunteer");
    }

    @Override
    public void stop() {
    }

    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals) || event.getEntity().getKiller() == null) return;
        double chance = plugin.getConfig().getDouble("modules.peta-volunteers.spawn-chance", 0.3);
        if (random.nextDouble() > chance) return;
        Villager villager = event.getEntity().getWorld().spawn(event.getEntity().getLocation(), Villager.class);
        villager.setCustomName(ChatColor.LIGHT_PURPLE + "PETA Volunteer");
        villager.setCustomNameVisible(true);
        villager.getPersistentDataContainer().set(volunteerKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onVolunteerTrade(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager villager) {
            Byte value = villager.getPersistentDataContainer().get(volunteerKey, PersistentDataType.BYTE);
            if (value != null && value == 1) {
                event.setCancelled(true);
                for (Player player : villager.getLocation().getNearbyPlayers(14.0)) {
                    player.sendMessage(ChatColor.RED + "[PETA]" + ChatColor.YELLOW + " Stop harming animals!");
                }
            }
        }
    }
}
