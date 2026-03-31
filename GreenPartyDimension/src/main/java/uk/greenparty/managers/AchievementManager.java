package uk.greenparty.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * AchievementManager — the official record of things players have done in the Verdant Utopia.
 *
 * The council takes achievements very seriously.
 * They have a spreadsheet.
 * The spreadsheet has 14 tabs.
 * Tab 9 is locked and no one knows why.
 */
public class AchievementManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Achievement definitions (id -> definition)
    private final Map<String, Achievement> definitions = new LinkedHashMap<>();

    // Player data: UUID -> set of earned achievement IDs
    private final Map<UUID, Set<String>> earned = new HashMap<>();

    // Progress counters: UUID -> (achievement counter type -> count)
    private final Map<UUID, Map<String, Integer>> counters = new HashMap<>();

    private File earnedFile;
    private File countersFile;

    // Thresholds (from config)
    private int threshKidnap;
    private int threshTrees;
    private int threshCredits;
    private int threshCouncil;

    public AchievementManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        registerAchievements();
        loadData();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        threshKidnap  = plugin.getConfig().getInt("achievements.thresholds.kidnapped",         5);
        threshTrees   = plugin.getConfig().getInt("achievements.thresholds.trees-planted",    100);
        threshCredits = plugin.getConfig().getInt("achievements.thresholds.green-credits",   1000);
        threshCouncil = plugin.getConfig().getInt("achievements.thresholds.council-meetings", 10);
    }

    // ─── Achievement Definitions ──────────────────────────────────────────────

    private void registerAchievements() {
        register(new Achievement(
            "kidnapped_5",
            "Involuntary Membership",
            "Been kidnapped by the Green Council " + threshKidnap + " times.",
            "§c§l✦ Involuntary Member §r§8— kidnapped " + threshKidnap + "x",
            "KIDNAP_COUNT",
            threshKidnap
        ));

        register(new Achievement(
            "planted_100",
            "Arboreal Champion",
            "Planted " + threshTrees + " trees/saplings. The council has named a forest after you.",
            "§2§l🌳 Arboreal Champion §r§8— planted " + threshTrees + " trees",
            "TREES_PLANTED",
            threshTrees
        ));

        register(new Achievement(
            "credits_1000",
            "Green Tycoon",
            "Earned " + threshCredits + " Green Credits. The council is suspicious of your wealth.",
            "§6§l💰 Green Tycoon §r§8— " + threshCredits + " GC earned",
            "GREEN_CREDITS",
            threshCredits
        ));

        register(new Achievement(
            "council_10",
            "Dedicated Attendee",
            "Attended " + threshCouncil + " council meetings. There are always more.",
            "§b§l📜 Dedicated Attendee §r§8— attended " + threshCouncil + " meetings",
            "COUNCIL_MEETINGS",
            threshCouncil
        ));
    }

    private void register(Achievement achievement) {
        definitions.put(achievement.id, achievement);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private void loadData() {
        File dir = new File(plugin.getDataFolder(), "playerdata");
        dir.mkdirs();

        earnedFile   = new File(dir, "achievements-earned.json");
        countersFile = new File(dir, "achievements-counters.json");

        // Load earned
        if (earnedFile.exists()) {
            try (Reader r = new FileReader(earnedFile)) {
                Type t = new TypeToken<Map<String, Set<String>>>() {}.getType();
                Map<String, Set<String>> raw = gson.fromJson(r, t);
                if (raw != null) {
                    for (Map.Entry<String, Set<String>> e : raw.entrySet()) {
                        try {
                            earned.put(UUID.fromString(e.getKey()), new HashSet<>(e.getValue()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[Achievements] Failed to load earned data: " + e.getMessage());
            }
        }

        // Load counters
        if (countersFile.exists()) {
            try (Reader r = new FileReader(countersFile)) {
                Type t = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
                Map<String, Map<String, Integer>> raw = gson.fromJson(r, t);
                if (raw != null) {
                    for (Map.Entry<String, Map<String, Integer>> e : raw.entrySet()) {
                        try {
                            counters.put(UUID.fromString(e.getKey()), new HashMap<>(e.getValue()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[Achievements] Failed to load counter data: " + e.getMessage());
            }
        }

        plugin.getLogger().info("[Achievements] Loaded data for " + earned.size() + " players.");
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        earned.computeIfAbsent(uuid, k -> new HashSet<>());
        counters.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public void saveAll() {
        // Save earned
        Map<String, Set<String>> rawEarned = new LinkedHashMap<>();
        for (Map.Entry<UUID, Set<String>> e : earned.entrySet()) {
            rawEarned.put(e.getKey().toString(), e.getValue());
        }
        try (Writer w = new FileWriter(earnedFile)) {
            gson.toJson(rawEarned, w);
        } catch (IOException e) {
            plugin.getLogger().severe("[Achievements] Failed to save earned data: " + e.getMessage());
        }

        // Save counters
        Map<String, Map<String, Integer>> rawCounters = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> e : counters.entrySet()) {
            rawCounters.put(e.getKey().toString(), e.getValue());
        }
        try (Writer w = new FileWriter(countersFile)) {
            gson.toJson(rawCounters, w);
        } catch (IOException e) {
            plugin.getLogger().severe("[Achievements] Failed to save counter data: " + e.getMessage());
        }
    }

    // ─── Counter API ─────────────────────────────────────────────────────────

    public int getCounter(Player player, String type) {
        return counters.getOrDefault(player.getUniqueId(), Collections.emptyMap()).getOrDefault(type, 0);
    }

    public int incrementCounter(Player player, String type, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> playerCounters = counters.computeIfAbsent(uuid, k -> new HashMap<>());
        int newVal = playerCounters.getOrDefault(type, 0) + amount;
        playerCounters.put(type, newVal);
        return newVal;
    }

    // ─── Achievement Checks ───────────────────────────────────────────────────

    /**
     * Check kidnap count achievement.
     */
    public void recordKidnap(Player player) {
        int count = incrementCounter(player, "KIDNAP_COUNT", 1);
        checkAndAward(player, "kidnapped_5", "KIDNAP_COUNT", count);
    }

    /**
     * Increment trees planted counter.
     */
    public void incrementTreesPlanted(Player player) {
        int count = incrementCounter(player, "TREES_PLANTED", 1);
        checkAndAward(player, "planted_100", "TREES_PLANTED", count);
    }

    /**
     * Called when eco-score changes — just triggers the tree check.
     */
    public void checkTreesPlanted(Player player) {
        int count = getCounter(player, "TREES_PLANTED");
        checkAndAward(player, "planted_100", "TREES_PLANTED", count);
    }

    /**
     * Check green credit balance achievement.
     */
    public void checkGreenCredits(Player player, int currentBalance) {
        // We use cumulative balance stored separately to count "ever earned" rather than "current"
        int totalEarned = incrementCounter(player, "GREEN_CREDITS_TOTAL", 0); // peek
        if (currentBalance > totalEarned) {
            incrementCounter(player, "GREEN_CREDITS_TOTAL", currentBalance - totalEarned);
        }
        checkAndAward(player, "credits_1000", "GREEN_CREDITS", currentBalance);
    }

    /**
     * Record council meeting attendance.
     */
    public void recordCouncilMeeting(Player player) {
        int count = incrementCounter(player, "COUNCIL_MEETINGS", 1);
        checkAndAward(player, "council_10", "COUNCIL_MEETINGS", count);
    }

    // ─── Award Logic ──────────────────────────────────────────────────────────

    private void checkAndAward(Player player, String achievementId, String counterType, int currentValue) {
        Achievement ach = definitions.get(achievementId);
        if (ach == null) return;

        UUID uuid = player.getUniqueId();
        Set<String> playerEarned = earned.computeIfAbsent(uuid, k -> new HashSet<>());

        if (playerEarned.contains(achievementId)) return; // Already earned
        if (!ach.counterType.equals(counterType)) return;
        if (currentValue < ach.threshold) return;

        // Award!
        playerEarned.add(achievementId);
        awardAchievement(player, ach);
    }

    private void awardAchievement(Player player, Achievement ach) {
        // Title display
        player.sendTitle(
            "§2§l✦ ACHIEVEMENT UNLOCKED ✦",
            "§a" + ach.name,
            10, 70, 20
        );

        // Chat message
        player.sendMessage("");
        player.sendMessage("§2§l╔══════════════════════════════════╗");
        player.sendMessage("§2§l║   ✦ ACHIEVEMENT UNLOCKED ✦       ║");
        player.sendMessage("§2§l╚══════════════════════════════════╝");
        player.sendMessage("§a§l" + ach.name);
        player.sendMessage("§7" + ach.description);
        player.sendMessage("§8Badge: " + ach.badge);
        player.sendMessage("");

        // Sound fanfare
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        plugin.getLogger().info("[Achievements] " + player.getName() + " earned: " + ach.name);
    }

    public int getTreesPlanted(Player player) {
        return getCounter(player, "TREES_PLANTED");
    }

    // ─── List Command ─────────────────────────────────────────────────────────

    public void listAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> playerEarned = earned.getOrDefault(uuid, Collections.emptySet());

        player.sendMessage("§2§l===== YOUR ACHIEVEMENTS =====");

        for (Achievement ach : definitions.values()) {
            int current = getCounter(player, ach.counterType);
            boolean isEarned = playerEarned.contains(ach.id);

            if (isEarned) {
                player.sendMessage("§a✔ §l" + ach.name);
                player.sendMessage("   §8" + ach.badge);
            } else {
                player.sendMessage("§7✗ " + ach.name + " §8(" + current + "/" + ach.threshold + ")");
                player.sendMessage("   §8" + ach.description);
            }
        }

        player.sendMessage("§8Progress: " + playerEarned.size() + "/" + definitions.size() + " achievements");
        player.sendMessage("§2§l==============================");
    }

    // ─── Inner Class ──────────────────────────────────────────────────────────

    public static class Achievement {
        public final String id;
        public final String name;
        public final String description;
        public final String badge;         // Formatted badge string for display
        public final String counterType;   // Which counter to check
        public final int threshold;        // Value needed to earn

        public Achievement(String id, String name, String description, String badge,
                           String counterType, int threshold) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.badge = badge;
            this.counterType = counterType;
            this.threshold = threshold;
        }
    }
}
