package uk.greenparty.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

/**
 * /greenportal command — for admins who want to create a portal manually.
 *
 * Actually, this just teleports the admin to the Verdant Utopia directly
 * because building portal frame detection is complicated and the council
 * is still debating the exact dimensions of the official portal frame.
 * (Current proposal: 3x4. Counter-proposal: 4x3. The argument has been ongoing for 6 months.)
 */
public class GreenPortalCommand implements CommandExecutor {

    private final GreenPartyPlugin plugin;

    public GreenPortalCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("greenparty.admin")) {
            player.sendMessage("§cYou don't have permission to create portals.");
            player.sendMessage("§7Have you considered writing a formal request to the Dimensional Travel Committee?");
            return true;
        }

        player.sendMessage("§2[Green Portal] §aOpening inter-dimensional portal...");
        player.sendMessage("§7This portal has been approved after §l3 planning committee meetings§r§7.");
        player.sendMessage("§7§o(A 4th meeting was scheduled but someone forgot to book the room)");

        if (plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            player.sendMessage("§7You're already in the Verdant Utopia. Use §a/greenparty leave§7 to exit.");
            return true;
        }

        plugin.getDimensionManager().teleportToVerdantUtopia(player);
        return true;
    }
}
