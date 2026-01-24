package com.example.asbestoshazard;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.List;
import java.util.Random;

public class AsbestosHazardPlugin extends JavaPlugin implements Listener {
    private static final double DROP_CHANCE = 0.12;
    private static final int ASBESTOS_ZONE_MAX_Y = 32;
    private static final long EFFECT_INTERVAL_TICKS = 40L;
    private static final int EFFECT_DURATION_TICKS = 120;

    private final Random random = new Random();
    private NamespacedKey asbestosKey;

    @Override
    public void onEnable() {
        asbestosKey = new NamespacedKey(this, "asbestos_item");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::applyAsbestosEffects, EFFECT_INTERVAL_TICKS, EFFECT_INTERVAL_TICKS);
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
            if (hasAsbestos(player.getInventory())) {
                applyEffect(player, PotionEffectType.SLOW, 0);
                applyEffect(player, PotionEffectType.POISON, 0);
            }
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
        for (ItemStack item : inventory.getContents()) {
            if (isAsbestos(item)) {
                return true;
            }
        }
        return false;
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
