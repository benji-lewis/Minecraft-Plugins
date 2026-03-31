package uk.greenparty.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * Handles Green Portals — the environmentally responsible way to travel to the Verdant Utopia.
 *
 * Portal Design (approved by the Dimensional Travel Committee after 8 meetings):
 * - Frame made of LEAVES (oak leaves, because oak is the most democratic tree)
 * - Minimum size: 2x3 (width x height)
 * - Interior: Air (the purity of air symbolises the Green Party's ideals)
 * - Activation: Right-click the frame with a COMPOSTER
 *   (because you can't enter utopia without demonstrating your commitment to composting)
 *
 * Alternative activation: water bucket (hydration is also important)
 *
 * The portal emits a gentle green glow and plays a nature sound.
 * The Nether portal was considered but rejected on environmental grounds. Too hot.
 */
public class PortalListener implements Listener {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Track portal activation cooldowns
    private final Map<UUID, Long> portalCooldowns = new HashMap<>();
    private static final long PORTAL_COOLDOWN_MS = 3000L; // 3 seconds

    // Materials that can activate the portal
    private static final Set<Material> PORTAL_ACTIVATORS = new HashSet<>(Arrays.asList(
        Material.COMPOSTER,
        Material.WATER_BUCKET,
        Material.BONE_MEAL // For the farmers among us
    ));

    // Portal frame materials (any leaf block counts — we're inclusive)
    private static final Set<Material> PORTAL_FRAME_MATERIALS = new HashSet<>();
    static {
        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_LEAVES") || mat.name().endsWith("_LOG")) {
                PORTAL_FRAME_MATERIALS.add(mat);
            }
        }
        // Also accept green blocks
        PORTAL_FRAME_MATERIALS.add(Material.GREEN_WOOL);
        PORTAL_FRAME_MATERIALS.add(Material.GREEN_CONCRETE);
        PORTAL_FRAME_MATERIALS.add(Material.MOSS_BLOCK);
        PORTAL_FRAME_MATERIALS.add(Material.MOSSY_COBBLESTONE);
        PORTAL_FRAME_MATERIALS.add(Material.MOSSY_STONE_BRICKS);
    }

    public PortalListener(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Check if they're clicking a portal frame with an activator
        if (!PORTAL_FRAME_MATERIALS.contains(clickedBlock.getType())) return;

        Material inHand = player.getInventory().getItemInMainHand().getType();
        if (!PORTAL_ACTIVATORS.contains(inHand)) return;

        // Check cooldown
        Long lastUse = portalCooldowns.get(player.getUniqueId());
        if (lastUse != null && System.currentTimeMillis() - lastUse < PORTAL_COOLDOWN_MS) return;
        portalCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Check if they're already in the Verdant Utopia
        if (plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            // Send them back
            player.sendMessage("§2[Green Portal] §7Returning to the overworld... §8(Don't forget your values)");
            plugin.getDimensionManager().teleportToOverworld(player);
            spawnPortalEffects(clickedBlock.getLocation());
        } else {
            // Send them to the Verdant Utopia
            player.sendMessage("§2[Green Portal] §aPortal activated! §7Travelling to §2§lThe Verdant Utopia§r§7...");
            player.sendMessage("§7§o(Please ensure you have filled in the inter-dimensional travel form. It's on the website.)");
            spawnPortalEffects(clickedBlock.getLocation());
            plugin.getDimensionManager().teleportToVerdantUtopia(player);
        }

        event.setCancelled(true);
    }

    private void spawnPortalEffects(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Particle effects — green, obviously
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.COMPOSTER, loc, 20, 0.5, 0.5, 0.5, 0.1);

        // Sound effects
        world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
        world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);
    }

    // Command-triggered teleport (doesn't require portal structure)
    public void triggerPortalTeleport(Player player) {
        if (plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            player.sendMessage("§7You're already in §2The Verdant Utopia§7. You can't get more green than this.");
            return;
        }

        player.sendMessage("§2§lActivating Green Portal!§r");
        player.sendMessage("§7§oThe council has approved your inter-dimensional travel request.");
        player.sendMessage("§7§o(This took 3 meetings but they fast-tracked it. You're welcome.)");

        // Effects at player location
        spawnPortalEffects(player.getLocation());

        // Slight delay for drama
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getDimensionManager().teleportToVerdantUtopia(player);
        }, 20L);
    }
}
