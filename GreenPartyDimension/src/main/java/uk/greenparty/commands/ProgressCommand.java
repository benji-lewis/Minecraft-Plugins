package uk.greenparty.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * /progress monthly — Show monthly standings and seasonal bonuses.
 */
public class ProgressCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public ProgressCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command must be used by a player.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("monthly")) {
            plugin.getProgressionManager().showMonthlyProgress(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "seasonal" -> {
                if (plugin.getProgressionManager().isSeasonalBonus()) {
                    player.sendMessage("§6§l🎉 Seasonal Bonus Active!");
                    player.sendMessage("§a" + plugin.getProgressionManager().getSeasonalBonusDescription());
                } else {
                    player.sendMessage("§7No seasonal bonus is currently active.");
                    player.sendMessage("§7§oComing up: Earth Day (April 22), World Environment Day (June 5)...");
                }
            }
            case "help" -> {
                player.sendMessage("§2§l===== PROGRESS =====");
                player.sendMessage("§a/progress monthly §7— monthly standings");
                player.sendMessage("§a/progress seasonal §7— seasonal bonus info");
                player.sendMessage("§2§l====================");
            }
            default -> plugin.getProgressionManager().showMonthlyProgress(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("monthly", "seasonal", "help");
        }
        return Collections.emptyList();
    }
}
