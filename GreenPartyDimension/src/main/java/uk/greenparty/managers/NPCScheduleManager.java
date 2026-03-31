package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * NPCScheduleManager — Councillors on the move!
 *
 * The Green Party Council has realised that standing at spawn all day
 * is not a good look. They've implemented a "Dynamic Presence Policy"
 * (Motion 77c, passed 6-3 with one abstention from Treasurer Daisy
 * who was auditing the tote bag stockpile).
 *
 * NPCs now move between 4 zones on a schedule:
 *   - Council Chamber (spawn area)     — Morning governance sessions
 *   - Compost Processing Centre        — Midday soil management
 *   - Tree Farm                        — Afternoon reforestation
 *   - Recycling Centre                 — Evening waste processing
 *
 * NPCs move ~0.22 blocks per tick toward their destination (teleport-based).
 * AI is disabled on villagers so they don't wander back.
 *
 * Bug fixes in v1.4.5:
 *   - AI disabled on NPCs (NpcManager) so movement isn't overridden by villager pathfinding
 *   - Schedule type now keyed by stable NPC name, not entity list index (which was non-deterministic)
 *   - Zone arrival announcements now fire on actual arrival, not on schedule assignment
 *   - /npc info now shows real coordinates + in-transit status, not just the target zone name
 *
 * Version 1.4.5 — Bugfix: NPC movement and location dialogue
 */
