package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * ChainRoutineStep — Queue another named routine to run after this one completes.
 *
 * Enables routine composition without duplicating steps. For example:
 *   council_session → chain → council_dismiss (councillors stand and walk to spawn)
 *
 * The chained routine is queued in RoutineManager at the SAME priority as the
 * parent, so it inherits the lock status. It will start after the current
 * routine finishes its last step naturally.
 *
 * delay_ticks: Optional gap between parent end and child start (default 0).
 *
 * YAML:
 *   - type: CHAIN_ROUTINE
 *     routine_name: "council_dismiss"
 *     delay_ticks: 40
 */
public class ChainRoutineStep implements RoutineStep {

    private final String routineName;
    private final int delayTicks;

    public ChainRoutineStep(String routineName, int delayTicks) {
        this.routineName = routineName;
        this.delayTicks = delayTicks;
    }

    @Override
    public StepType getType() { return StepType.CHAIN_ROUTINE; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        // Queue the chained routine. It will start after delayTicks.
        manager.scheduleRoutine(routineName, delayTicks, state.getPriority());
        return 0;
    }

    @Override
    public String describe() {
        return String.format("ChainRoutine[→ %s, delay=%d]", routineName, delayTicks);
    }
}
