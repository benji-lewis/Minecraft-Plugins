package uk.greenparty.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * DebateCommand — /debate vote <npc_name>
 *
 * Players vote on the current councillor debate.
 * Winner gets +20 GC, loser gets -5 GC.
 * The council records all votes. Probably.
 */
public class DebateCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public DebateCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            showDebateStatus(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("vote")) {
            String[] debateArgs = new String[args.length + 1];
            debateArgs[0] = "debate";
            System.arraycopy(args, 0, debateArgs, 1, args.length);
            plugin.getDebateManager().handleVoteCommand(player, debateArgs);
        } else if (args[0].equalsIgnoreCase("status")) {
            showDebateStatus(player);
        } else if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("trigger")) {
            handleTriggerDebate(player);
        } else {
            player.sendMessage("§cUsage: /debate vote <councillor_name>");
            player.sendMessage("§7       /debate status");
            if (player.hasPermission("greenparty.admin")) {
                player.sendMessage("§7       /debate start §8(admin: force a debate now)");
            }
        }

        return true;
    }

    private void handleTriggerDebate(Player player) {
        if (!player.hasPermission("greenparty.admin")) {
            player.sendMessage("§cYou don't have permission to trigger debates.");
            return;
        }
        var dm = plugin.getDebateManager();
        if (dm.isDebateActive()) {
            player.sendMessage("§cA debate is already in progress! §7Let the councillors finish.");
            return;
        }
        if (dm.isPreDebateCountdown()) {
            player.sendMessage("§cA debate countdown is already running! §7Patience — it will begin shortly.");
            return;
        }
        boolean started = dm.triggerNow();
        if (started) {
            int minutes = plugin.getConfig().getInt("debates.announcement-offset-ticks", 2400) / (20 * 60);
            player.sendMessage("§2§lDEBATE ANNOUNCED! §7The council will convene in " + minutes + " minute(s). Voting opens when it begins.");
        } else {
            player.sendMessage("§cCould not start debate — one may already be active or pending.");
        }
    }

    private void showDebateStatus(Player player) {
        var dm = plugin.getDebateManager();
        if (dm.isPreDebateCountdown() && !dm.isDebateActive()) {
            int minutes = plugin.getConfig().getInt("debates.announcement-offset-ticks", 2400) / (20 * 60);
            player.sendMessage("§2§l===== Upcoming Debate =====");
            player.sendMessage("§eStarting in approximately " + minutes + " minute(s)!");
            player.sendMessage("§e" + dm.getDebateTitle());
            player.sendMessage("§a" + dm.getDebatantA() + " §7vs §c" + dm.getDebatantB());
            player.sendMessage("§7Voting opens when the debate begins.");
            player.sendMessage("§2§l===========================");
        } else if (!dm.isDebateActive()) {
            player.sendMessage("§7No debate is currently active.");
            player.sendMessage("§8Debates occur every 30 minutes. The council runs a very tight schedule.");
        } else {
            player.sendMessage("§2§l===== Current Debate =====");
            player.sendMessage("§e" + dm.getDebateTitle());
            player.sendMessage("§aSide A: §7" + dm.getDebatantA());
            player.sendMessage("§cSide B: §7" + dm.getDebatantB());
            player.sendMessage("§7Vote with: §a/debate vote <councillor_name>");
            player.sendMessage("§2§l==========================");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("vote", "status"));
            if (sender.hasPermission("greenparty.admin")) {
                subs.add("start");
                subs.add("trigger");
            }
            String prefix = args[0].toLowerCase();
            subs.stream().filter(s -> s.startsWith(prefix)).forEach(completions::add);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("vote")) {
            var dm = plugin.getDebateManager();
            if (dm.isDebateActive()) {
                String a = dm.getDebatantA();
                String b = dm.getDebatantB();
                if (a != null) completions.add(a.split(" ")[0].toLowerCase());
                if (b != null) completions.add(b.split(" ")[0].toLowerCase());
            }
        }
        return completions;
    }
}
