package uk.greenparty.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.LeaderboardManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /leaderboard <category> — View top 10 players in a category.
 */
public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public LeaderboardCommand(GreenPartyPlugin plugin) {
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

        String catId = args[0].toLowerCase();
        LeaderboardManager.Category cat = LeaderboardManager.Category.fromId(catId);

        if (cat == null) {
            // Try partial match
            for (LeaderboardManager.Category c : LeaderboardManager.Category.values()) {
                if (c.id.contains(catId) || c.displayName.toLowerCase().contains(catId)) {
                    cat = c;
                    break;
                }
            }
        }

        if (cat == null) {
            player.sendMessage("§cUnknown category: §7" + args[0]);
            showHelp(player);
            return true;
        }

        plugin.getLeaderboardManager().displayLeaderboard(player, cat);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§2§l===== LEADERBOARDS =====");
        player.sendMessage("§7Usage: §a/leaderboard <category>");
        player.sendMessage("");
        for (LeaderboardManager.Category cat : LeaderboardManager.Category.values()) {
            player.sendMessage("  §a" + cat.id + " §8— " + cat.displayName + " " + cat.description);
        }
        player.sendMessage("§2§l========================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Arrays.stream(LeaderboardManager.Category.values())
                .map(c -> c.id)
                .filter(id -> id.startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
