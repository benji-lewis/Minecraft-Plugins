package uk.greenparty.managers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ProgressionManager — Phase 4 monthly cycles, seasonal bonuses, and long-term progression.
 *
 * Tracks monthly standings, distributes rewards, manages seasonal bonuses.
 * The council approved this in Motion 201a: Competitive Sustainability Rankings,
 * Seasonal Editions. It passed after 40 minutes of debate about what qualifies as "Earth Day".
 */
public class ProgressionManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File dataFile;

    private String currentMonth = "";
    private boolean seasonalBonusesEnabled;

    // Track composter placements per player (for cosmetics unlock)
    private final Map<UUID, Integer> composterCounts = new HashMap<>();

    // Seasonal bonus periods: key = "MM-dd", value = multiplier
    private static final Map<String, Double> SEASONAL_EVENTS = new LinkedHashMap<>();

    static {
        SEASONAL_EVENTS.put("04-22", 2.0); // Earth Day — 2x GC
        SEASONAL_EVENTS.put("04-23", 2.0); // Earth Day +1
        SEASONAL_EVENTS.put("06-05", 1.5); // World Environment Day — 1.5x
        SEASONAL_EVENTS.put("12-25", 1.5); // Green Christmas — 1.5x
        SEASONAL_EVENTS.put("01-01", 1.5); // New Year - fresh start bonus
        SEASONAL_EVENTS.put("03-20", 1.5); // Spring Equinox
    }

    public ProgressionManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        scheduleChecks();
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    private void loadConfig() {
        seasonalBonusesEnabled = plugin.getConfig().getBoolean("progression.seasonal-bonuses.enabled", true);
        currentMonth = YearMonth.now().toString();
    }

    // ─── Scheduling ───────────────────────────────────────────────────────────

    private void scheduleChecks() {
        // Check every 5 minutes for seasonal bonuses announcement
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkSeasonalBonus, 400L, 6000L);
    }

    // ─── Seasonal Bonus ───────────────────────────────────────────────────────

    private String lastSeasonalAnnounced = "";

    private void checkSeasonalBonus() {
        if (!seasonalBonusesEnabled) return;

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        Double multiplier = SEASONAL_EVENTS.get(today);
        if (multiplier != null && !today.equals(lastSeasonalAnnounced)) {
            lastSeasonalAnnounced = today;
            String eventName = getEventName(today);
            Bukkit.broadcastMessage("§2§l[Seasonal Bonus] §a" + eventName + "!");
            Bukkit.broadcastMessage("§a§l" + (int)(multiplier * 100) + "% Green Credit bonus active today!");
            Bukkit.broadcastMessage("§7§o\"The council has declared this an official day of environmental celebration.\"");
        }
    }

    private String getEventName(String mmdd) {
        return switch (mmdd) {
            case "04-22" -> "🌍 Earth Day — 2x Green Credit bonus";
            case "04-23" -> "🌍 Earth Day (Day 2)";
            case "06-05" -> "🌿 World Environment Day — 1.5x bonus";
            case "12-25" -> "🎄 Green Christmas — sustainable celebrations";
            case "01-01" -> "🎊 Green New Year — fresh ecological start";
            case "03-20" -> "🌱 Spring Equinox — renewal bonus";
            default -> "Special Event";
        };
    }

    public double getSeasonalMultiplier() {
        if (!seasonalBonusesEnabled) return 1.0;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        return SEASONAL_EVENTS.getOrDefault(today, 1.0);
    }

    public boolean isSeasonalBonus() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        return seasonalBonusesEnabled && SEASONAL_EVENTS.containsKey(today);
    }

    public String getSeasonalBonusDescription() {
        if (!isSeasonalBonus()) return "None";
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        double mult = SEASONAL_EVENTS.getOrDefault(today, 1.0);
        return getEventName(today) + " (x" + mult + " GC)";
    }

    // ─── Progress Display ─────────────────────────────────────────────────────

    public void showMonthlyProgress(Player player) {
        LeaderboardManager lm = plugin.getLeaderboardManager();

        player.sendMessage("§2§l╔══════════════════════════════╗");
        player.sendMessage("§2§l║   Monthly Standings — " + lm.getCurrentMonth());
        player.sendMessage("§2§l╠══════════════════════════════╣");

        // Show top 3 from Green Credits board
        var top3 = lm.getTopEntries(LeaderboardManager.Category.GREEN_CREDITS, 3);
        String[] rankLabels = {"§61st", "§72nd", "§83rd"};
        String[] rewards = {"100 GC + §2[Green Champion]", "50 GC + §a[Eco Advocate]", "25 GC + §2[Green Supporter]"};

        player.sendMessage("§2§l║ §7Green Credits Top 3:");
        for (int i = 0; i < top3.size(); i++) {
            var entry = top3.get(i);
            player.sendMessage("§2§l║  " + rankLabels[i] + " §f" + entry.name
                + " §8— §a" + entry.value + " GC §8→ " + rewards[i]);
        }
        if (top3.isEmpty()) {
            player.sendMessage("§2§l║  §8No data yet this month.");
        }

        // Player's own rank
        int myRank = lm.getPlayerRank(player, LeaderboardManager.Category.GREEN_CREDITS);
        player.sendMessage("§2§l║");
        if (myRank > 0) {
            player.sendMessage("§2§l║ §7Your rank: §a#" + myRank);
        } else {
            player.sendMessage("§2§l║ §7You are not yet on the board.");
        }

        // Seasonal bonus
        if (isSeasonalBonus()) {
            player.sendMessage("§2§l║ §6🎉 " + getSeasonalBonusDescription());
        }

        player.sendMessage("§2§l╠══════════════════════════════╣");
        player.sendMessage("§2§l║ §7Resets on 1st of each month.");
        player.sendMessage("§2§l║ §7Top 3 earn Green Credits + Titles.");
        player.sendMessage("§2§l╚══════════════════════════════╝");
    }

    // ─── Composter Tracking ───────────────────────────────────────────────────

    public void onComposterPlaced(Player player) {
        UUID uuid = player.getUniqueId();
        int count = composterCounts.merge(uuid, 1, Integer::sum);
        plugin.getCosmeticsManager().onComposterPlaced(player, count);
        saveData();
    }

    public int getComposterCount(Player player) {
        return composterCounts.getOrDefault(player.getUniqueId(), 0);
    }

    // ─── Player All-Stats ─────────────────────────────────────────────────────

    public void showFullStats(Player player, Player target) {
        GreenCreditManager gcm = plugin.getGreenCreditManager();
        EcoScoreManager esm = plugin.getEcoScoreManager();
        ViolationManager vm = plugin.getViolationManager();
        LeaderboardManager lm = plugin.getLeaderboardManager();
        CosmeticsManager cm = plugin.getCosmeticsManager();
        QuestChainManager qcm = plugin.getQuestChainManager();

        player.sendMessage("§2§l╔════════════════════════════════╗");
        player.sendMessage("§2§l║  §f" + target.getName() + "§2§l — Green Party Stats");
        player.sendMessage("§2§l╠════════════════════════════════╣");
        player.sendMessage("§2§l║ §7Green Credits: §a" + gcm.getBalance(target) + " GC");
        player.sendMessage("§2§l║ §7Eco-Score:     §a" + esm.getEcoScore(target));
        player.sendMessage("§2§l║ §7Violations:    §c" + vm.getViolations(target));
        player.sendMessage("§2§l║ §7Composters:    §a" + getComposterCount(target) + " placed");

        // Quest chains
        int chainsCompleted = qcm.getCompletedChainsCount(target);
        player.sendMessage("§2§l║ §7Quest Chains:  §e" + chainsCompleted + " completed");

        // Leaderboard ranks
        player.sendMessage("§2§l╠════════════════════════════════╣");
        player.sendMessage("§2§l║ §8Leaderboard Ranks (this month):");
        for (LeaderboardManager.Category cat : LeaderboardManager.Category.values()) {
            int rank = lm.getPlayerRank(target, cat);
            String rankStr = rank > 0 ? "§a#" + rank : "§8Not ranked";
            player.sendMessage("§2§l║  §7" + cat.displayName + ": " + rankStr);
        }

        // Cosmetics summary
        var cosData = cm.getPlayerData(target);
        player.sendMessage("§2§l╠════════════════════════════════╣");
        player.sendMessage("§2§l║ §8Cosmetics:");
        player.sendMessage("§2§l║  §7Unlocked: §a" + cosData.unlocked.size() + " cosmetics");
        if (!cosData.equippedTitle.isEmpty()) {
            player.sendMessage("§2§l║  §7Title: " + cosData.equippedTitle);
        }
        if (!cosData.equippedTrail.isEmpty()) {
            player.sendMessage("§2§l║  §7Trail: §a" + cosData.equippedTrail);
        }

        // Seasonal bonus
        if (isSeasonalBonus()) {
            player.sendMessage("§2§l╠════════════════════════════════╣");
            player.sendMessage("§2§l║ §6🎉 Seasonal: " + getSeasonalBonusDescription());
        }

        player.sendMessage("§2§l╚════════════════════════════════╝");
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "progression.json");
        if (!dataFile.exists()) return;

        try (Reader r = new FileReader(dataFile)) {
            JsonObject obj = gson.fromJson(r, JsonObject.class);
            if (obj == null) return;

            if (obj.has("composterCounts")) {
                JsonObject cc = obj.getAsJsonObject("composterCounts");
                for (Map.Entry<String, JsonElement> e : cc.entrySet()) {
                    try {
                        composterCounts.put(UUID.fromString(e.getKey()), e.getValue().getAsInt());
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Progression] Could not load data: " + e.getMessage());
        }
    }

    public void saveData() {
        try (Writer w = new FileWriter(dataFile)) {
            JsonObject obj = new JsonObject();

            JsonObject cc = new JsonObject();
            for (Map.Entry<UUID, Integer> e : composterCounts.entrySet()) {
                cc.addProperty(e.getKey().toString(), e.getValue());
            }
            obj.add("composterCounts", cc);

            gson.toJson(obj, w);
        } catch (IOException e) {
            plugin.getLogger().warning("[Progression] Could not save data: " + e.getMessage());
        }
    }
}
