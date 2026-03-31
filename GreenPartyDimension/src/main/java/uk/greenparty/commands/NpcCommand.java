package uk.greenparty.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.routines.RoutineCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * NpcCommand — /npc info <name> and /npc routine info <name>
 *
 * Shows a councillor's current location, zone, daily schedule,
 * and (via RoutineCommand delegation) their current routine state.
 */
public class NpcCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;
    private RoutineCommand routineCommand; // set after init

    public NpcCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    public void setRoutineCommand(RoutineCommand routineCommand) {
        this.routineCommand = routineCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§2§l===== NPC Commands =====");
            player.sendMessage("§a/npc info <npc_name> §7— Show councillor's location and schedule");
            player.sendMessage("§a/npc routine info <npc_name> §7— Show what routine an NPC is in");
            return true;
        }

        // /npc routine info <npc_name> — delegate to RoutineCommand
        if (args[0].equalsIgnoreCase("routine")) {
            if (routineCommand != null) {
                return routineCommand.onCommand(sender, command, label, args);
            } else {
                player.sendMessage("§cRoutine system not yet initialised.");
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("info")) {
            // Pass as ["npc", "info", ...]
            String[] scheduleArgs = new String[args.length + 1];
            scheduleArgs[0] = "npc";
            System.arraycopy(args, 0, scheduleArgs, 1, args.length);
            plugin.getNpcScheduleManager().handleNpcInfoCommand(player, scheduleArgs);
        } else {
            player.sendMessage("§cUnknown NPC sub-command: §7" + args[0]);
            player.sendMessage("§7Usage: §a/npc info <name> §8or§7 /npc routine info <name>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List.of("info", "routine").forEach(s -> {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            });
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("routine")) {
            if ("info".startsWith(args[1].toLowerCase())) completions.add("info");
        }
        return completions;
    }
}
