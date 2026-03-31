package uk.greenparty.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /violations <player> — check a player's environmental violation count.
 *
 * Only usable by ops or players with greenparty.admin.
 * Or any player checking their own record (greenparty.violations.self).
 *
 * The council takes these numbers very seriously.
 * They have a spreadsheet. Actually, three spreadsheets.
 */
public class ViolationsCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public ViolationsCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Self-check (players only)
            if (sender instanceof Player player) {
                showViolations(sender, player);
            } else {
                sender.sendMessage("§cUsage: /violations <player>");
            }
            return true;
        }

        String targetName = args[0];

        // Admin-only for checking other players
        if (sender instanceof Player player) {
            if (!player.getName().equalsIgnoreCase(targetName)) {
                if (!player.hasPermission("greenparty.admin") && !player.hasPermission("greenparty.violations.check")) {
                    player.sendMessage("§cYou don't have permission to check other players' violations.");
                    player.sendMessage("§7§o(You can check your own with /violations)");
                    return true;
                }
            }
        }

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            // Offline check — look up by name (requires UUID lookup)
            sender.sendMessage("§7Player §a" + targetName + " §7is offline.");

            // We can still look up offline data by trying UUID resolution
            // Try to find from violation map by matching name
            UUID targetUUID = null;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(targetName)) {
                    targetUUID = p.getUniqueId();
                    break;
                }
            }

            if (targetUUID == null) {
                // Try offline player
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                if (offline.hasPlayedBefore()) {
                    targetUUID = offline.getUniqueId();
                }
            }

            if (targetUUID != null) {
                int count = plugin.getViolationManager().getViolationsByUUID(targetUUID);
                sender.sendMessage("§7Violations for §a" + targetName + "§7: §c" + count);
            } else {
                sender.sendMessage("§cPlayer not found or has never played on this server.");
            }
            return true;
        }

        showViolations(sender, target);
        return true;
    }

    private void showViolations(CommandSender sender, Player target) {
        int count = plugin.getViolationManager().getViolations(target);
        int fine = plugin.getViolationManager().getFineAmount();
        int totalFined = count * fine;

        String selfOrOther = (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId()))
            ? "Your" : target.getName() + "'s";

        sender.sendMessage("§2§l===== VIOLATION RECORD =====");
        sender.sendMessage("§7" + selfOrOther + " environmental violations: §c" + count);
        sender.sendMessage("§7Total fines issued: §c" + totalFined + " GC");
        sender.sendMessage("§7Current balance: §a" + plugin.getGreenCreditManager().getBalance(target) + " GC");

        if (count == 0) {
            sender.sendMessage("§a§o(A clean record! The council is cautiously pleased.)");
        } else if (count < 3) {
            sender.sendMessage("§7§o(Minor violations. The council notes this on their clipboard.)");
        } else if (count < 10) {
            sender.sendMessage("§7§o(Repeat offender. The council has convened a special subcommittee.)");
        } else {
            sender.sendMessage("§c§o(SERIAL OFFENDER. The council is DEEPLY disappointed. And drafting policy.)");
        }

        sender.sendMessage("§8Violations reset weekly via automated amnesty.");
        sender.sendMessage("§2§l============================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
