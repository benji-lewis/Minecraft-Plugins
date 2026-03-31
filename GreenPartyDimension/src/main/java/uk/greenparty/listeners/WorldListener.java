package uk.greenparty.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import uk.greenparty.GreenPartyPlugin;

/**
 * Listens for world events in The Verdant Utopia.
 *
 * Ensures the dimension remains:
 * 1. Overwhelmingly green
 * 2. Hostile-mob free (they were asked to leave at the last meeting)
 * 3. Coal-free (banned by council resolution 14b)
 * 4. Generally pleasant and environmental
 *
 * The Green Council has filed a formal complaint with Mojang about the existence
 * of the Nether. This file does not affect the Nether. Yet.
 */
public class WorldListener implements Listener {

    private final GreenPartyPlugin plugin;

    public WorldListener(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        if (!plugin.getDimensionManager().isVerdantWorld(world)) return;

        // Only allow friendly things in the Verdant Utopia
        if (entity instanceof Monster) {
            event.setCancelled(true);
            // The council has spoken: monsters are banned
            // (They tried giving them a warning letter first. It didn't work.)
            return;
        }

        // Phantoms are banned — they represent the spectre of sleeplessness
        // (The council considers sleep essential for environmental awareness)
        if (entity instanceof Phantom) {
            event.setCancelled(true);
            return;
        }

        // Withers are definitely banned
        if (entity instanceof Wither) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        if (!plugin.getDimensionManager().isVerdantWorld(world)) return;

        // Players cannot be damaged in the Verdant Utopia
        // (The health and safety committee spent 3 meetings on this)
        if (entity instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        Entity entity = event.getEntity();
        if (!plugin.getDimensionManager().isVerdantWorld(entity.getWorld())) return;

        // Food never depletes in the Verdant Utopia — abundance for all
        // (This was extremely easy to pass in committee)
        event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!plugin.getDimensionManager().isVerdantWorld(world)) return;

        // Don't process every chunk load — too intensive
        // The council would approve a random quality check
        if (Math.random() < 0.05) { // 5% of chunks get checked
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Nothing to do here currently — chunk generation handles it
                // But the council likes knowing we checked
            }, 5L);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!plugin.getDimensionManager().isVerdantWorld(entity.getWorld())) return;

        // Clear drops for any entity that somehow dies
        // The council finds death "aesthetically unpleasant"
        // event.getDrops().clear();  // Commented out — let drops happen, they're useful for quests

        // But do replace any coal drops with green dye (symbolic)
        event.getDrops().replaceAll(item -> {
            if (item.getType() == Material.COAL || item.getType() == Material.COAL_BLOCK) {
                return new org.bukkit.inventory.ItemStack(Material.GREEN_DYE, item.getAmount());
            }
            return item;
        });
    }

    @EventHandler
    public void onFireSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        if (!plugin.getDimensionManager().isVerdantWorld(event.getBlock().getWorld())) return;

        // No fire spreading — the council has banned it
        // (Motion 22: "Fire is antithetical to the verdant vision")
        if (event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        if (!plugin.getDimensionManager().isVerdantWorld(event.getBlock().getWorld())) return;
        // No burning blocks in the Verdant Utopia
        event.setCancelled(true);
    }
}
