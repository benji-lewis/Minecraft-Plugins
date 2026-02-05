package uk.co.xfour.kimjongun;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/**
 * Manages the ICBM targeting user interface and launch confirmation.
 */
public class IcbmTargetingManager implements Listener {
    private static final int SLOT_TARGET_X = 10;
    private static final int SLOT_TARGET_Y = 12;
    private static final int SLOT_TARGET_Z = 14;
    private static final int SLOT_USE_CURRENT = 16;
    private static final int SLOT_LAUNCH = 22;
    private static final int SLOT_CANCEL = 26;

    private final KimJongUnPlugin plugin;
    private final KimJongUnItems items;
    private final KimJongUnBlocks blocks;
    private final LaunchManager launchManager;
    private final Map<UUID, TargetingSession> sessions = new HashMap<>();

    public IcbmTargetingManager(KimJongUnPlugin plugin, KimJongUnItems items, KimJongUnBlocks blocks,
                                LaunchManager launchManager) {
        this.plugin = plugin;
        this.items = items;
        this.blocks = blocks;
        this.launchManager = launchManager;
    }

    /**
     * Opens the targeting interface for the given player and launchpad.
     *
     * @param player the player initiating the launch
     * @param launchpadLocation the launchpad block location
     */
    public void openTargeting(Player player, Location launchpadLocation) {
        if (!plugin.getConfig().getBoolean("icbm.enabled", true)) {
            Vector direction = player.getLocation().getDirection();
            Location launchLocation = launchpadLocation.clone().add(0.5, 0.0, 0.5);
            launchManager.launchMissile(launchLocation, direction);
            consumeMissile(player);
            return;
        }
        TargetingSession session = new TargetingSession(player, launchpadLocation);
        sessions.put(player.getUniqueId(), session);
        player.openInventory(session.inventory);
    }

    /**
     * Closes any open targeting sessions.
     */
    public void shutdown() {
        for (TargetingSession session : sessions.values()) {
            session.player.closeInventory();
        }
        sessions.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof TargetingHolder holder)) {
            return;
        }
        TargetingSession session = sessions.get(holder.playerId);
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == SLOT_LAUNCH) {
            handleLaunch(player, session);
            return;
        }
        if (slot == SLOT_CANCEL) {
            closeSession(player);
            return;
        }
        if (slot == SLOT_USE_CURRENT) {
            session.setTarget(player.getLocation());
            session.refreshInventory();
            return;
        }
        if (slot == SLOT_TARGET_X) {
            session.adjustTarget(TargetAxis.X, adjustmentForClick(event));
            session.refreshInventory();
        } else if (slot == SLOT_TARGET_Y) {
            session.adjustTarget(TargetAxis.Y, adjustmentForClick(event));
            session.refreshInventory();
        } else if (slot == SLOT_TARGET_Z) {
            session.adjustTarget(TargetAxis.Z, adjustmentForClick(event));
            session.refreshInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof TargetingHolder)) {
            return;
        }
        sessions.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void handleLaunch(Player player, TargetingSession session) {
        if (!consumeMissile(player)) {
            player.sendMessage("You must hold an assembled ICBM to launch.");
            return;
        }
        if (!blocks.isLaunchpad(session.launchpadLocation)) {
            player.sendMessage("The launchpad is no longer available.");
            closeSession(player);
            return;
        }
        Location launchLocation = session.launchpadLocation.clone().add(0.5, 0.0, 0.5);
        launchManager.launchIcbm(launchLocation, session.targetLocation());
        closeSession(player);
    }

    private boolean consumeMissile(Player player) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        return items.identify(held)
            .filter(item -> item == KimJongUnItems.KimJongUnItem.ICBM)
            .map(item -> {
                held.setAmount(held.getAmount() - 1);
                return true;
            })
            .orElse(false);
    }

    private void closeSession(Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private int adjustmentForClick(InventoryClickEvent event) {
        int delta = event.isShiftClick() ? 10 : 1;
        if (event.isRightClick()) {
            delta = -delta;
        }
        return delta;
    }

    private enum TargetAxis {
        X, Y, Z
    }

    private final class TargetingSession {
        private final Player player;
        private final Location launchpadLocation;
        private final World world;
        private int targetX;
        private int targetY;
        private int targetZ;
        private final Inventory inventory;

        private TargetingSession(Player player, Location launchpadLocation) {
            this.player = player;
            this.launchpadLocation = launchpadLocation;
            this.world = launchpadLocation.getWorld();
            setTarget(player.getLocation());
            TargetingHolder holder = new TargetingHolder(player.getUniqueId());
            this.inventory = Bukkit.createInventory(holder, 27, Component.text("ICBM Targeting"));
            holder.setInventory(this.inventory);
            refreshInventory();
        }

        private void setTarget(Location location) {
            targetX = location.getBlockX();
            targetY = clampHeight(location.getBlockY());
            targetZ = location.getBlockZ();
        }

        private void adjustTarget(TargetAxis axis, int delta) {
            switch (axis) {
                case X -> targetX += delta;
                case Y -> targetY = clampHeight(targetY + delta);
                case Z -> targetZ += delta;
                default -> {
                }
            }
        }

        private int clampHeight(int value) {
            int min = world.getMinHeight();
            int max = world.getMaxHeight() - 1;
            return Math.max(min, Math.min(max, value));
        }

        private Location targetLocation() {
            return new Location(world, targetX + 0.5, targetY + 0.5, targetZ + 0.5);
        }

        private void refreshInventory() {
            inventory.setItem(SLOT_TARGET_X, buildAxisItem(Material.COMPASS, "Target X", targetX));
            inventory.setItem(SLOT_TARGET_Y, buildAxisItem(Material.CLOCK, "Target Y", targetY));
            inventory.setItem(SLOT_TARGET_Z, buildAxisItem(Material.ENDER_PEARL, "Target Z", targetZ));
            inventory.setItem(SLOT_USE_CURRENT, buildButton(Material.MAP, "Use Current Location"));
            inventory.setItem(SLOT_LAUNCH, buildButton(Material.FIRE_CHARGE, "Launch ICBM"));
            inventory.setItem(SLOT_CANCEL, buildButton(Material.BARRIER, "Cancel"));
        }

        private ItemStack buildAxisItem(Material material, String label, int value) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(label));
                meta.lore(
                    java.util.List.of(
                        Component.text("Value: " + value),
                        Component.text("Left click: +1"),
                        Component.text("Right click: -1"),
                        Component.text("Shift click: +/-10")
                    )
                );
                item.setItemMeta(meta);
            }
            return item;
        }

        private ItemStack buildButton(Material material, String label) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(label));
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    private static final class TargetingHolder implements InventoryHolder {
        private final UUID playerId;
        private Inventory inventory;

        private TargetingHolder(UUID playerId) {
            this.playerId = playerId;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
