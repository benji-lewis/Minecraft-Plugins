package uk.co.xfour.protestors.modules;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lightweight Kim Jong Un inspired module.
 */
public class KimJongUnModule implements Module, Listener {
    private final JavaPlugin plugin;

    public KimJongUnModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.getLogger().info("Kim Jong Un module enabled.");
    }

    @Override
    public void stop() {
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.FIRE_CHARGE) return;
        if (!plugin.getConfig().getBoolean("modules.kim-jong-un.fire-charge-launch-enabled", true)) return;
        if (player.getTargetBlockExact(80) == null) return;
        player.getWorld().createExplosion(player.getTargetBlockExact(80).getLocation(), 3.5f, false, true);
        player.sendMessage(ChatColor.RED + "[KimJongUn] Payload delivered.");
    }
}
