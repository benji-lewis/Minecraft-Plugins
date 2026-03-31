package uk.greenparty.routines.steps;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * TurnFaceStep — Rotate an NPC to face a named location, coordinate, or another NPC.
 *
 * Uses yaw calculation to point the entity in the right direction without moving it.
 * Useful for:
 *   - Sitting councillors facing the presiding chair
 *   - An NPC turning to face whoever just spoke
 *   - Group inspection: all councillors face the tree/object being discussed
 *
 * Target types:
 *   Named location  : "council_chamber", "tree_farm" (resolved same as MoveTo)
 *   NPC name        : "Councillor Wheatgrass" (entity looked up by name)
 *   Coordinate      : "100,55,-200" (x,y,z string)
 *
 * YAML:
 *   - type: TURN_FACE
 *     npc: "Recycling Evangelist Bramble"
 *     face_target: "Councillor Wheatgrass"   # can be NPC name, location key, or coords
 */
public class TurnFaceStep implements RoutineStep {

    private final String targetNpcName;   // NPC doing the turning (null = self)
    private final String faceTargetKey;   // What to face (NPC name, location key, or "x,y,z")

    public TurnFaceStep(String targetNpcName, String faceTargetKey) {
        this.targetNpcName = targetNpcName;
        this.faceTargetKey = faceTargetKey;
    }

    @Override
    public StepType getType() { return StepType.TURN_FACE; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity turner = (targetNpcName != null) ? manager.findNpc(targetNpcName) : npc;
        if (turner == null) return 0;

        // Try to resolve as NPC name first, then as named location
        Location faceLoc = null;
        Entity faceEntity = manager.findNpc(faceTargetKey);
        if (faceEntity != null) {
            faceLoc = faceEntity.getLocation();
        } else {
            faceLoc = manager.resolveLocation(faceTargetKey, turner.getWorld());
        }

        if (faceLoc == null) return 0;

        // Calculate yaw angle toward the target
        Location from = turner.getLocation();
        double dx = faceLoc.getX() - from.getX();
        double dz = faceLoc.getZ() - from.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        Location newLoc = from.clone();
        newLoc.setYaw(yaw);
        turner.teleport(newLoc);

        return 0; // Instant
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return String.format("TurnFace[%s → %s]", who, faceTargetKey);
    }
}
