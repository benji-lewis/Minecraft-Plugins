package uk.co.xfour.protestors.modules;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Debt collector module logic.
 */
public class DebtCollectorModule implements Module, Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, UUID> activeCollectors = new HashMap<>();
    private final Random random = new Random();
    private NamespacedKey collectorKey;

    public DebtCollectorModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        collectorKey = new NamespacedKey(plugin, "debt_collector");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkPlayers, 60L, 100L);
    }

    @Override
    public void stop() {
        activeCollectors.values().forEach(this::removeCollectorById);
        activeCollectors.clear();
    }

    private void checkPlayers() {
        double threshold = plugin.getConfig().getDouble("modules.debt-collector.mock-balance-threshold", 5.0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLevel() < threshold) {
                ensureCollector(player);
            } else {
                removeCollector(player.getUniqueId());
            }
        }
    }

    private void ensureCollector(Player player) {
        UUID existingId = activeCollectors.get(player.getUniqueId());
        if (existingId != null) {
            Entity entity = Bukkit.getEntity(existingId);
            if (entity instanceof Zombie zombie && !zombie.isDead()) {
                zombie.setTarget(player);
                return;
            }
        }
        Zombie collector = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        collector.setCustomName(ChatColor.RED + "Debt Collector #" + (random.nextInt(900) + 100));
        collector.setCustomNameVisible(true);
        AttributeInstance followRange = collector.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(40.0);
        collector.setTarget(player);
        collector.getPersistentDataContainer().set(collectorKey, PersistentDataType.BYTE, (byte) 1);
        activeCollectors.put(player.getUniqueId(), collector.getUniqueId());
    }

    private void removeCollector(UUID playerId) {
        UUID collectorId = activeCollectors.remove(playerId);
        if (collectorId != null) removeCollectorById(collectorId);
    }

    private void removeCollectorById(UUID collectorId) {
        Entity entity = Bukkit.getEntity(collectorId);
        if (entity != null && !entity.isDead()) entity.remove();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeCollector(event.getPlayer().getUniqueId());
    }
}
