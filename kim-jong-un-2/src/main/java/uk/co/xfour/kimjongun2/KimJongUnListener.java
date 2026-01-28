package uk.co.xfour.kimjongun2;

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
import org.bukkit.util.Vector;

public class KimJongUnListener implements Listener {
    private final KimJongUn2Plugin plugin;
    private final KimJongUnItems items;
    private final LaunchManager launchManager;

    public KimJongUnListener(KimJongUn2Plugin plugin, KimJongUnItems items, LaunchManager launchManager) {
        this.plugin = plugin;
        this.items = items;
        this.launchManager = launchManager;
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
    }

    @EventHandler
    public void onLaunchpadPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("kimjongun2.use")) {
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
        if (!event.getPlayer().hasPermission("kimjongun2.use")) {
            return;
        }
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        Optional<KimJongUnItems.KimJongUnItem> itemType = items.identify(held);
        if (itemType.isEmpty() || itemType.get() != KimJongUnItems.KimJongUnItem.MISSILE) {
            return;
        }
        event.setCancelled(true);
        Vector direction = event.getPlayer().getLocation().getDirection();
        launchManager.launchMissile(stand.getLocation(), direction);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            held.setAmount(held.getAmount() - 1);
        }
    }
}
