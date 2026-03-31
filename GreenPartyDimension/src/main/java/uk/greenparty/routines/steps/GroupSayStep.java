package uk.greenparty.routines.steps;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

import java.util.List;

/**
 * GroupSayStep — A turn-based conversation between multiple NPCs.
 *
 * Stores a list of (speaker, line) pairs. Each execution of this step fires
 * ONE line of the dialogue, then pauses for pausePerLineTicks. The step uses
 * NpcRoutineState scratch data to remember which line is next. When all lines
 * have been spoken, it returns 0 to immediately advance to the next step.
 *
 * This means GroupSayStep is re-entrant: the RoutineManager calls execute()
 * repeatedly, once per dialogue line, until the scratch counter runs out.
 * To support this, RoutineManager must NOT advance stepIndex after execute()
 * returns a pause — it should only advance when execute() returns 0 AND
 * the GroupSayStep signals completion (via state scratch "group_say_done" = true).
 *
 * YAML:
 *   - type: GROUP_SAY
 *     pause_per_line_ticks: 80
 *     lines:
 *       - speaker: "Councillor Wheatgrass"
 *         message: "I now call this session to order."
 *       - speaker: "Recycling Evangelist Bramble"
 *         message: "Point of order — are these chairs recycled?"
 *       - speaker: "Councillor Wheatgrass"
 *         message: "Bramble. Please. Every. Single. Time."
 */
public class GroupSayStep implements RoutineStep {

    /** A single line of the conversation. */
    public record DialogueLine(String speakerName, String message) {}

    private static final String SCRATCH_INDEX = "group_say_index";
    private static final String SCRATCH_DONE  = "group_say_done";

    private final List<DialogueLine> lines;
    private final int pausePerLineTicks;

    public GroupSayStep(List<DialogueLine> lines, int pausePerLineTicks) {
        this.lines = List.copyOf(lines);
        this.pausePerLineTicks = pausePerLineTicks > 0 ? pausePerLineTicks : 80;
    }

    @Override
    public StepType getType() { return StepType.GROUP_SAY; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        int index = state.getScratchInt(SCRATCH_INDEX, 0);

        if (index >= lines.size()) {
            // All lines spoken — signal done and advance
            state.setScratch(SCRATCH_DONE, true);
            return 0;
        }

        DialogueLine line = lines.get(index);
        Entity speaker = manager.findNpc(line.speakerName());
        String displayName = manager.getNpcDisplayName(speaker != null ? speaker : npc);

        String formatted = "§2[" + displayName + "] §7" + line.message();
        World world = npc.getWorld();
        world.getPlayers().forEach(p -> p.sendMessage(formatted));

        // If NPC is found, trigger arm swing for the speaker
        if (speaker != null) {
            manager.swingArm(speaker);
        }

        // Advance counter for next call
        state.setScratch(SCRATCH_INDEX, index + 1);

        if (index + 1 >= lines.size()) {
            // Last line just spoken — mark done, pause once more then move on
            state.setScratch(SCRATCH_DONE, true);
        }

        return pausePerLineTicks;
    }

    /**
     * Whether this step has finished all its lines.
     * RoutineManager should check this after each execution to know whether
     * to advance stepIndex or re-fire this step.
     */
    public boolean isDone(NpcRoutineState state) {
        return state.getScratchBool(SCRATCH_DONE, false);
    }

    public List<DialogueLine> getLines() { return lines; }

    @Override
    public String describe() {
        return String.format("GroupSay[%d lines, %dtick/line]", lines.size(), pausePerLineTicks);
    }
}
