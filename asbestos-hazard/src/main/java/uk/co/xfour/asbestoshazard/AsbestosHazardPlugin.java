package uk.co.xfour.asbestoshazard;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AsbestosHazardPlugin extends JavaPlugin implements Listener {
    private static final double DROP_CHANCE = 0.12;
    private static final int ASBESTOS_ZONE_MAX_Y = 32;
    private static final long EFFECT_INTERVAL_TICKS = 40L;
    private static final int EFFECT_DURATION_TICKS = 120;
    private static final int MAX_EFFECT_AMPLIFIER = 3;
    private static final int ASBESTOS_PER_SEVERITY = 4;
    private static final int EXPOSURE_TICKS_PER_SEVERITY = 20 * 30;

    private final Random random = new Random();
    private final Map<UUID, Integer> exposureTicks = new HashMap<>();
    private NamespacedKey asbestosKey;

    @Override
    public void onEnable() {
        asbestosKey = new NamespacedKey(this, "asbestos_item");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::applyAsbestosEffects, EFFECT_INTERVAL_TICKS, EFFECT_INTERVAL_TICKS);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("asbestoschunk")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }

        boolean isAsbestosChunk = isAsbestosZone(player.getLocation());
        if (isAsbestosChunk) {
            player.sendMessage(ChatColor.GRAY + "This chunk is within the asbestos zone.");
        } else {
            player.sendMessage(ChatColor.GREEN + "This chunk is not within the asbestos zone.");
        }
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (!isRockBlock(event.getBlock().getType())) {
            return;
        }

        if (!isAsbestosZone(event.getBlock().getLocation())) {
            return;
        }

        if (random.nextDouble() > DROP_CHANCE) {
            return;
        }

        Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        event.getBlock().getWorld().dropItemNaturally(dropLocation, createAsbestosItem());
    }

    private void applyAsbestosEffects() {
        for (Player player : getServer().getOnlinePlayers()) {
            int asbestosCount = getAsbestosCount(player.getInventory());
            if (asbestosCount <= 0) {
                exposureTicks.remove(player.getUniqueId());
                continue;
            }

            UUID playerId = player.getUniqueId();
            int updatedExposure = exposureTicks.getOrDefault(playerId, 0) + (int) EFFECT_INTERVAL_TICKS;
            exposureTicks.put(playerId, updatedExposure);

            int amplifier = Math.min(MAX_EFFECT_AMPLIFIER,
                    (asbestosCount - 1) / ASBESTOS_PER_SEVERITY + updatedExposure / EXPOSURE_TICKS_PER_SEVERITY);

            applyEffect(player, PotionEffectType.SLOW, amplifier);
            applyEffect(player, PotionEffectType.POISON, amplifier);
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect effect = new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, true, true);
        player.addPotionEffect(effect);
    }

    private boolean isAsbestosZone(Location location) {
        return location.getBlockY() <= ASBESTOS_ZONE_MAX_Y && location.getWorld() != null
                && location.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL;
    }

    private boolean isRockBlock(Material material) {
        return material == Material.COBBLESTONE
                || material == Material.COBBLED_DEEPSLATE
                || material == Material.DEEPSLATE
                || material == Material.STONE
                || material == Material.GRANITE
                || material == Material.DIORITE
                || material == Material.ANDESITE
                || material == Material.TUFF
                || material == Material.CALCITE
                || material == Material.BLACKSTONE
                || material == Material.BASALT
                || material == Material.SMOOTH_BASALT;
    }

    private boolean hasAsbestos(PlayerInventory inventory) {
        return getAsbestosCount(inventory) > 0;
    }

    private int getAsbestosCount(PlayerInventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (isAsbestos(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean isAsbestos(ItemStack item) {
        if (item == null || item.getType() != Material.GRAY_DYE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(asbestosKey, PersistentDataType.BYTE);
    }

    private ItemStack createAsbestosItem() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Asbestos");
            meta.setLore(List.of(
                    ChatColor.DARK_GRAY + "Handle with care.",
                    ChatColor.DARK_RED + "Prolonged exposure is hazardous."
            ));
            meta.getPersistentDataContainer().set(asbestosKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
