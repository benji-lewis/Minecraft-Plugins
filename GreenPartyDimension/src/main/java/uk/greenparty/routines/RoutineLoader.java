package uk.greenparty.routines;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.routines.steps.*;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * RoutineLoader — Parses routines.yml and registers routines with RoutineManager.
 *
 * Each routine in the YAML has this structure:
 * <pre>
 * routines:
 *   council_session:
 *     description: "Formal council meeting in the chamber"
 *     required_npcs:
 *       - "Councillor Wheatgrass"
 *       - "Recycling Evangelist Bramble"
 *     duration_ticks: 3600
 *     cooldown_ticks: 12000
 *     priority: 5
 *     weight: 10
 *     steps:
 *       - type: BROADCAST
 *         message: "§2The Council is now in session."
 *         chat_only: true
 *       - type: MOVE_TO
 *         npc: "Councillor Wheatgrass"
 *         location: "council_chamber"
 *         speed: WALK
 *       - type: SAY
 *         npc: "Councillor Wheatgrass"
 *         message: "I call this session to order."
 *         pause_ticks: 80
 *       - type: GROUP_SAY
 *         pause_per_line_ticks: 80
 *         lines:
 *           - speaker: "Councillor Wheatgrass"
 *             message: "Motion 77c is on the table."
 *           - speaker: "Recycling Evangelist Bramble"
 *             message: "Are these chairs made from recycled materials?"
 * </pre>
 *
 * The loader is forgiving — unknown step types are logged and skipped.
 * Routines with no steps or invalid configs are skipped with a warning.
 */
public class RoutineLoader {

    private final GreenPartyPlugin plugin;
    private final RoutineManager manager;
    private final Logger log;

    /**
     * Optional location registry used to warn when a MOVE_TO or TURN_FACE step
     * references a location name that isn't registered. May be null if the registry
     * hasn't been wired in yet (structures registered after load).
     * Null-safe throughout — null means "skip validation".
     */
    private final LocationRegistry locationRegistry; // nullable

    private static final String ROUTINES_FILE = "routines.yml";

    /**
     * Primary constructor — accepts a LocationRegistry for load-time validation.
     * Pass null to disable location validation during step parsing.
     */
    public RoutineLoader(GreenPartyPlugin plugin, RoutineManager manager, LocationRegistry locationRegistry) {
        this.plugin = plugin;
        this.manager = manager;
        this.log = plugin.getLogger();
        this.locationRegistry = locationRegistry;
    }

    /** Legacy constructor — no location validation. */
    public RoutineLoader(GreenPartyPlugin plugin, RoutineManager manager) {
        this(plugin, manager, null);
    }

    // ─── Entry Point ──────────────────────────────────────────────────────────

