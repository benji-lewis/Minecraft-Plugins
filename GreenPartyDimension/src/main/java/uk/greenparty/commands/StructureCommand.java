package uk.greenparty.commands;

import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.StructureManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /structure info <name>     — Show structure details.
 * /structure list            — List all structures.
 * /structure rebuild <name>  — (admin) Force rebuild a single structure.
 * /structure rebuild-all     — (admin) Mark all structures as unbuilt and regenerate immediately.
 * /structure regenerate      — (admin) Alias for rebuild-all.
 */
public class StructureCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;
    private static final List<String> STRUCTURE_IDS = Arrays.asList(
        "council_chamber", "compost_plant", "tree_farm", "recycling_centre"
    );

    public StructureCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command must be used by a player.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: §a/structure info <name>"); return true; }
                plugin.getStructureManager().sendStructureInfo(player, args[1]);
            }
            case "list" -> {
                player.sendMessage("§2§l===== STRUCTURES =====");
                for (StructureManager.StructureInfo info : plugin.getStructureManager().getAllStructures()) {
                    String status = info.built ? "§a✓" : "§c✗";
                    player.sendMessage("  " + status + " " + info.displayName
                        + " §8— §7" + info.locationString());
                }
                player.sendMessage("§7Use §a/structure info <name>§7 for details.");
                player.sendMessage("§2§l======================");
            }
            case "rebuild" -> {
                if (!player.hasPermission("greenparty.admin")) {
                    player.sendMessage("§cAdmins only.");
                    return true;
                }
                if (args.length < 2) { player.sendMessage("§cUsage: §a/structure rebuild <name>"); return true; }
                StructureManager sm = plugin.getStructureManager();
                StructureManager.StructureInfo info = sm.getStructure(args[1]);
                if (info != null) {
                    info.built = false;
                    sm.saveData();
                    player.sendMessage("§2[Structures] §aRebuild queued for " + info.displayName
                        + "§a. Restart or re-init dimension to rebuild.");
                } else {
                    player.sendMessage("§cUnknown structure: §7" + args[1]);
                }
            }
            case "rebuild-all", "regenerate" -> {
                if (!player.hasPermission("greenparty.admin")) {
                    player.sendMessage("§cAdmins only.");
                    return true;
                }
                rebuildAll(player);
            }
            default -> showHelp(player);
        }

        return true;
    }

    /**
     * Marks all 4 structures as unbuilt, saves, then calls initialiseStructures()
     * on the Verdant Utopia world — equivalent to deleting structures.json and
     * restarting, but live with no restart required.
     */
    private void rebuildAll(Player player) {
        StructureManager sm = plugin.getStructureManager();

        // Mark all 4 structures as unbuilt
        for (String id : STRUCTURE_IDS) {
            StructureManager.StructureInfo info = sm.getStructure(id);
            if (info != null) {
                info.built = false;
            }
        }
        sm.saveData();

        // Resolve the Verdant Utopia world
        String worldName = plugin.getConfig().getString("dimension.name", "the_verdant_utopia");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            // Fall back to the world the player is currently in
            world = player.getWorld();
        }

        player.sendMessage("§2[Structures] §aAll 4 structures marked for rebuild. Regenerating now — stand back!");
        plugin.getLogger().info("[Structures] Admin " + player.getName() + " triggered a full structure rebuild via /structure rebuild-all.");

        sm.initialiseStructures(world);
    }

    private void showHelp(Player player) {
        player.sendMessage("§2§l===== STRUCTURE COMMANDS =====");
        player.sendMessage("§a/structure list §7— list all structures");
        player.sendMessage("§a/structure info <name> §7— structure details");
        if (player.hasPermission("greenparty.admin")) {
            player.sendMessage("§c/structure rebuild <name> §8[admin] §7— force rebuild one");
            player.sendMessage("§c/structure rebuild-all §8[admin] §7— regenerate all structures immediately");
        }
        player.sendMessage("§7Structure names: §a" + String.join("§7, §a", STRUCTURE_IDS));
        player.sendMessage("§2§l==============================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "list"));
            if (sender.hasPermission("greenparty.admin")) {
                subs.add("rebuild");
                subs.add("rebuild-all");
                subs.add("regenerate");
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rebuild")) {
            return STRUCTURE_IDS.stream()
                .filter(id -> id.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
