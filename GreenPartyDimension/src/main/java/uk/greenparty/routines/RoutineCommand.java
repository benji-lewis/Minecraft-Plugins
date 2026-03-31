package uk.greenparty.routines;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import uk.greenparty.GreenPartyPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RoutineCommand — /routine and /npc routine info commands.
 *
 * Commands:
 *   /routine list                    — List all registered routines with status
 *   /routine trigger <name>          — Start a routine immediately
 *   /routine stop <npc_name>         — Interrupt an NPC's current routine
 *   /routine info <name>             — Show routine details (steps, NPCs, cooldown)
 *   /npc routine info <npc_name>     — Show what routine an NPC is currently in
 *
 * Permission: greenparty.admin (for trigger/stop), none for list/info.
 */
public class RoutineCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public RoutineCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        RoutineManager rm = plugin.getRoutineManager();
        String cmd = command.getName().toLowerCase();

        // /npc routine info <npc_name>
        if (cmd.equals("npc") && args.length >= 1 && args[0].equalsIgnoreCase("routine")) {
            return handleNpcRoutineInfo(sender, rm, args);
        }

        // /routine <subcommand>
        if (cmd.equals("routine")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            return switch (args[0].toLowerCase()) {
                case "list"    -> handleList(sender, rm);
                case "trigger" -> handleTrigger(sender, rm, args);
                case "stop"    -> handleStop(sender, rm, args);
                case "info"    -> handleInfo(sender, rm, args);
                default        -> { sendHelp(sender); yield true; }
            };
        }

        return false;
    }

    // ─── /routine list ────────────────────────────────────────────────────────

    private boolean handleList(CommandSender sender, RoutineManager rm) {
        Map<String, Routine> registry = rm.getRoutineRegistry();
        if (registry.isEmpty()) {
            sender.sendMessage("§7No routines registered. Check routines.yml.");
            return true;
        }

        sender.sendMessage("§2§l===== Council Routines =====");
        sender.sendMessage("§8(" + registry.size() + " routines loaded from routines.yml)");
        sender.sendMessage("");

        for (Routine r : registry.values()) {
            String prio = switch (r.getPriority()) {
                case 0  -> "§7Normal";
                case 5  -> "§aCouncil";
                case 10 -> "§eDebate";
                case 20 -> "§cEmergency";
                default -> "§7Prio " + r.getPriority();
            };
            sender.sendMessage("§2" + r.getName() + " §8[" + prio + "§8]");
            sender.sendMessage("  §7" + r.getDescription());
            sender.sendMessage("  §8NPCs: §7" + String.join(", ", r.getRequiredNpcs()));
            sender.sendMessage("  §8Steps: §7" + r.getSteps().size()
                + "  §8Duration: §7~" + (r.getDurationTicks() / 20) + "s"
                + "  §8Weight: §7" + r.getWeight());
        }
        return true;
    }

    // ─── /routine trigger <name> ──────────────────────────────────────────────

    private boolean handleTrigger(CommandSender sender, RoutineManager rm, String[] args) {
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage("§cYou don't have permission to trigger routines.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /routine trigger <routine_name>");
            return true;
        }

        String name = args[1].toLowerCase();
        Routine routine = rm.getRoutine(name);
        if (routine == null) {
            sender.sendMessage("§cUnknown routine: §7" + name);
            sender.sendMessage("§8Use /routine list to see available routines.");
            return true;
        }

        boolean started = rm.startRoutine(name, -1);
        if (started) {
            sender.sendMessage("§2Routine '§a" + name + "§2' started.");
            sender.sendMessage("§8NPCs: " + String.join(", ", routine.getRequiredNpcs()));
        } else {
            sender.sendMessage("§eRoutine '§a" + name + "§e' could not start right now.");
            sender.sendMessage("§8Possible reasons: NPCs busy with higher-priority routine, on cooldown, or NPCs not found.");
        }
        return true;
    }

    // ─── /routine stop <npc_name> ─────────────────────────────────────────────

    private boolean handleStop(CommandSender sender, RoutineManager rm, String[] args) {
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage("§cYou don't have permission to stop routines.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /routine stop <npc_name>");
            return true;
        }

        String npcName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        NpcRoutineState state = rm.getNpcState(npcName);

        if (state.isIdle()) {
            sender.sendMessage("§7" + npcName + " is not in a routine.");
            return true;
        }

        rm.stopNpcRoutine(npcName);
        sender.sendMessage("§2Routine interrupted for §a" + npcName + "§2.");
        sender.sendMessage("§8They will return to their normal schedule.");
        return true;
    }

    // ─── /routine info <name> ─────────────────────────────────────────────────

    private boolean handleInfo(CommandSender sender, RoutineManager rm, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /routine info <routine_name>");
            return true;
        }

        String name = args[1].toLowerCase();
        Routine routine = rm.getRoutine(name);
        if (routine == null) {
            sender.sendMessage("§cUnknown routine: §7" + name);
            return true;
        }

        sender.sendMessage("§2§l===== Routine: " + routine.getName() + " =====");
        sender.sendMessage("§aDescription: §7" + routine.getDescription());
        sender.sendMessage("§aRequired NPCs: §7" + String.join(", ", routine.getRequiredNpcs()));
        sender.sendMessage("§aPriority: §7" + routine.getPriority()
            + " §8| §aCooldown: §7" + (routine.getCooldownTicks() / 20) + "s"
            + " §8| §aWeight: §7" + routine.getWeight());
        sender.sendMessage("§aSteps (" + routine.getSteps().size() + "):");
        int i = 1;
        for (RoutineStep step : routine.getSteps()) {
            sender.sendMessage("  §8" + i + ". §7" + step.describe());
            i++;
        }
        return true;
    }

    // ─── /npc routine info <npc_name> ─────────────────────────────────────────

    private boolean handleNpcRoutineInfo(CommandSender sender, RoutineManager rm, String[] args) {
        // args[0] = "routine", args[1] = "info", args[2..] = npc name
        if (args.length < 3 || !args[1].equalsIgnoreCase("info")) {
            sender.sendMessage("§cUsage: /npc routine info <npc_name>");
            return true;
        }

        String npcName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        NpcRoutineState state = rm.getNpcState(npcName);

        sender.sendMessage("§2§l===== NPC Routine: " + npcName + " =====");
        if (state.isIdle()) {
            sender.sendMessage("§7Status: §aIdle §8— following normal schedule");
        } else {
            Routine r = state.getCurrentRoutine();
            sender.sendMessage("§7Status: §eIn routine");
            sender.sendMessage("§aRoutine: §7" + r.getName());
            sender.sendMessage("§aDescription: §7" + r.getDescription());
            sender.sendMessage("§aStep: §7" + (state.getStepIndex() + 1) + " / " + r.getSteps().size());
            if (state.isWaiting()) {
                sender.sendMessage("§aWaiting: §7" + state.getWaitTicks() + " ticks remaining");
            }
            sender.sendMessage("§aPriority: §7" + state.getPriority());
            sender.sendMessage("§aLocked: §7" + state.isLocked());

            // Show current step
            List<RoutineStep> steps = r.getSteps();
            if (state.getStepIndex() < steps.size()) {
                sender.sendMessage("§aCurrent Step: §7" + steps.get(state.getStepIndex()).describe());
            }
        }

        return true;
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        RoutineManager rm = plugin.getRoutineManager();
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("routine")) {
            if (args.length == 1) {
                List.of("list", "trigger", "stop", "info").forEach(sub -> {
                    if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
                });
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("trigger") || args[0].equalsIgnoreCase("info")) {
                    rm.getRoutineRegistry().keySet().forEach(name -> {
                        if (name.startsWith(args[1].toLowerCase())) completions.add(name);
                    });
                }
            }
        }

        if (cmd.equals("npc") && args.length >= 1) {
            if (args.length == 1 && "routine".startsWith(args[0].toLowerCase())) {
                completions.add("routine");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("routine")
                && "info".startsWith(args[1].toLowerCase())) {
                completions.add("info");
            }
        }

        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§2§l===== /routine Commands =====");
        sender.sendMessage("§a/routine list §7— Show all available routines");
        sender.sendMessage("§a/routine trigger <name> §7— Start a routine immediately");
        sender.sendMessage("§a/routine stop <npc_name> §7— Stop an NPC's current routine");
        sender.sendMessage("§a/routine info <name> §7— Show routine steps and details");
        sender.sendMessage("§a/npc routine info <npc_name> §7— Show NPC's current routine state");
    }
}
