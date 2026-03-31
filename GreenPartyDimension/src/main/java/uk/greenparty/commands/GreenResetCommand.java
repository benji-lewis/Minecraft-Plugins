package uk.greenparty.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * /greenreset command — resets a player's Green Party quest progress.
 *
 * This is an admin command because resetting progress is considered
 * "retroactive carbon credit fraud" by the council and should not be
 * undertaken lightly.
 */
public class GreenResetCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public GreenResetCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage("§cYou don't have permission to reset Green Party progress.");
            if (sender instanceof Player) {
                sender.sendMessage("§7The council requires admin authority for this. It's quite serious.");
            }
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: §7" + args[0]);
                sender.sendMessage("§7§o(They may have left. The council does not approve of leaving.)");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("§cPlease specify a player name.");
            return true;
        }

        plugin.getQuestManager().resetPlayer(target);

        if (sender != target) {
            sender.sendMessage("§aReset Green Party progress for §2" + target.getName() + "§a.");
            sender.sendMessage("§7§o(The council has been notified. They are very unhappy about this.)");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
