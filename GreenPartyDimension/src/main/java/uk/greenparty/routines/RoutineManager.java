package uk.greenparty.routines;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.routines.steps.GroupSayStep;
import uk.greenparty.routines.steps.ConditionalStep;

import java.util.*;
import java.util.logging.Logger;

/**
 * RoutineManager — The engine that runs all scripted NPC routines.
 *
 * Architecture overview:
 * ┌─────────────────────────────────────────────────┐
 * │  RoutineManager                                 │
 * │  ┌─────────────┐  ┌──────────────────────────┐ │
 * │  │ Routine     │  │ NpcRoutineState (per NPC) │ │
 * │  │ [steps...]  │  │  currentRoutine           │ │
 * │  │             │  │  stepIndex                │ │
 * │  └─────────────┘  │  waitTicks                │ │
 * │                   │  locked                   │ │
 * │                   └──────────────────────────-┘ │
 * │                                                 │
 * │  Each tick: decrement waits, fire next step     │
 * │  NPCScheduleManager paused while locked         │
 * └─────────────────────────────────────────────────┘
 *
 * Thread model: all execution happens on the main server thread via BukkitTask.
 *
 * Integration with NPCScheduleManager:
 *   When a routine locks an NPC, RoutineManager calls
 *   plugin.getNpcScheduleManager().pauseNpc(name) to suppress schedule movement.
 *   On routine completion or interrupt, resumeNpc(name) is called.
 *
 * Priority system:
 *   0  — Normal background schedule
 *   5  — Council routine (overrides normal schedule)
 *   10 — Debate / triggered event
 *   20 — Emergency override
 *
 * A higher-priority routine can preempt a lower-priority running routine.
 *
 * Version 1.0.0 — Initial implementation
 */
public class RoutineManager {

    private final GreenPartyPlugin plugin;
    private final Logger log;

    /** Registry for named locations — replaces (and extends) the static NAMED_LOCATIONS map. */
    private LocationRegistry locationRegistry;

    // ─── Registered Routines ──────────────────────────────────────────────────

    /** All routines loaded from routines.yml, keyed by name. */
    private final Map<String, Routine> routineRegistry = new LinkedHashMap<>();

    // ─── Per-NPC State ────────────────────────────────────────────────────────

    /**
     * Per-NPC execution state, keyed by stripped NPC name.
     * Created lazily when a routine first targets an NPC.
     */
    private final Map<String, NpcRoutineState> npcStates = new HashMap<>();

    /**
     * Movement targets for NPCs executing MOVE_TO steps.
     * Keyed by entity UUID. RoutineManager's tick loop handles movement.
     */
    private final Map<UUID, Location> npcMoveTargets = new HashMap<>();

    // ─── Cooldown Tracking ────────────────────────────────────────────────────

    /** Last completion time (server tick) per routine name, for cooldown checks. */
    private final Map<String, Long> lastRunTick = new HashMap<>();

    // ─── Pending Queue ────────────────────────────────────────────────────────

    /**
     * Routines queued to start after a delay.
     * Entries: [routineName, startAtTick, priority]
     */
    private final List<long[]> pendingQueue = new ArrayList<>();
    // Parallel list of names for pending queue (long[] can't hold String)
    private final List<String> pendingNames = new ArrayList<>();

    // ─── Zone Location Map ────────────────────────────────────────────────────

