package uk.greenparty.routines;

import java.util.HashMap;
import java.util.Map;

/**
 * NpcRoutineState — Per-NPC runtime state while executing a routine.
 *
 * Tracks which routine and step an NPC is currently on, whether it's locked
 * (mid-execution), and any transient data steps need to pass between themselves
 * (e.g., which GroupSay line we're on).
 *
 * One instance lives in RoutineManager#npcStates, keyed by stripped NPC name.
 */
public class NpcRoutineState {

    /** The routine currently executing. Null if idle. */
    private Routine currentRoutine;

    /** Index into currentRoutine.getSteps() of the next step to run. */
    private int stepIndex;

    /** Ticks remaining before the next step fires. Decremented each tick. */
    private int waitTicks;

    /**
     * Whether this NPC is "locked" into its routine and should not be moved
     * by NPCScheduleManager. Set true on routine start, false on completion.
     */
    private boolean locked;

    /** Priority of the current routine. Higher priority routines can preempt lower ones. */
    private int priority;

    /**
     * Step-local scratch data. Steps can read/write arbitrary String→Object values here.
     * Examples: GroupSay uses "group_say_index" to track which line is next.
     */
    private final Map<String, Object> scratch = new HashMap<>();

    // ─── Constructor ──────────────────────────────────────────────────────────

    public NpcRoutineState() {
        this.currentRoutine = null;
        this.stepIndex = 0;
        this.waitTicks = 0;
        this.locked = false;
        this.priority = 0;
    }

    // ─── State Access ─────────────────────────────────────────────────────────

    public boolean isIdle() {
        return currentRoutine == null;
    }

    public boolean isLocked() {
        return locked;
    }

    public Routine getCurrentRoutine() {
        return currentRoutine;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public int getWaitTicks() {
        return waitTicks;
    }

    public int getPriority() {
        return priority;
    }

    // ─── State Mutation ───────────────────────────────────────────────────────

    public void startRoutine(Routine routine) {
        this.currentRoutine = routine;
        this.stepIndex = 0;
        this.waitTicks = 0;
        this.locked = true;
        this.priority = routine.getPriority();
        this.scratch.clear();
    }

    public void advanceStep(int pauseTicks) {
        this.stepIndex++;
        this.waitTicks = pauseTicks;
    }

    /**
     * Set wait ticks WITHOUT advancing the step index.
     * Used by re-entrant steps like GroupSayStep that fire multiple times on the same step.
     */
    public void setWaitTicksDirect(int ticks) {
        this.waitTicks = ticks;
    }

    /**
     * Skip the next N steps forward (used by ConditionalStep).
     * Advances the step index by N without executing those steps.
     */
    public void skipSteps(int count) {
        this.stepIndex += count;
        this.waitTicks = 0;
    }

    public void tickWait() {
        if (waitTicks > 0) waitTicks--;
    }

    public boolean isWaiting() {
        return waitTicks > 0;
    }

    public void complete() {
        this.currentRoutine = null;
        this.stepIndex = 0;
        this.waitTicks = 0;
        this.locked = false;
        this.priority = 0;
        this.scratch.clear();
    }

    public void interrupt() {
        complete(); // same effect — clean slate
    }

    // ─── Scratch Data ─────────────────────────────────────────────────────────

    public void setScratch(String key, Object value) {
        scratch.put(key, value);
    }

    public Object getScratch(String key) {
        return scratch.get(key);
    }

    public int getScratchInt(String key, int defaultValue) {
        Object val = scratch.get(key);
        if (val instanceof Integer i) return i;
        return defaultValue;
    }

    public boolean getScratchBool(String key, boolean defaultValue) {
        Object val = scratch.get(key);
        if (val instanceof Boolean b) return b;
        return defaultValue;
    }

    @Override
    public String toString() {
        if (isIdle()) return "Idle";
        return String.format("Routine='%s' step=%d/%d wait=%d prio=%d locked=%b",
            currentRoutine.getName(), stepIndex,
            currentRoutine.getSteps().size(), waitTicks, priority, locked);
    }
}
