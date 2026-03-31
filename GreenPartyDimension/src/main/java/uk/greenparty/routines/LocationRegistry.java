package uk.greenparty.routines;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * LocationRegistry — A thread-safe named location store for the routine system.
 *
 * Structures and spawn-relative points register their internal locations here
 * during server start-up. RoutineManager resolves MOVE_TO and TURN_FACE step
 * targets by looking up names in this registry.
 *
 * Design notes:
 *   - Uses ConcurrentHashMap for safe reads from the main tick thread while
 *     locations are registered from the scheduler thread on startup.
 *   - All names are normalised to lower-case + underscores on registration and
 *     lookup to prevent case-sensitivity mismatches in YAML configs.
 *   - registerOffset() resolves the full world Location at registration time,
 *     so the registry stores concrete Bukkit Locations (world, x, y, z).
 *
 * Usage:
 *   // In StructureManager after building:
 *   registry.registerOffset("council_chamber", center, 0, 1, 0);
 *
 *   // In RoutineManager tick:
 *   Location dest = registry.getLocation("council_chamber");
 */
public class LocationRegistry {

    private final Map<String, Location> locations = new ConcurrentHashMap<>();

    // Optional logger for debug output; set via setLogger() after construction.
    private Logger log = null;

    // ─── Constructor ──────────────────────────────────────────────────────────

    /** No-arg constructor — populate on demand via register/registerOffset. */
    public LocationRegistry() {}

    // ─── Optional logger ──────────────────────────────────────────────────────

    public void setLogger(Logger logger) {
        this.log = logger;
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * Register a named location with an exact Bukkit Location.
     * Name is normalised to lower_snake_case.
     */
    public void register(String name, Location loc) {
        if (name == null || loc == null) return;
        String key = normalise(name);
        locations.put(key, loc.clone());
        if (log != null) {
            log.fine("[LocationRegistry] Registered '" + key + "' → "
                + formatLoc(loc));
        }
    }

    /**
     * Register a location as an offset from a base point.
     * The resolved Location is stored; the offsets are applied once at registration time.
     *
     * @param name  Location key (normalised to lower_snake_case)
     * @param base  Base Location (e.g. structure centre)
     * @param dx    X offset from base
     * @param dy    Y offset from base
     * @param dz    Z offset from base
     */
    public void registerOffset(String name, Location base, int dx, int dy, int dz) {
        if (name == null || base == null || base.getWorld() == null) return;
        Location resolved = new Location(
            base.getWorld(),
            base.getBlockX() + dx + 0.5,
            base.getBlockY() + dy,
            base.getBlockZ() + dz + 0.5
        );
        register(name, resolved);
    }

    // ─── Retrieval ────────────────────────────────────────────────────────────

    /**
     * Retrieve a named location, or null if not registered.
     * Returns a clone so callers cannot mutate the stored Location.
     */
    public Location getLocation(String name) {
        if (name == null) return null;
        Location loc = locations.get(normalise(name));
        return loc != null ? loc.clone() : null;
    }

    /**
     * Returns true if the given name is registered.
     */
    public boolean has(String name) {
        if (name == null) return false;
        return locations.containsKey(normalise(name));
    }

    /**
     * Returns an unmodifiable view of all registered location names.
     * Useful for debugging and the /location list command.
     */
    public Collection<String> listAll() {
        return Collections.unmodifiableSet(locations.keySet());
    }

    /** Returns the total number of registered locations. */
    public int size() {
        return locations.size();
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static String normalise(String name) {
        return name.toLowerCase().replace(" ", "_");
    }

    private static String formatLoc(Location loc) {
        if (loc.getWorld() == null) return "(no world)";
        return String.format("(%s %.1f, %.1f, %.1f)",
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
