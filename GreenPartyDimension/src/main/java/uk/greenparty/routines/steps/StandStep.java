package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * StandStep — Cancel sitting/crouching and restore the NPC to upright posture.
 *
 * Paired with SitStep. Use after dialogue completes in meeting sequences so
 * councillors stand back up before moving to the next location.
 *
 * YAML:
 *   - type: STAND
 *     npc: "Councillor Wheatgrass"   # optional, defaults to self
 */
public class StandStep implements RoutineStep {

    private final String targetNpcName; // null = self

    public StandStep(String targetNpcName) {
        this.targetNpcName = targetNpcName;
    }

    @Override
    public StepType getType() { return StepType.STAND; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity target = (targetNpcName != null) ? manager.findNpc(targetNpcName) : npc;
        if (target instanceof LivingEntity le) {
            le.setSneaking(false);
        }
        return 0; // Instant
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return "Stand[" + who + "]";
    }
}
