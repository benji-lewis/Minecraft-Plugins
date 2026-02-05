package uk.co.xfour.protestors.modules;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;

/**
 * Asbestos hazard module logic.
 */
public class AsbestosHazardModule implements Module, Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private NamespacedKey asbestosKey;

    public AsbestosHazardModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        asbestosKey = new NamespacedKey(plugin, "asbestos_item");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::applyEffects, 40L, 40L);
    }

    @Override
    public void stop() {
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;
        if (!event.getBlock().getType().name().contains("STONE")) return;
        if (event.getBlock().getY() > plugin.getConfig().getInt("modules.asbestos-hazard.max-y", 32)) return;
        if (random.nextDouble() > plugin.getConfig().getDouble("modules.asbestos-hazard.drop-chance", 0.12)) return;
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), createAsbestos());
    }

    private ItemStack createAsbestos() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Asbestos");
            meta.setLore(List.of(ChatColor.DARK_RED + "Hazardous material"));
            meta.getPersistentDataContainer().set(asbestosKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyEffects() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() != Material.GRAY_DYE || !item.hasItemMeta()) continue;
                Byte value = item.getItemMeta().getPersistentDataContainer().get(asbestosKey, PersistentDataType.BYTE);
                if (value != null && value == 1) count += item.getAmount();
            }
            if (count > 0) {
                int amp = Math.min(3, count / 4);
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, amp, true, true));
            }
        }
    }
}
