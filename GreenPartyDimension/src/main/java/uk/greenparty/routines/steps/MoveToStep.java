package uk.greenparty.routines.steps;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * MoveToStep — Walk or teleport an NPC to a named location or world coordinates.
 *
 * Speed.WALK   : NPC moves ~0.22 blocks/tick (matches NPCScheduleManager pace).
 *                The step returns an estimated tick count based on distance.
 * Speed.TELEPORT: Instant. Step returns 0 ticks (continue immediately).
 *
 * Named locations (e.g. "council_chamber", "tree_farm") are resolved by
 * RoutineManager#resolveLocation(), which looks up zone offsets from spawn.
 *
 * YAML:
 *   - type: MOVE_TO
 *     npc: "Councillor Wheatgrass"   # optional — defaults to "self" (current NPC)
 *     location: "council_chamber"    # named zone OR "x,y,z" coordinate string
 *     speed: WALK                    # WALK | TELEPORT
 */
public class MoveToStep implements RoutineStep {

    public enum Speed { WALK, TELEPORT }

    private final String targetNpcName; // null = "self" (the NPC executing this step)
    private final String locationKey;   // named location or "x,y,z"
    private final Speed speed;

    public MoveToStep(String targetNpcName, String locationKey, Speed speed) {
        this.targetNpcName = targetNpcName;
        this.locationKey = locationKey;
        this.speed = speed;
    }

    @Override
    public StepType getType() { return StepType.MOVE_TO; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        // If a different NPC is named, resolve it; otherwise use self
        Entity target = (targetNpcName != null)
            ? manager.findNpc(targetNpcName)
            : npc;

        if (target == null) return 0; // NPC not found — skip gracefully

        Location dest = manager.resolveLocation(locationKey, target.getWorld());
        if (dest == null) return 0; // Unknown location — skip

        if (speed == Speed.TELEPORT) {
            target.teleport(dest);
            return 0; // Instant — advance immediately
        }

        // WALK: register target in RoutineManager's movement map and estimate ticks
        double dist = target.getLocation().distance(dest);
        int estimatedTicks = (int) Math.ceil(dist / 0.22); // ~0.22 blocks/tick

        manager.setNpcMoveTarget(target, dest);
        return estimatedTicks + 5; // +5 tick buffer for arrival
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return String.format("MoveTo[%s → %s, speed=%s]", who, locationKey, speed);
    }
}
