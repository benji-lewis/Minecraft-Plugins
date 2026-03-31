package uk.greenparty.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.QuestChainManager;

import java.util.ArrayList;
import java.util.List;

/**
 * QuestCommand — /quest <list|start|progress|abandon>
 *
 * Handles the Phase 3 quest chain system.
 * The Quest Committee approved this command after a 3-hour meeting.
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public QuestCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            plugin.getQuestChainManager().handleCommand(player, new String[]{"quest", "list"});
            return true;
        }

        // Pass full args to chain manager
        String[] chainArgs = new String[args.length + 1];
        chainArgs[0] = "quest";
        System.arraycopy(args, 0, chainArgs, 1, args.length);

        plugin.getQuestChainManager().handleCommand(player, chainArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = List.of("list", "start", "progress", "abandon");
            String prefix = args[0].toLowerCase();
            subs.stream().filter(s -> s.startsWith(prefix)).forEach(completions::add);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("start")) {
            String prefix = args[1].toLowerCase();
            plugin.getQuestChainManager().getAllChains().stream()
                .map(c -> c.id)
                .filter(id -> id.startsWith(prefix))
                .forEach(completions::add);
        }

        return completions;
    }
}
