package uk.greenparty.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.MotionManager;

import java.util.*;

/**
 * /motion — the democratic command of The Verdant Utopia.
 *
 * Subcommands:
 *   /motion start <title>   — start a motion (councillors / greenparty.council permission)
 *   /motion vote <yes|no>   — cast your vote
 *   /motion results         — see current vote status
 *   /motion cancel          — cancel current motion (admin only)
 *
 * The council takes all motions seriously. Some have been in committee since 2019.
 */
public class MotionCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public MotionCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command requires a player. (The council only accepts in-person motions.)");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start", "raise", "propose" -> handleStart(player, args);
            case "vote", "v" -> handleVote(player, args);
            case "results", "status", "r" -> handleResults(player);
            case "cancel", "withdraw" -> handleCancel(player);
            case "help", "?" -> sendHelp(player);
            default -> {
                player.sendMessage("§cUnknown subcommand. Try §a/motion help§c.");
            }
        }

        return true;
    }

    // ─── Subcommand Handlers ──────────────────────────────────────────────────

    private void handleStart(Player player, String[] args) {
        // Must be a councillor or have the permission
        if (!player.hasPermission("greenparty.council") && !player.hasPermission("greenparty.admin")) {
            player.sendMessage("§cOnly Green Councillors may raise motions.");
            player.sendMessage("§7§o(You need §aгreenparty.council §7§opermission to start a vote.)");
            player.sendMessage("§7Ask an admin, or write a strongly-worded letter to the Permissions Committee.");
            return;
        }

        // Must be in the dimension
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            player.sendMessage("§cMotions may only be raised in §2The Verdant Utopia§c.");
            player.sendMessage("§7Travel there with §a/greenparty teleport§7 first.");
            return;
        }

        // Check if already active
        if (plugin.getMotionManager().hasActiveMotion()) {
            MotionManager.ActiveMotion current = plugin.getMotionManager().getCurrentMotion();
            player.sendMessage("§cA motion is already active: §a\"" + current.getTitle() + "\"§c.");
            player.sendMessage("§7Wait for it to conclude, or use §a/motion cancel §7(admin only).");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: §a/motion start <title>");
            player.sendMessage("§7Example: §a/motion start Ban all dark blocks");
            return;
        }

        // Join args 1+ as the title
        String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (title.length() > 80) {
            player.sendMessage("§cMotion title too long (max 80 chars). The council has character limits.");
            return;
        }

        boolean started = plugin.getMotionManager().startMotion(player, title);
        if (started) {
            player.sendMessage("§2[Motion] §7Your motion has been raised! The council awaits democracy.");
        }
    }

    private void handleVote(Player player, String[] args) {
        if (!plugin.getMotionManager().hasActiveMotion()) {
            player.sendMessage("§7There is no active motion to vote on.");
            player.sendMessage("§7§o(The council will raise one eventually. They're in committee.)");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: §a/motion vote <yes|no>");
            return;
        }

        boolean voteYes;
        switch (args[1].toLowerCase()) {
            case "yes", "y", "aye", "1", "true", "for" -> voteYes = true;
            case "no", "n", "nay", "0", "false", "against" -> voteYes = false;
            default -> {
                player.sendMessage("§cVote must be §ayes§c or §cno§c.");
                return;
            }
        }

        MotionManager.VoteResult result = plugin.getMotionManager().castVote(player, voteYes);

        switch (result) {
            case NO_MOTION ->
                player.sendMessage("§7No active motion to vote on.");
            case NOT_IN_DIMENSION ->
                player.sendMessage("§cYou must be in §2The Verdant Utopia §cto vote.");
            case ALREADY_VOTED -> {
                player.sendMessage("§7You have already cast your vote on this motion.");
                player.sendMessage("§7§o(The council only allows one vote per player per motion. This is democracy.)");
            }
            case VOTED_YES -> {
                player.sendMessage("§2§l[Motion] §r§aYou voted §2YES§a on: §7\""
                    + plugin.getMotionManager().getCurrentMotion().getTitle() + "\"");
                player.sendMessage("§7§o(+2 Green Credits for civic participation. The council thanks you.)");
            }
            case VOTED_NO -> {
                player.sendMessage("§2§l[Motion] §r§cYou voted §4NO§c on: §7\""
                    + plugin.getMotionManager().getCurrentMotion().getTitle() + "\"");
                player.sendMessage("§7§o(+2 Green Credits despite your dissent. Democracy includes disagreement.)");
            }
        }
    }

    private void handleResults(Player player) {
        if (!plugin.getMotionManager().hasActiveMotion()) {
            player.sendMessage("§7No motion is currently active.");
            player.sendMessage("§7§o(All quiet on the democratic front. For now.)");
            return;
        }

        MotionManager.ActiveMotion motion = plugin.getMotionManager().getCurrentMotion();

        player.sendMessage("§2§l===== CURRENT MOTION =====");
        player.sendMessage("§a\"" + motion.getTitle() + "\"");
        player.sendMessage("§7Raised by: §a" + motion.getInitiatorName());
        player.sendMessage("§7Votes cast: §a" + motion.getTotalVotes()
            + " §8(§a" + motion.getYesVotes() + " yes §8/ §c" + motion.getNoVotes() + " no§8)");

        int yes = motion.getYesVotes();
        int no = motion.getNoVotes();
        int total = yes + no;

        if (total > 0) {
            int yesPct = (yes * 100) / total;
            int noPct = 100 - yesPct;
            String bar = buildBar(yesPct, 20);
            player.sendMessage("§a[" + bar + "] §a" + yesPct + "% YES / §c" + noPct + "% NO");
        } else {
            player.sendMessage("§7§o(No votes cast yet. The council awaits participation.)");
        }

        player.sendMessage("§7Use §a/motion vote yes§7 or §a/motion vote no§7 to participate.");
        player.sendMessage("§2§l===========================");
    }

    private void handleCancel(Player player) {
        if (!player.hasPermission("greenparty.admin")) {
            player.sendMessage("§cOnly admins can cancel motions.");
            return;
        }

        if (!plugin.getMotionManager().hasActiveMotion()) {
            player.sendMessage("§7No active motion to cancel.");
            return;
        }

        MotionManager.ActiveMotion motion = plugin.getMotionManager().getCurrentMotion();
        plugin.getMotionManager().cancelAll();
        // Force field — set to null via cancelAll
        player.sendMessage("§c[Motion] §7Motion \"§c" + motion.getTitle() + "§7\" cancelled by admin.");

        // Broadcast
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                p.sendMessage("§c§l[MOTION CANCELLED] §r§7The motion \"§a" + motion.getTitle()
                    + "§7\" has been withdrawn by the administration.");
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildBar(int percent, int width) {
        int filled = (percent * width) / 100;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            if (i < filled) sb.append("§a█");
            else sb.append("§c░");
        }
        return sb.toString();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§2§l===== /MOTION HELP =====");
        player.sendMessage("§a/motion start <title> §7— Raise a motion for a vote §8(councillors only)");
        player.sendMessage("§a/motion vote <yes|no> §7— Cast your vote on the active motion");
        player.sendMessage("§a/motion results §7— Check current vote status");
        if (player.hasPermission("greenparty.admin")) {
            player.sendMessage("§c/motion cancel §7— Cancel the active motion §8(admin)");
        }
        player.sendMessage("");
        player.sendMessage("§7Voting earns §a+2 Green Credits§7. The council thanks all participants.");
        player.sendMessage("§7Passed motions have real effects on The Verdant Utopia!");
        player.sendMessage("§2§l========================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "vote", "results", "cancel", "help");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vote")) {
                return Arrays.asList("yes", "no");
            }
        }
        return Collections.emptyList();
    }
}
