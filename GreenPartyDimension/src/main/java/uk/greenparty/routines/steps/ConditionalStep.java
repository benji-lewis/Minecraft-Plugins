package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * ConditionalStep — Skip the next N steps if a named condition is false.
 *
 * Conditions are string identifiers evaluated by RoutineManager#checkCondition().
 * The manager checks the game state and returns true/false.
 *
 * Built-in conditions:
 *   "players_online"       — at least 1 player is in the world
 *   "players_nearby"       — at least 1 player within 30 blocks of any councillor
 *   "eco_score_high"       — world average eco-score > 50
 *   "eco_score_low"        — world average eco-score < 20
 *   "time_day"             — in-game time is daytime (0-12000)
 *   "time_night"           — in-game time is night (12000-24000)
 *   "debate_not_active"    — no debate is currently running
 *   Any other string falls back to false (logs a warning).
 *
 * If the condition is FALSE, the next skipCount steps are skipped.
 * If the condition is TRUE, execution continues normally.
 *
 * YAML:
 *   - type: CONDITIONAL
 *     condition: "players_online"
 *     skip_steps: 3           # skip next 3 steps if no players online
 */
public class ConditionalStep implements RoutineStep {

    private final String condition;
    private final int skipSteps; // How many following steps to skip if condition is false

    public ConditionalStep(String condition, int skipSteps) {
        this.condition = condition;
        this.skipSteps = skipSteps;
    }

    @Override
    public StepType getType() { return StepType.CONDITIONAL; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        boolean result = manager.checkCondition(condition, npc);
        if (!result) {
            // Signal the RoutineManager to skip the next N steps
            // We store the skip count in scratch; the manager reads it after execute()
            state.setScratch("conditional_skip", skipSteps);
        }
        return 0; // Conditional check is instant
    }

    public String getCondition() { return condition; }
    public int getSkipSteps() { return skipSteps; }

    @Override
    public String describe() {
        return String.format("Conditional[if !%s → skip %d steps]", condition, skipSteps);
    }
}
