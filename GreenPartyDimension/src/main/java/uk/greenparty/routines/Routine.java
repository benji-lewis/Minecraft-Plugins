package uk.greenparty.routines;

import java.util.List;

/**
 * Routine — A named, reusable scripted sequence for one or more NPCs.
 *
 * Routines are loaded from routines.yml at startup and can be triggered
 * by command, schedule, or event. They are immutable once loaded.
 *
 * YAML structure:
 * <pre>
 *   routines:
 *     council_session:
 *       name: "council_session"
 *       description: "Councillors convene in the chamber for a formal session"
 *       required_npcs:
 *         - "Councillor Wheatgrass"
 *         - "Recycling Evangelist Bramble"
 *       duration_ticks: 3600    # ~3 minutes
 *       cooldown_ticks: 12000   # 10 minutes between repeats
 *       priority: 5             # overrides normal schedule (prio 0)
 *       weight: 10              # relative weight for random scheduling
 *       steps:
 *         - type: MOVE_TO
 *           npc: "Councillor Wheatgrass"
 *           location: "council_chamber"
 *           speed: WALK
 *         - ...
 * </pre>
 */
public class Routine {

    /** Internal identifier (snake_case). Used in commands: /routine trigger council_session */
    private final String name;

    /** Human-friendly description shown in /routine list */
    private final String description;

    /**
     * NPC names (stripped of colour) that must be present for this routine to run.
     * RoutineManager checks these are alive in the world before starting.
     */
    private final List<String> requiredNpcs;

    /**
     * Expected total duration in ticks. Used for scheduling and overlap detection.
     * Actual runtime may differ if steps finish early.
     */
    private final int durationTicks;

    /**
     * Minimum ticks between successive runs of this routine.
     * RoutineManager tracks last-run timestamp per routine name.
     */
    private final int cooldownTicks;

    /**
     * Priority level. Higher value = more important.
     *   0  — Normal (background schedule)
     *   5  — Council routine (overrides normal schedule)
     *   10 — Debate / event (overrides council routine)
     *   20 — Emergency (overrides everything)
     */
    private final int priority;

    /**
     * Relative weight for random scheduling. Higher = more likely to be picked
     * when the RoutineManager's idle scheduler chooses a random routine.
     * 0 = never auto-scheduled (manual/event trigger only).
     */
    private final int weight;

    /** The ordered list of steps that make up this routine. */
    private final List<RoutineStep> steps;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public Routine(String name, String description, List<String> requiredNpcs,
                   int durationTicks, int cooldownTicks, int priority, int weight,
                   List<RoutineStep> steps) {
        this.name = name;
        this.description = description;
        this.requiredNpcs = List.copyOf(requiredNpcs);
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
        this.priority = priority;
        this.weight = weight;
        this.steps = List.copyOf(steps);
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public String getName()                { return name; }
    public String getDescription()         { return description; }
    public List<String> getRequiredNpcs()  { return requiredNpcs; }
    public int getDurationTicks()          { return durationTicks; }
    public int getCooldownTicks()          { return cooldownTicks; }
    public int getPriority()               { return priority; }
    public int getWeight()                 { return weight; }
    public List<RoutineStep> getSteps()    { return steps; }

    @Override
    public String toString() {
        return String.format("Routine{name='%s', npcs=%s, steps=%d, prio=%d}",
            name, requiredNpcs, steps.size(), priority);
    }
}