    /**
     * Load all routines from routines.yml and register them.
     * If routines.yml doesn't exist in the plugin data folder, save the default.
     */
    public void loadAll() {
        File file = new File(plugin.getDataFolder(), ROUTINES_FILE);

        if (!file.exists()) {
            // Save default from resources/ if present
            if (plugin.getResource(ROUTINES_FILE) != null) {
                plugin.saveResource(ROUTINES_FILE, false);
                log.info("[RoutineLoader] Saved default routines.yml");
            } else {
                log.warning("[RoutineLoader] routines.yml not found and no default in jar. No routines loaded.");
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection routinesSection = config.getConfigurationSection("routines");

        if (routinesSection == null) {
            log.warning("[RoutineLoader] 'routines:' section missing in routines.yml");
            return;
        }

        int count = 0;
        for (String routineKey : routinesSection.getKeys(false)) {
            ConfigurationSection section = routinesSection.getConfigurationSection(routineKey);
            if (section == null) continue;

            Routine routine = parseRoutine(routineKey, section);
            if (routine != null) {
                manager.registerRoutine(routine);
                count++;
            }
        }

        log.info("[RoutineLoader] Loaded " + count + " routines from " + ROUTINES_FILE);
    }

    // ─── Routine Parsing ──────────────────────────────────────────────────────

    private Routine parseRoutine(String key, ConfigurationSection section) {
        String description  = section.getString("description", "No description.");
        List<String> npcs   = section.getStringList("required_npcs");
        int duration        = section.getInt("duration_ticks", 1200);
        int cooldown        = section.getInt("cooldown_ticks", 6000);
        int priority        = section.getInt("priority", 5);
        int weight          = section.getInt("weight", 10);

        List<?> rawSteps = section.getList("steps");
        if (rawSteps == null || rawSteps.isEmpty()) {
            log.warning("[RoutineLoader] Routine '" + key + "' has no steps — skipping.");
            return null;
        }

        List<RoutineStep> steps = new ArrayList<>();
        int stepIdx = 0;
        for (Object rawStep : rawSteps) {
            if (!(rawStep instanceof java.util.Map<?, ?> stepMap)) {
                log.warning("[RoutineLoader] Step " + stepIdx + " in '" + key + "' is not a map — skipping.");
                stepIdx++;
                continue;
            }

            RoutineStep step = buildStep(key, stepIdx, stepMap);
            if (step != null) steps.add(step);
            stepIdx++;
        }

        if (steps.isEmpty()) {
            log.warning("[RoutineLoader] Routine '" + key + "' produced 0 valid steps — skipping.");
            return null;
        }

        return new Routine(key, description, npcs, duration, cooldown, priority, weight, steps);
    }

    // ─── Step Factory ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private RoutineStep buildStep(String routineKey, int stepIdx, java.util.Map<?, ?> map) {
        String type = getString(map, "type");
        if (type == null) {
            log.warning("[RoutineLoader] Step " + stepIdx + " in '" + routineKey + "' has no type — skipping.");
            return null;
        }

        try {
            return switch (type.toUpperCase()) {

                case "MOVE_TO" -> {
                    String locationKey = requireString(map, "location", routineKey, stepIdx);
                    validateLocationKey(locationKey, routineKey, stepIdx, "location");
                    yield new MoveToStep(
                        getString(map, "npc"),
                        locationKey,
                        parseEnum(MoveToStep.Speed.class, getString(map, "speed"), MoveToStep.Speed.WALK)
                    );
                }

                case "SIT" -> new SitStep(
                    getString(map, "npc"),
                    getInt(map, "duration_ticks", 0)
                );

                case "STAND" -> new StandStep(getString(map, "npc"));

                case "SAY" -> new SayStep(
                    getString(map, "npc"),
                    requireString(map, "message", routineKey, stepIdx),
                    getInt(map, "pause_ticks", 60)
                );

                case "GROUP_SAY" -> {
                    List<GroupSayStep.DialogueLine> lines = new ArrayList<>();
                    List<?> rawLines = (List<?>) map.get("lines");
                    if (rawLines != null) {
                        for (Object rawLine : rawLines) {
                            if (rawLine instanceof java.util.Map<?, ?> lineMap) {
                                String speaker = getString(lineMap, "speaker");
                                String message = getString(lineMap, "message");
                                if (speaker != null && message != null) {
                                    lines.add(new GroupSayStep.DialogueLine(speaker, message));
                                }
                            }
                        }
                    }
                    yield new GroupSayStep(lines, getInt(map, "pause_per_line_ticks", 80));
                }

                case "WAIT" -> new WaitStep(getInt(map, "ticks", 20));

                case "ANIMATE" -> new AnimateStep(
                    getString(map, "npc"),
                    parseEnum(AnimateStep.AnimationType.class, getString(map, "animation"), AnimateStep.AnimationType.ARM_SWING)
                );

                case "TURN_FACE" -> {
                    String faceTarget = requireString(map, "face_target", routineKey, stepIdx);
                    // face_target can be an NPC name (not in registry) or a location key — only warn
                    // if it looks like a location key (no spaces, underscored) and isn't registered
                    validateLocationKeyOptional(faceTarget, routineKey, stepIdx, "face_target");
                    yield new TurnFaceStep(
                        getString(map, "npc"),
                        faceTarget
                    );
                }

                case "EMOTE" -> new EmoteStep(
                    getString(map, "npc"),
                    parseEnum(EmoteStep.EmoteType.class, getString(map, "emote"), EmoteStep.EmoteType.THINKING)
                );

                case "BROADCAST" -> new BroadcastStep(
                    requireString(map, "message", routineKey, stepIdx),
                    getBoolean(map, "chat_only", true)
                );

                case "CONDITIONAL" -> new ConditionalStep(
                    requireString(map, "condition", routineKey, stepIdx),
                    getInt(map, "skip_steps", 1)
                );

                case "CHAIN_ROUTINE" -> new ChainRoutineStep(
                    requireString(map, "routine_name", routineKey, stepIdx),
                    getInt(map, "delay_ticks", 0)
                );

                default -> {
                    log.warning("[RoutineLoader] Unknown step type '" + type + "' at step "
                        + stepIdx + " in routine '" + routineKey + "' — skipping.");
                    yield null;
                }
            };
        } catch (RequiredFieldException e) {
            log.warning("[RoutineLoader] " + e.getMessage() + " — skipping step.");
            return null;
        } catch (Exception e) {
            log.warning("[RoutineLoader] Error parsing step " + stepIdx + " in '"
                + routineKey + "': " + e.getMessage());
            return null;
        }
    }

    // ─── Map Helpers ──────────────────────────────────────────────────────────

    private String getString(java.util.Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String requireString(java.util.Map<?, ?> map, String key, String routineKey, int stepIdx) {
        String val = getString(map, key);
        if (val == null) throw new RequiredFieldException(
            "Required field '" + key + "' missing in step " + stepIdx + " of routine '" + routineKey + "'");
        return val;
    }

    private int getInt(java.util.Map<?, ?> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private boolean getBoolean(java.util.Map<?, ?> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultValue;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E fallback) {
        if (value == null) return fallback;
        try { return Enum.valueOf(clazz, value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            log.warning("[RoutineLoader] Unknown enum value '" + value + "' for " + clazz.getSimpleName()
                + " — using default: " + fallback);
            return fallback;
        }
    }

    // ─── Location Validation Helpers ──────────────────────────────────────────

    /**
     * Warn if the given key is not a coordinate string and is absent from the registry.
     * Used for MOVE_TO — these must always be resolvable to a Location.
     */
    private void validateLocationKey(String key, String routineKey, int stepIdx, String field) {
        if (locationRegistry == null) return;         // no registry yet — skip
        if (key == null) return;                      // missing field caught elsewhere
        if (isCoordinateString(key)) return;          // "x,y,z" literals always valid
        if (!locationRegistry.has(key)) {
            log.warning("[RoutineLoader] WARNING: " + field + " '" + key
                + "' in step " + stepIdx + " of routine '" + routineKey
                + "' is not registered in LocationRegistry. "
                + "Step will be skipped at runtime if not resolved.");
        }
    }

    /**
     * Softer validation for TURN_FACE face_target — may also be an NPC name,
     * so we only warn if it looks like a location key (no spaces/colour codes) and
     * isn't in the registry.
     */
    private void validateLocationKeyOptional(String key, String routineKey, int stepIdx, String field) {
        if (locationRegistry == null || key == null) return;
        if (isCoordinateString(key)) return;
        // Skip if it looks like an NPC display name (contains spaces or colour codes)
        if (key.contains(" ") || key.contains("§")) return;
        if (!locationRegistry.has(key)) {
            log.fine("[RoutineLoader] Note: " + field + " '" + key
                + "' in step " + stepIdx + " of routine '" + routineKey
                + "' not found in LocationRegistry — may be an NPC name (will be resolved at runtime).");
        }
    }

    /** Returns true if the key looks like a coordinate string ("x,y,z"). */
    private static boolean isCoordinateString(String key) {
        return key != null && key.matches("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?");
    }

    // ─── Internal Exception ───────────────────────────────────────────────────

    private static class RequiredFieldException extends RuntimeException {
        public RequiredFieldException(String message) { super(message); }
    }
}
