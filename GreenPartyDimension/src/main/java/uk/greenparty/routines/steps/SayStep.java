package uk.greenparty.routines.steps;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * SayStep — The NPC broadcasts a line of dialogue to the world chat.
 *
 * Message is prefixed with "§2[NPC Name] §7" for visibility.
 * Supports placeholders:
 *   {name}     → replaced with the NPC's stripped name
 *   {player}   → replaced with a random nearby player name (or "everyone")
 *
 * After speaking, the step pauses for pauseAfterTicks so players can read the line
 * before the next one fires. Recommended: 60–100 ticks (~3–5 seconds).
 *
 * YAML:
 *   - type: SAY
 *     npc: "Councillor Wheatgrass"    # who speaks; null = self
 *     message: "The session is now in order. Motion 77c is on the table."
 *     pause_ticks: 80                 # ticks to wait after speaking (default 60)
 */
public class SayStep implements RoutineStep {

    private final String speakerNpcName; // null = self
    private final String message;
    private final int pauseTicks;

    public SayStep(String speakerNpcName, String message, int pauseTicks) {
        this.speakerNpcName = speakerNpcName;
        this.message = message;
        this.pauseTicks = pauseTicks > 0 ? pauseTicks : 60;
    }

    @Override
    public StepType getType() { return StepType.SAY; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity speaker = (speakerNpcName != null) ? manager.findNpc(speakerNpcName) : npc;
        String speakerName = manager.getNpcDisplayName(speaker != null ? speaker : npc);

        String resolved = message
            .replace("{name}", speakerName)
            .replace("{player}", manager.getNearbyPlayerName(npc));

        World world = npc.getWorld();
        String formatted = "§2[" + speakerName + "] §7" + resolved;
        world.getPlayers().forEach(p -> p.sendMessage(formatted));

        return pauseTicks;
    }

    @Override
    public String describe() {
        String who = (speakerNpcName != null) ? speakerNpcName : "self";
        return String.format("Say[%s: \"%s\"]", who, message.length() > 40
            ? message.substring(0, 40) + "..." : message);
    }
}
