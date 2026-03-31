package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * WaitStep — Pause the routine for a fixed number of ticks.
 *
 * Use to add dramatic pauses, let players absorb the scene, or synchronise
 * multiple NPCs that are running parallel routines.
 *
 * 20 ticks = 1 second
 * 200 ticks = 10 seconds
 * 1200 ticks = 1 minute
 *
 * YAML:
 *   - type: WAIT
 *     ticks: 100
 */
public class WaitStep implements RoutineStep {

    private final int ticks;

    public WaitStep(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public StepType getType() { return StepType.WAIT; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        return ticks;
    }

    @Override
    public String describe() {
        return String.format("Wait[%d ticks / %.1fs]", ticks, ticks / 20.0);
    }
}