public class NPCScheduleManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // zone name -> waypoint offset from spawn
    private static final Map<String, int[]> ZONE_OFFSETS = new LinkedHashMap<>();
    static {
        ZONE_OFFSETS.put("Council Chamber",      new int[]{ 5,   3});
        ZONE_OFFSETS.put("Compost Processing",   new int[]{ 30,  8});
        ZONE_OFFSETS.put("Tree Farm",            new int[]{-20, 25});
        ZONE_OFFSETS.put("Recycling Centre",     new int[]{ 15,-30});
    }

    // NPC name (stripped) -> current assigned zone name (destination)
    private final Map<String, String> npcZone = new HashMap<>();
    // NPC name -> target location (the waypoint they're walking to)
    private final Map<String, Location> npcTarget = new HashMap<>();
    // NPC name -> whether arrival announcement has fired for this zone visit
    private final Map<String, Boolean> npcArrivalAnnounced = new HashMap<>();
    // NPC name -> stable schedule type (0 or 1), assigned on first encounter
    private final Map<String, Integer> npcScheduleType = new HashMap<>();

    private BukkitTask movementTask;
    private BukkitTask scheduleTask;

    // Schedule slots: hour (0-23 in-game time) -> zone
    // In-game time: dawn=0, noon=6000, sunset=12000, midnight=18000
    // Mapping 0-24000 ticks to 0-24h: 1000 ticks = 1 hour
    // Zone schedules per councillor role
    private static final String[][] COUNCILLOR_SCHEDULES = {
        // Odd councillors: chamber morning, compost midday, tree farm evening, recycling night
        {"Council Chamber", "Council Chamber", "Council Chamber", "Compost Processing",
         "Compost Processing", "Compost Processing", "Tree Farm", "Tree Farm",
         "Tree Farm", "Recycling Centre", "Recycling Centre", "Recycling Centre"},
        // Even councillors: compost morning, tree farm midday, recycling evening, chamber night
        {"Compost Processing", "Compost Processing", "Tree Farm", "Tree Farm",
         "Recycling Centre", "Recycling Centre", "Council Chamber", "Council Chamber",
         "Compost Processing", "Tree Farm", "Recycling Centre", "Council Chamber"},
    };

    // Greeting messages when NPC approaches a player with good eco-score
    private static final String[] GREET_HIGH_ECO = {
        "§a%s says: Ah, %s! A true environmental champion walks among us!",
        "§a%s says: The Council notes your exemplary green credentials, %s!",
        "§a%s says: Oh, %s! Your eco-score fills my heart with renewable energy!",
        "§a%s says: Brilliant work, %s! You're practically a composter yourself!",
        "§a%s says: The Greenness Index just ticked up. That was you, %s. Well done.",
    };

    // Heckling messages when NPC approaches a player with bad eco-score
    private static final String[] HECKLE_LOW_ECO = {
        "§c%s says: Hmm. §7I see your eco-score is... disappointing, %s.",
        "§c%s says: §7%s! The council is watching your environmental record. Closely.",
        "§c%s says: I've filed a pre-emptive violation notice for %s. Just in case.",
        "§c%s says: §7The Greenness Index dips whenever you walk by, %s. Interesting.",
        "§c%s says: §7I don't want to alarm you, %s, but your carbon footprint is showing.",
    };

    // Zone transition announcements (fired when NPC actually arrives, not when assigned)
    private static final Map<String, String> ZONE_ARRIVAL_MSG = new HashMap<>();
    static {
        ZONE_ARRIVAL_MSG.put("Council Chamber",    "§2The Council is convening in the Chamber. Governance is happening.");
        ZONE_ARRIVAL_MSG.put("Compost Processing", "§2The Council has relocated to Compost Processing. Mid-session soil management.");
        ZONE_ARRIVAL_MSG.put("Tree Farm",          "§2The Council is at the Tree Farm. Reforestation policy review in progress.");
        ZONE_ARRIVAL_MSG.put("Recycling Centre",   "§2The Council is at the Recycling Centre. Evening waste processing commenced.");
    }

    public NPCScheduleManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;

        // NPC movement tick (every 4 ticks = ~0.2s for smooth movement)
        movementTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMovement, 20L, 4L);

        // Schedule check every 30 seconds (reassign zones based on in-game time)
        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateSchedules, 20L * 30, 20L * 30);
    }

    // ─── Zone & Schedule Logic ────────────────────────────────────────────────

    private void updateSchedules() {
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) return;

        long time = world.getTime(); // 0-24000
        int hourSlot = (int)(time / 2000) % 12; // 12 slots of 2000 ticks each

        List<Entity> npcs = getNpcs(world);
        for (Entity npc : npcs) {
            String npcName = stripColor(npc.getCustomName());
            if (npcName == null) continue;

            // Skip NPCs locked into a routine — RoutineManager owns their movement
            if (pausedNpcs.contains(npcName)) continue;

            // Assign a stable schedule type based on name hash (not list index, which is non-deterministic)
            int scheduleType = npcScheduleType.computeIfAbsent(npcName,
                k -> Math.abs(k.hashCode()) % 2);

            String targetZone = COUNCILLOR_SCHEDULES[scheduleType][hourSlot];
            String currentZone = npcZone.getOrDefault(npcName, "");

            if (!targetZone.equals(currentZone)) {
                npcZone.put(npcName, targetZone);
                Location target = getZoneLocation(world, targetZone);
                npcTarget.put(npcName, target);
                // Reset arrival announcement flag when destination changes
                npcArrivalAnnounced.put(npcName, false);
            }
        }
    }

    private void tickMovement() {
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) return;

        List<Entity> npcs = getNpcs(world);
        for (Entity npc : npcs) {
            String npcName = stripColor(npc.getCustomName());
            if (npcName == null) continue;

            // Skip NPCs locked into a routine
            if (pausedNpcs.contains(npcName)) continue;

            Location target = npcTarget.get(npcName);
            if (target == null) continue;

            Location current = npc.getLocation();
            if (current.getWorld() != target.getWorld()) continue;

            double dist = current.distance(target);
            if (dist < 2.0) {
                // Arrived at destination
                boolean alreadyAnnounced = npcArrivalAnnounced.getOrDefault(npcName, false);
                if (!alreadyAnnounced) {
                    // Fire zone arrival announcement now that NPC has physically arrived
                    npcArrivalAnnounced.put(npcName, true);
                    if (!world.getPlayers().isEmpty() && random.nextInt(3) == 0) {
                        String zone = npcZone.getOrDefault(npcName, "");
                        String msg = ZONE_ARRIVAL_MSG.getOrDefault(zone, "");
                        if (!msg.isEmpty()) {
                            world.getPlayers().forEach(p -> p.sendMessage("§8[NPC Schedule] " + msg));
                        }
                    }
                }

                // Check if there are nearby players to greet/heckle
                checkNearbyPlayers(npc, npcName, current);
                continue;
            }

            // Move ~0.22 blocks toward target (natural walking pace)
            double dx = target.getX() - current.getX();
            double dz = target.getZ() - current.getZ();
            double magnitude = Math.sqrt(dx * dx + dz * dz);

            if (magnitude > 0) {
                double speed = 0.22;
                double newX = current.getX() + (dx / magnitude) * speed;
                double newZ = current.getZ() + (dz / magnitude) * speed;

                // Face direction of travel
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

                Location newLoc = new Location(world, newX, current.getY(), newZ, yaw, 0);

                // Ensure on ground
                int blockY = world.getHighestBlockYAt((int)newX, (int)newZ);
                newLoc.setY(blockY + 1);

                npc.teleport(newLoc);
            }
        }
    }

    private void checkNearbyPlayers(Entity npc, String npcName, Location npcLoc) {
        World world = npcLoc.getWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(npcLoc) < 5.0) {
                // Random chance to greet (don't spam every tick)
                if (random.nextInt(600) != 0) continue; // ~once per 30s at 20tps

                int ecoScore = plugin.getEcoScoreManager().getEcoScore(player);
                String[] lines;
                if (ecoScore >= 50) {
                    lines = GREET_HIGH_ECO;
                } else {
                    lines = HECKLE_LOW_ECO;
                }

                String msg = String.format(
                    lines[random.nextInt(lines.length)],
                    npcName, player.getName()
                );
                player.sendMessage(msg);
            }
        }
    }

    // ─── Command Handler ──────────────────────────────────────────────────────

    public void handleNpcInfoCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /npc info <npc_name>");
            return;
        }

        String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).toLowerCase();

        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) {
            player.sendMessage("§cThe Verdant Utopia is not loaded.");
            return;
        }

        List<Entity> npcs = getNpcs(world);
        Entity found = null;
        for (Entity npc : npcs) {
            String name = stripColor(npc.getCustomName());
            if (name != null && name.toLowerCase().contains(query)) {
                found = npc;
                break;
            }
        }

        if (found == null) {
            player.sendMessage("§cCouncillor not found: §7" + query);
            player.sendMessage("§8(They may be at a meeting. Or composting. Probably composting.)");
            return;
        }

        String npcName = stripColor(found.getCustomName());

        // Report actual current coordinates, not just the assigned zone name
        Location loc = found.getLocation();
        Location target = npcTarget.get(npcName);
        String assignedZone = npcZone.getOrDefault(npcName, "§7Unknown (probably lost)");

        // Determine if in transit or arrived
        boolean arrived = target == null || loc.distance(target) < 2.0;
        String statusStr = arrived
            ? "§aArrived"
            : "§eIn transit → §7" + assignedZone;

        World w = plugin.getDimensionManager().getVerdantWorld();
        long time = w != null ? w.getTime() : 0;
        int hourSlot = (int)(time / 2000) % 12;

        player.sendMessage("§2§l===== NPC Info =====");
        player.sendMessage("§aName: §7" + npcName);
        player.sendMessage("§aAssigned Zone: §7" + assignedZone);
        player.sendMessage("§aStatus: " + statusStr);
        player.sendMessage("§aActual Location: §7" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ());
        if (target != null && !arrived) {
            player.sendMessage("§aDestination: §7" + (int)target.getX() + ", " + (int)target.getY() + ", " + (int)target.getZ());
        }
        player.sendMessage("§aCurrent Schedule Slot: §7" + hourSlot + "/11");

        int scheduleType = npcScheduleType.getOrDefault(npcName, Math.abs(npcName.hashCode()) % 2);
        player.sendMessage("§aFull Schedule:");
        String[] schedule = COUNCILLOR_SCHEDULES[scheduleType];
        String[] slotNames = {"Dawn", "Early Morning", "Morning", "Late Morning",
                              "Noon", "Afternoon", "Late Afternoon", "Evening",
                              "Dusk", "Night", "Late Night", "Midnight"};
        for (int i = 0; i < schedule.length; i++) {
            String marker = (i == hourSlot) ? "§e▶ " : "§8  ";
            player.sendMessage(marker + slotNames[i] + ": §7" + schedule[i]);
        }
        player.sendMessage("§2§l====================");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Location getZoneLocation(World world, String zoneName) {
        int[] offset = ZONE_OFFSETS.getOrDefault(zoneName, new int[]{0, 0});
        Location spawn = world.getSpawnLocation();
        int x = spawn.getBlockX() + offset[0];
        int z = spawn.getBlockZ() + offset[1];
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private List<Entity> getNpcs(World world) {
        List<Entity> result = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                result.add(e);
            }
        }
        return result;
    }

    private String stripColor(String s) {
        if (s == null) return null;
        return s.replaceAll("§.", "");
    }

    public String getNpcCurrentZone(String npcName) {
        return npcZone.getOrDefault(npcName, "Council Chamber");
    }

    /**
     * Returns the actual location of an NPC by name, or null if not found.
     * Used by dialogue systems to report accurate position instead of assigned zone.
     */
    public Location getNpcActualLocation(String npcName) {
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) return null;
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                String name = stripColor(e.getCustomName());
                if (npcName.equals(name)) return e.getLocation();
            }
        }
        return null;
    }

    // ─── Routine Integration: Pause / Resume ─────────────────────────────────

    /**
     * Paused NPC names — these will be skipped in tickMovement() and updateSchedules().
     * Set by RoutineManager when a routine locks an NPC; cleared on routine completion.
     */
    private final Set<String> pausedNpcs = new HashSet<>();

    /**
     * Pause NPCScheduleManager movement for a specific NPC.
     * Called by RoutineManager when a routine starts for that NPC.
     * The NPC will not receive schedule reassignments or movement ticks until resumed.
     *
     * @param npcName Stripped NPC name (no colour codes)
     */
    public void pauseNpc(String npcName) {
        pausedNpcs.add(npcName);
        // Clear any pending movement target so they don't slide to their old destination
        npcTarget.remove(npcName);
    }

    /**
     * Resume NPCScheduleManager movement for an NPC after a routine completes.
     * Forces an immediate schedule recalculation so the NPC resumes their timetable.
     *
     * @param npcName Stripped NPC name (no colour codes)
     */
    public void resumeNpc(String npcName) {
        pausedNpcs.remove(npcName);
        // Trigger a fresh zone assignment on next update cycle
        npcZone.remove(npcName);
        npcArrivalAnnounced.remove(npcName);
    }

    /**
     * Check whether a specific NPC is paused by an active routine.
     */
    public boolean isNpcPaused(String npcName) {
        return pausedNpcs.contains(npcName);
    }

    public void cancelAll() {
        if (movementTask != null) movementTask.cancel();
        if (scheduleTask != null) scheduleTask.cancel();
    }
}
