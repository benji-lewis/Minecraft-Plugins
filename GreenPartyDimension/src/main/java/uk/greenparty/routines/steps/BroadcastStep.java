package uk.greenparty.routines.steps;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * BroadcastStep — Send a message to the world or the whole server.
 *
 * Used for dramatic announcements at the start/end of major routines
 * ("§2The Council is now in session.") or narrative flavour text that
 * frames what's about to happen.
 *
 * chatOnly = true  → message sent only to players in the Verdant world
 * chatOnly = false → server-wide broadcast (all worlds)
 *
 * YAML:
 *   - type: BROADCAST
 *     message: "§2§lThe Green Party Council is now in session."
 *     chat_only: true
 */
public class BroadcastStep implements RoutineStep {

    private final String message;
    private final boolean chatOnly; // true = world-only, false = server-wide

    public BroadcastStep(String message, boolean chatOnly) {
        this.message = message;
        this.chatOnly = chatOnly;
    }

    @Override
    public StepType getType() { return StepType.BROADCAST; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        if (chatOnly) {
            // Only players in the same world
            npc.getWorld().getPlayers().forEach(p -> p.sendMessage(message));
        } else {
            // Server-wide
            Bukkit.broadcastMessage(message);
        }
        return 0;
    }

    @Override
    public String describe() {
        String scope = chatOnly ? "world" : "server";
        return String.format("Broadcast[%s, \"%s\"]", scope,
            message.length() > 40 ? message.substring(0, 40) + "..." : message);
    }
}