    /**
     * Named locations for routine steps, resolved relative to world spawn.
     * Populated from config + mirrors NPCScheduleManager zone offsets.
     */
    private static final Map<String, int[]> NAMED_LOCATIONS = new LinkedHashMap<>();
    static {
        NAMED_LOCATIONS.put("council_chamber",    new int[]{  5,  3 });
        NAMED_LOCATIONS.put("compost_processing", new int[]{ 30,  8 });
        NAMED_LOCATIONS.put("tree_farm",          new int[]{-20, 25 });
        NAMED_LOCATIONS.put("recycling_centre",   new int[]{ 15,-30 });
        NAMED_LOCATIONS.put("spawn",              new int[]{  0,  0 });
        // Chamber sub-locations (relative to spawn)
        NAMED_LOCATIONS.put("chamber_presider",   new int[]{  3,  3 });
        NAMED_LOCATIONS.put("chamber_seat_1",     new int[]{  5,  5 });
        NAMED_LOCATIONS.put("chamber_seat_2",     new int[]{  7,  5 });
        NAMED_LOCATIONS.put("chamber_seat_3",     new int[]{  5,  7 });
        NAMED_LOCATIONS.put("chamber_seat_4",     new int[]{  7,  7 });
        NAMED_LOCATIONS.put("chamber_seat_5",     new int[]{  9,  5 });
        NAMED_LOCATIONS.put("chamber_seat_6",     new int[]{  9,  7 });
        // Tree Farm sub-locations
        NAMED_LOCATIONS.put("tree_farm_oak",      new int[]{-20, 25 });
        NAMED_LOCATIONS.put("tree_farm_birch",    new int[]{-22, 28 });
        NAMED_LOCATIONS.put("tree_farm_centre",   new int[]{-19, 26 });
        // Debate positions
        NAMED_LOCATIONS.put("debate_podium_a",    new int[]{  4,  2 });
        NAMED_LOCATIONS.put("debate_podium_b",    new int[]{  8,  2 });
        NAMED_LOCATIONS.put("debate_audience_1",  new int[]{  5, -2 });
        NAMED_LOCATIONS.put("debate_audience_2",  new int[]{  6, -2 });
        NAMED_LOCATIONS.put("debate_audience_3",  new int[]{  7, -2 });
    }

    // ─── Scheduler ────────────────────────────────────────────────────────────

    private BukkitTask tickTask;

    // ─── Constructor ──────────────────────────────────────────────────────────

    /**
     * @param plugin            The plugin instance.
     * @param locationRegistry  The shared LocationRegistry (may be null for backwards
     *                          compatibility, in which case only the static map is used).
     */
    public RoutineManager(GreenPartyPlugin plugin, LocationRegistry locationRegistry) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.locationRegistry = locationRegistry;
        if (locationRegistry != null) {
            locationRegistry.setLogger(log);
        }

        // Load routines from config
        RoutineLoader loader = new RoutineLoader(plugin, this, locationRegistry);
        loader.loadAll();

        // Main tick loop: 1 tick = 50ms
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Legacy no-registry constructor — kept for compile safety if called elsewhere. */
    public RoutineManager(GreenPartyPlugin plugin) {
        this(plugin, null);
    }

    // ─── Main Tick Loop ───────────────────────────────────────────────────────

    private void tick() {
        long currentTick = plugin.getServer().getCurrentTick();

        // 1. Start any pending routines whose delay has elapsed
        processPendingQueue(currentTick);

        // 2. Advance movement for all NPCs with move targets
        tickMovement();

        // 3. Advance each NPC's routine state
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) return;

