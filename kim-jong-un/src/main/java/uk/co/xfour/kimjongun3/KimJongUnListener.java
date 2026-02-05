package uk.co.xfour.kimjongun3;

import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class KimJongUnListener implements Listener {
    private final KimJongUn3Plugin plugin;
    private final KimJongUnItems items;
    private final KimJongUnBlocks blocks;
    private final LaunchManager launchManager;
    private final IcbmTargetingManager targetingManager;

    public KimJongUnListener(KimJongUn3Plugin plugin, KimJongUnItems items, KimJongUnBlocks blocks,
                             LaunchManager launchManager, IcbmTargetingManager targetingManager) {
        this.plugin = plugin;
        this.items = items;
        this.blocks = blocks;
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
    public void onLaunchpadInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        if (!event.getPlayer().hasPermission("kimjongun3.use")) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        Optional<KimJongUnItems.KimJongUnItem> itemType = items.identify(held);
        Block clickedBlock = event.getClickedBlock();
        Location blockLocation = clickedBlock.getLocation();
        if (itemType.isPresent() && itemType.get() == KimJongUnItems.KimJongUnItem.LAUNCHPAD) {
            Block targetBlock = clickedBlock.getRelative(event.getBlockFace());
            if (targetBlock.isReplaceable()) {
                Location targetLocation = targetBlock.getLocation();
                if (blocks.placeLaunchpad(targetLocation, event.getPlayer())) {
                    event.setCancelled(true);
                    if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                        held.setAmount(held.getAmount() - 1);
                    }
                    return;
                }
            }
        }
        if (!blocks.isLaunchpad(blockLocation)) {
            return;
        }
        if (itemType.isEmpty()) {
            return;
        }
        if (itemType.get() == KimJongUnItems.KimJongUnItem.MISSILE
            || itemType.get() == KimJongUnItems.KimJongUnItem.ICBM) {
            Block mountBlock = clickedBlock.getRelative(BlockFace.UP);
            Location mountLocation = mountBlock.getLocation();
            if (mountBlock.isReplaceable() && !blocks.hasPlacedBlock(mountLocation)) {
                boolean placed = itemType.get() == KimJongUnItems.KimJongUnItem.ICBM
                    ? blocks.placeIcbm(mountLocation, event.getPlayer())
                    : blocks.placeMissile(mountLocation, event.getPlayer());
                if (placed) {
                    event.setCancelled(true);
                    if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                        held.setAmount(held.getAmount() - 1);
                    }
                    return;
                }
            }
        }
        event.setCancelled(true);
        Location launchLocation = blockLocation.clone().add(0.5, 0.0, 0.5);
        if (itemType.get() == KimJongUnItems.KimJongUnItem.ICBM) {
            targetingManager.openTargeting(event.getPlayer(), blockLocation);
            return;
        }
        if (itemType.get() == KimJongUnItems.KimJongUnItem.MISSILE) {
            Vector direction = event.getPlayer().getLocation().getDirection();
            launchManager.launchMissile(launchLocation, direction);
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                held.setAmount(held.getAmount() - 1);
            }
        }
    }
}
