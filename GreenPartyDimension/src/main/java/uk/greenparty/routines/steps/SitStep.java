package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * SitStep — Make an NPC crouch (sneak) for a given duration.
 *
 * Paper 1.21.1 doesn't have a native "sit" animation for villagers, so we
 * simulate sitting by setting the entity to pose SNEAKING. This visually
 * crouches the villager and is the closest approximation without armour stand tricks.
 *
 * For a richer "seated" look, combine with a TurnFaceStep pointing at a table/lectern.
 * A StandStep (or the SitStep completing its duration) restores the upright pose.
 *
 * YAML:
 *   - type: SIT
 *     npc: "Councillor Wheatgrass"
 *     duration_ticks: 200    # How long to stay seated before auto-standing
 *                            # 0 = sit indefinitely until a STAND step fires
 */
public class SitStep implements RoutineStep {

    private final String targetNpcName; // null = self
    private final int durationTicks;    // 0 = hold until STAND

    public SitStep(String targetNpcName, int durationTicks) {
        this.targetNpcName = targetNpcName;
        this.durationTicks = durationTicks;
    }

    @Override
    public StepType getType() { return StepType.SIT; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity target = (targetNpcName != null) ? manager.findNpc(targetNpcName) : npc;
        if (target instanceof LivingEntity le) {
            le.setSneaking(true);
        }
        // If duration == 0, we don't pause — a later StandStep will un-crouch
        return durationTicks;
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return String.format("Sit[%s, %d ticks]", who, durationTicks);
    }
}