        for (Map.Entry<String, NpcRoutineState> entry : new HashMap<>(npcStates).entrySet()) {
            String npcName = entry.getKey();
            NpcRoutineState state = entry.getValue();

            if (state.isIdle()) continue;

            // Decrement wait timer
            if (state.isWaiting()) {
                state.tickWait();
                continue;
            }

            // Find the NPC entity
            Entity npc = findNpc(npcName);
            if (npc == null) {
                log.warning("[RoutineManager] NPC not found during tick: " + npcName);
                state.interrupt();
                continue;
            }

            Routine routine = state.getCurrentRoutine();
            List<RoutineStep> steps = routine.getSteps();

            if (state.getStepIndex() >= steps.size()) {
                // Routine complete
                completeRoutine(npcName, state, routine);
                continue;
            }

            RoutineStep step = steps.get(state.getStepIndex());

            // Execute the step
            int pauseTicks = step.execute(npc, state, this);

            // Handle GroupSayStep re-entrancy:
            // GroupSayStep is executed once per dialogue line. It re-enters until done.
            // When not done: pause, but DON'T advance stepIndex (re-fire same step next time).
            // When done: advance normally.
            if (step instanceof GroupSayStep gss) {
                if (!gss.isDone(state)) {
                    // Still more lines to speak — stay on this step, just pause
                    state.setWaitTicksDirect(pauseTicks);
                } else {
                    // All lines spoken — advance to next step
                    state.advanceStep(pauseTicks);
                }
            } else {
                // Check if a ConditionalStep set a skip
                int skipSteps = state.getScratchInt("conditional_skip", 0);
                if (skipSteps > 0) {
                    state.setScratch("conditional_skip", 0);
                    // Skip N steps (skip_steps = how many to skip after the conditional)
                    // We already executed the conditional, now skip N more
                    state.skipSteps(skipSteps);
                } else {
                    state.advanceStep(pauseTicks);
                }
            }
        }
    }

    // ─── Routine Lifecycle ────────────────────────────────────────────────────

    /**
     * Start a routine immediately for all its required NPCs.
     * If any required NPC is mid-routine at the same or higher priority, the
     * start is blocked and false is returned.
     *
     * @param routineName The routine identifier from the registry
     * @param priority    Override priority (use -1 to use the routine's default)
     * @return true if the routine started successfully
     */
    public boolean startRoutine(String routineName, int priority) {
        Routine routine = routineRegistry.get(routineName);
        if (routine == null) {
            log.warning("[RoutineManager] Unknown routine: " + routineName);
            return false;
        }

        // Check cooldown
        long currentTick = plugin.getServer().getCurrentTick();
        long lastRun = lastRunTick.getOrDefault(routineName, 0L);
        if (currentTick - lastRun < routine.getCooldownTicks()) {
            log.info("[RoutineManager] Routine '" + routineName + "' is on cooldown.");
            return false;
        }

        int effectivePriority = (priority >= 0) ? priority : routine.getPriority();

        // Check all required NPCs are available
        for (String npcName : routine.getRequiredNpcs()) {
            NpcRoutineState state = npcStates.computeIfAbsent(npcName, k -> new NpcRoutineState());
            if (!state.isIdle() && state.getPriority() >= effectivePriority) {
                log.info("[RoutineManager] NPC '" + npcName + "' is locked by higher/equal priority routine — blocking start of '" + routineName + "'");
                return false;
            }
        }

        // All clear — interrupt any lower-priority routines and start
        for (String npcName : routine.getRequiredNpcs()) {
            NpcRoutineState state = npcStates.get(npcName);
            if (!state.isIdle()) {
                log.info("[RoutineManager] Interrupting routine '" + state.getCurrentRoutine().getName()
                    + "' for NPC '" + npcName + "' (preempted by '" + routineName + "')");
                state.interrupt();
                plugin.getNpcScheduleManager().resumeNpc(npcName);
            }
            state.startRoutine(routine);

            // Tell NPCScheduleManager to stop moving this NPC
            plugin.getNpcScheduleManager().pauseNpc(npcName);
        }

        log.info("[RoutineManager] Started routine: " + routineName
            + " (NPCs: " + routine.getRequiredNpcs() + ")");
        return true;
    }

    /** Stop a specific NPC's current routine and return them to the schedule. */
    public void stopNpcRoutine(String npcName) {
        NpcRoutineState state = npcStates.get(npcName);
        if (state == null || state.isIdle()) return;

        log.info("[RoutineManager] Interrupting routine for NPC: " + npcName);
        state.interrupt();
        npcMoveTargets.remove(findNpcUuid(npcName));
        plugin.getNpcScheduleManager().resumeNpc(npcName);
    }

    private void completeRoutine(String npcName, NpcRoutineState state, Routine routine) {
        log.info("[RoutineManager] Routine '" + routine.getName() + "' completed for NPC: " + npcName);
        lastRunTick.put(routine.getName(), (long) plugin.getServer().getCurrentTick());
        state.complete();
        plugin.getNpcScheduleManager().resumeNpc(npcName);
    }

    /** Queue a routine to start after a delay. Called by ChainRoutineStep. */
    public void scheduleRoutine(String routineName, int delayTicks, int priority) {
        long startAt = plugin.getServer().getCurrentTick() + delayTicks;
        pendingNames.add(routineName);
        pendingQueue.add(new long[]{startAt, priority});
    }

    private void processPendingQueue(long currentTick) {
        for (int i = pendingQueue.size() - 1; i >= 0; i--) {
            long[] entry = pendingQueue.get(i);
            if (currentTick >= entry[0]) {
                String name = pendingNames.get(i);
                int prio = (int) entry[1];
                startRoutine(name, prio);
                pendingQueue.remove(i);
                pendingNames.remove(i);
            }
        }
    }

    // ─── Movement Tick ────────────────────────────────────────────────────────

    private void tickMovement() {
        Iterator<Map.Entry<UUID, Location>> it = npcMoveTargets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Location> entry = it.next();
            Entity npc = Bukkit.getEntity(entry.getKey());
            Location target = entry.getValue();

            if (npc == null) { it.remove(); continue; }

            Location current = npc.getLocation();
            if (current.getWorld() != target.getWorld()) { it.remove(); continue; }

            double dist = current.distance(target);
            if (dist < 1.5) {
                it.remove(); // Arrived
                continue;
            }

            double dx = target.getX() - current.getX();
            double dz = target.getZ() - current.getZ();
            double mag = Math.sqrt(dx * dx + dz * dz);
            double speed = 0.22;

            double newX = current.getX() + (dx / mag) * speed;
            double newZ = current.getZ() + (dz / mag) * speed;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

            World world = current.getWorld();
            int blockY = world.getHighestBlockYAt((int) newX, (int) newZ);
            Location newLoc = new Location(world, newX, blockY + 1, newZ, yaw, 0);
            npc.teleport(newLoc);
        }
    }

    // ─── NPC Lookup Helpers ───────────────────────────────────────────────────

    /**
     * Find an NPC entity by stripped name. Returns null if not found in the Verdant world.
     */
    public Entity findNpc(String name) {
        if (name == null) return null;
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) return null;
        String stripped = name.replaceAll("§.", "").toLowerCase();
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                String npcName = e.getCustomName();
                if (npcName != null && npcName.replaceAll("§.", "").equalsIgnoreCase(stripped)) {
                    return e;
                }
            }
        }
        return null;
    }

    private UUID findNpcUuid(String name) {
        Entity e = findNpc(name);
        return e != null ? e.getUniqueId() : null;
    }

    public String getNpcDisplayName(Entity npc) {
        if (npc == null) return "Unknown";
        String name = npc.getCustomName();
        if (name == null) return "Unknown";
        return name.replaceAll("§.", "");
    }

    public String getNearbyPlayerName(Entity npc) {
        if (npc == null) return "everyone";
        World world = npc.getWorld();
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(npc.getLocation()) < 30) return p.getName();
        }
        return "everyone";
    }

    /** Tell RoutineManager that this NPC should move to the given location. */
    public void setNpcMoveTarget(Entity npc, Location target) {
        npcMoveTargets.put(npc.getUniqueId(), target);
    }

    /** Arm swing animation helper. */
    public void swingArm(Entity entity) {
        if (entity instanceof LivingEntity le) {
            le.swingMainHand();
        }
    }

    // ─── Location Resolution ──────────────────────────────────────────────────

    /**
     * Resolve a location key to a world Location.
     *
     * Resolution order:
     *   1. LocationRegistry (structure-accurate named locations)
     *   2. Static NAMED_LOCATIONS fallback (spawn-relative offsets)
     *   3. Coordinate string "x,y,z"
     *
     * Logs a warning and returns null if no match is found.
     */
    public Location resolveLocation(String key, World world) {
        if (key == null || world == null) return null;

        // 1. Try LocationRegistry first (most accurate — uses real structure coordinates)
        if (locationRegistry != null) {
            Location registryLoc = locationRegistry.getLocation(key);
            if (registryLoc != null) {
                // Ensure the location's world matches (registry stores world by reference)
                if (registryLoc.getWorld() != null) {
                    return registryLoc;
                }
            }
        }

        // 2. Try static named location (spawn-relative fallback)
        int[] offset = NAMED_LOCATIONS.get(key.toLowerCase().replace(" ", "_"));
        if (offset != null) {
            Location spawn = world.getSpawnLocation();
            int x = spawn.getBlockX() + offset[0];
            int z = spawn.getBlockZ() + offset[1];
            int y = world.getHighestBlockYAt(x, z) + 1;
            return new Location(world, x + 0.5, y, z + 0.5);
        }

        // 3. Try coordinate string "x,y,z"
        String[] parts = key.split(",");
        if (parts.length == 3) {
            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                return new Location(world, x, y, z);
            } catch (NumberFormatException ignored) {}
        }

        log.warning("[RoutineManager] Unknown location key: '" + key
            + "' — check LocationRegistry or NAMED_LOCATIONS.");
        return null;
    }

    /**
     * Register spawn-relative debate podium locations in the LocationRegistry.
     * Called from GreenPartyPlugin after structures are built and the registry
     * has been populated with structure locations.
     *
     * @param registry The shared LocationRegistry to register into.
     * @param world    The Verdant world (provides spawn location).
     */
    public void registerDebatePodiums(LocationRegistry registry, World world) {
        if (registry == null || world == null) return;

        Location spawn = world.getSpawnLocation();

        registry.registerOffset("debate_podium_a",   spawn, -3, 0,  0);
        registry.registerOffset("debate_podium_b",   spawn,  3, 0,  0);
        registry.registerOffset("debate_audience_1", spawn,  0, 0,  5);
        registry.registerOffset("debate_audience_2", spawn, -5, 0,  0);
        registry.registerOffset("debate_audience_3", spawn,  5, 0,  0);
        registry.registerOffset("debate_audience_4", spawn,  0, 0, -3);

        // Also register "spawn" itself for convenience
        registry.register("spawn", spawn.clone());

        log.info("[RoutineManager] Registered debate podium & audience locations.");
    }

    /** Expose the LocationRegistry for commands that want to inspect it. */
    public LocationRegistry getLocationRegistry() {
        return locationRegistry;
    }

    // ─── Condition Checker ────────────────────────────────────────────────────

    /**
     * Evaluate a named condition for ConditionalStep.
     */
    public boolean checkCondition(String condition, Entity contextNpc) {
        World world = contextNpc.getWorld();
        return switch (condition.toLowerCase()) {
            case "players_online"   -> !world.getPlayers().isEmpty();
            case "players_nearby"   -> isPlayerNearAnyNpc(world, 30);
            case "eco_score_high"   -> getWorldAverageEcoScore(world) > 50;
            case "eco_score_low"    -> getWorldAverageEcoScore(world) < 20;
            case "time_day"         -> world.getTime() < 12000;
            case "time_night"       -> world.getTime() >= 12000;
            case "debate_not_active"-> !plugin.getDebateManager().isDebateActive();
            default -> {
                log.warning("[RoutineManager] Unknown condition: " + condition);
                yield false;
            }
        };
    }

    private boolean isPlayerNearAnyNpc(World world, double radius) {
        for (Player p : world.getPlayers()) {
            for (Entity e : world.getEntities()) {
                if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                    if (p.getLocation().distance(e.getLocation()) <= radius) return true;
                }
            }
        }
        return false;
    }

    private double getWorldAverageEcoScore(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return 50; // Default mid-range
        return players.stream()
            .mapToInt(p -> plugin.getEcoScoreManager().getEcoScore(p))
            .average()
            .orElse(50);
    }

    // ─── Registry Access ──────────────────────────────────────────────────────

    public void registerRoutine(Routine routine) {
        routineRegistry.put(routine.getName(), routine);
        log.info("[RoutineManager] Registered routine: " + routine.getName());
    }

    public Map<String, Routine> getRoutineRegistry() {
        return Collections.unmodifiableMap(routineRegistry);
    }

    public Routine getRoutine(String name) {
        return routineRegistry.get(name);
    }

    public NpcRoutineState getNpcState(String npcName) {
        return npcStates.getOrDefault(npcName, new NpcRoutineState());
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    public void cancelAll() {
        if (tickTask != null) tickTask.cancel();
        npcStates.values().forEach(NpcRoutineState::interrupt);
    }
}
