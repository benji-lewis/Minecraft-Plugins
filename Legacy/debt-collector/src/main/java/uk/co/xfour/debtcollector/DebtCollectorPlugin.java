package uk.co.xfour.debtcollector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DebtCollectorPlugin extends JavaPlugin implements Listener {
    private static final double DEBT_THRESHOLD = 0.0;
    private static final int CHECK_INTERVAL_TICKS = 20 * 5;

    private final Map<UUID, UUID> activeCollectors = new HashMap<>();
    private final Random random = new Random();
    private final List<String> collectorNames = Arrays.asList(
            "Morgan",
            "Ellis",
            "Taylor",
            "Parker",
            "Reed",
            "Hayes",
            "Blake",
            "Mason",
            "Quinn",
            "Bennett"
    );

    private Object economy;
    private Method getBalanceMethod;
    private NamespacedKey collectorKey;

    @Override
    public void onEnable() {
        collectorKey = new NamespacedKey(this, "debt_collector");

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, 40L, CHECK_INTERVAL_TICKS);
    }

    @Override
    public void onDisable() {
        activeCollectors.values().forEach(this::removeCollectorById);
        activeCollectors.clear();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        Class<?> economyClass;
        try {
            economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException exception) {
            getLogger().warning("Vault API not found on the classpath.");
            return false;
        }

        RegisteredServiceProvider<?> provider = getServer().getServicesManager().getRegistration(economyClass);
        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        if (economy == null) {
            return false;
        }

        try {
            getBalanceMethod = economy.getClass().getMethod("getBalance", OfflinePlayer.class);
        } catch (NoSuchMethodException exception) {
            getLogger().warning("Vault economy provider does not expose getBalance(OfflinePlayer).");
            return false;
        }

        return true;
    }

    private void checkPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double balance = getBalance(player);
            if (balance < DEBT_THRESHOLD) {
                ensureCollector(player);
            } else {
                removeCollector(player.getUniqueId());
            }
        }
    }

    private void ensureCollector(Player player) {
        UUID existingCollectorId = activeCollectors.get(player.getUniqueId());
        if (existingCollectorId != null) {
            Entity existing = Bukkit.getEntity(existingCollectorId);
            if (existing instanceof Zombie zombie && !zombie.isDead()) {
                zombie.setTarget(player);
                return;
            }
        }

        Zombie collector = (Zombie) player.getWorld().spawnEntity(spawnNear(player.getLocation()), EntityType.ZOMBIE);
        collector.setCustomNameVisible(true);
        collector.setCustomName(ChatColor.RED + "Debt Collector " + randomName());
        collector.setAdult();
        collector.setBaby(false);
        collector.setCanPickupItems(false);
        collector.setRemoveWhenFarAway(false);

        AttributeInstance followRange = collector.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(40.0);
        }

        AttributeInstance attackDamage = collector.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(6.0);
        }

        equipSuit(collector);
        collector.setTarget(player);
        collector.getPersistentDataContainer().set(collectorKey, PersistentDataType.BYTE, (byte) 1);

        activeCollectors.put(player.getUniqueId(), collector.getUniqueId());
    }

    private void equipSuit(Zombie collector) {
        EntityEquipment equipment = collector.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack helmet = dyedLeather(org.bukkit.Material.LEATHER_HELMET, Color.fromRGB(20, 20, 20));
        ItemStack chestplate = dyedLeather(org.bukkit.Material.LEATHER_CHESTPLATE, Color.fromRGB(30, 30, 30));
        ItemStack leggings = dyedLeather(org.bukkit.Material.LEATHER_LEGGINGS, Color.fromRGB(25, 25, 25));
        ItemStack boots = dyedLeather(org.bukkit.Material.LEATHER_BOOTS, Color.fromRGB(15, 15, 15));

        equipment.setArmorContents(new ItemStack[]{boots, leggings, chestplate, helmet});
        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
    }

    private ItemStack dyedLeather(org.bukkit.Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Location spawnNear(Location location) {
        double offsetX = (random.nextDouble() * 6.0) - 3.0;
        double offsetZ = (random.nextDouble() * 6.0) - 3.0;
        return location.clone().add(offsetX, 0.0, offsetZ);
    }

    private String randomName() {
        return collectorNames.get(random.nextInt(collectorNames.size()));
    }

    private void removeCollector(UUID playerId) {
        UUID collectorId = activeCollectors.remove(playerId);
        if (collectorId != null) {
            removeCollectorById(collectorId);
        }
    }

    private void removeCollectorById(UUID collectorId) {
        Entity entity = Bukkit.getEntity(collectorId);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    private double getBalance(OfflinePlayer player) {
        if (economy == null || getBalanceMethod == null) {
            return 0.0;
        }

        try {
            Object result = getBalanceMethod.invoke(economy, player);
            if (result instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException exception) {
            getLogger().severe("Failed to query Vault economy balance. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }

        return 0.0;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeCollector(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCollectorDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }

        PersistentDataContainer container = zombie.getPersistentDataContainer();
        if (!container.has(collectorKey, PersistentDataType.BYTE)) {
            return;
        }

        activeCollectors.entrySet().removeIf(entry -> entry.getValue().equals(zombie.getUniqueId()));
    }
}
