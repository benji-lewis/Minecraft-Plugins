package uk.greenparty.routines;

import org.bukkit.entity.Entity;

/**
 * RoutineStep — A single discrete action in a scripted routine.
 *
 * Each step is executed by the RoutineManager and returns a result indicating
 * whether execution should continue immediately or pause for a duration.
 *
 * Implementing a new step type:
 *   1. Add a new StepType enum entry
 *   2. Create a class implementing RoutineStep
 *   3. Register it in RoutineLoader.buildStep()
 *
 * Design principle: steps are immutable data objects. All state lives in NpcRoutineState.
 */
public interface RoutineStep {

    /**
     * The type of step — used for logging, serialisation, and routing in RoutineLoader.
     */
    StepType getType();

    /**
     * Execute this step for the given NPC.
     *
     * @param npc     The villager entity executing the step
     * @param state   The NPC's current routine state (for reading/writing transient data)
     * @param manager The RoutineManager (for spawning effects, sending messages, etc.)
     * @return Ticks to wait before advancing to the next step.
     *         Return 0 to advance immediately (synchronous steps like TurnFace).
     *         Return a positive number to pause (e.g., Wait(100) returns 100).
     */
    int execute(Entity npc, NpcRoutineState state, RoutineManager manager);

    /**
     * Human-readable description for logging and /routine info output.
     */
    String describe();

    // ─── Step Type Enum ───────────────────────────────────────────────────────

    enum StepType {
        MOVE_TO,       // Walk or teleport an NPC to a named location or coordinate
        SIT,           // Put the NPC in a crouching/sitting pose for a duration
        STAND,         // Cancel sitting / restore upright pose
        SAY,           // The NPC says something (chat message with name prefix)
        GROUP_SAY,     // Turn-based multi-NPC conversation
        WAIT,          // Pause N ticks before proceeding
        ANIMATE,       // Trigger an arm-swing or gesture animation
        TURN_FACE,     // Rotate NPC to face a named location or coordinate
        EMOTE,         // Particle effect + sound (celebration, distress, thinking, etc.)
        BROADCAST,     // Server-wide or world chat message
        CONDITIONAL,   // Skip next N steps if a named condition is false
        CHAIN_ROUTINE, // Queue another named routine after this one (for chaining/nesting)
    }
}
