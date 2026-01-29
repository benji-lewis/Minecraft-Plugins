package uk.co.xfour.kimjongun3;

import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class KimJongUnListener implements Listener {
    private final KimJongUn3Plugin plugin;
    private final KimJongUnItems items;
    private final LaunchManager launchManager;
    private final IcbmTargetingManager targetingManager;

    public KimJongUnListener(KimJongUn3Plugin plugin, KimJongUnItems items, LaunchManager launchManager,
                             IcbmTargetingManager targetingManager) {
        this.plugin = plugin;
        this.items = items;
        this.launchManager = launchManager;
        this.targetingManager = targetingManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getPersistentDataContainer().has(items.keys().mobKey, PersistentDataType.BYTE)) {
            return;
        }
        event.getDrops().clear();
        int drops = plugin.getConfig().getInt("drops.parts-per-kill", 1);
        for (int i = 0; i < drops; i++) {
            ItemStack part = items.createItem(items.randomPart());
            entity.getWorld().dropItemNaturally(entity.getLocation(), part);
        }
        double icbmChance = plugin.getConfig().getDouble("drops.icbm-core-chance", 0.15);
        if (Math.random() <= icbmChance) {
            ItemStack core = items.createItem(KimJongUnItems.KimJongUnItem.ICBM_CORE);
            entity.getWorld().dropItemNaturally(entity.getLocation(), core);
        }
    }

    @EventHandler
    public void onLaunchpadPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("kimjongun3.use")) {
            return;
        }
        Optional<KimJongUnItems.KimJongUnItem> itemType = items.identify(event.getItem());
        if (itemType.isEmpty() || itemType.get() != KimJongUnItems.KimJongUnItem.LAUNCHPAD) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        Location location = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        ArmorStand stand = launchManager.spawnLaunchpad(location);
        if (stand == null) {
            return;
        }
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack stack = event.getItem();
            stack.setAmount(stack.getAmount() - 1);
        }
    }

    @EventHandler
    public void onLaunchpadInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }
        ArmorStand stand = (ArmorStand) entity;
        if (!launchManager.isLaunchpad(stand)) {
            return;
        }
        if (!event.getPlayer().hasPermission("kimjongun3.use")) {
            return;
        }
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        Optional<KimJongUnItems.KimJongUnItem> itemType = items.identify(held);
        if (itemType.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (itemType.get() == KimJongUnItems.KimJongUnItem.ICBM) {
            targetingManager.openTargeting(event.getPlayer(), stand);
            return;
        }
        if (itemType.get() == KimJongUnItems.KimJongUnItem.MISSILE) {
            launchManager.launchMissile(stand.getLocation(), event.getPlayer().getLocation().getDirection());
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                held.setAmount(held.getAmount() - 1);
            }
        }
    }
}
