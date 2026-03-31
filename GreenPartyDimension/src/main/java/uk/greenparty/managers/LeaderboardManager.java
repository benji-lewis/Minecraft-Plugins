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
import java.util.stream.*;

/**
 * LeaderboardManager — Phase 4 competitive ranking system.
 *
 * Top 10 players tracked across 5 categories. Updated in real-time.
 * Monthly resets with rewards for the top 3 environmentally virtuous (or violating) players.
 *
 * "The leaderboard was passed by Motion 112a: Competitive Sustainability Rankings.
 *  It passed 6-0. The losing councillor was Councillor Wheatgrass (abstained, conflict of interest)."
 */
public class LeaderboardManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File dataFile;
    private File archiveFile;

    public enum Category {
        GREEN_CREDITS("green_credits", "§aGreen Credits", "§7Most earned"),
        ECO_SCORE("eco_score", "§2Eco-Score", "§7Highest rating"),
        TREES_PLANTED("trees_planted", "§2Trees Planted", "§7Sapling count"),
        QUESTS_COMPLETED("quests_completed", "§eQuests Completed", "§7Chain completion count"),
        VIOLATIONS("violations", "§cViolations", "§7Most violations (chaos board)");

        public final String id;
        public final String displayName;
        public final String description;

        Category(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public static Category fromId(String id) {
            for (Category c : values()) {
                if (c.id.equalsIgnoreCase(id)) return c;
            }
            return null;
        }
    }

    public static class LeaderboardEntry {
        public String uuid;
        public String name;
        public long value;

        public LeaderboardEntry() {}

        public LeaderboardEntry(String uuid, String name, long value) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
    }

    // In-memory board: category -> sorted list (highest first, except violations also highest first)
    private final Map<String, List<LeaderboardEntry>> boards = new HashMap<>();
    private String currentMonth = "";
    private boolean monthlyResetEnabled = true;

    private static final int TOP_N = 10;

    public LeaderboardManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        scheduleMonthlyCheck();
        scheduleRealTimeUpdate();
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    private void loadConfig() {
        monthlyResetEnabled = plugin.getConfig().getBoolean("leaderboard.monthly-reset.enabled", true);
    }

    // ─── Init / Schedule ──────────────────────────────────────────────────────

    private void scheduleMonthlyCheck() {
        if (!monthlyResetEnabled) return;
        // Check every 5 minutes (game time) for month rollover
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String month = YearMonth.now().toString();
            if (!month.equals(currentMonth) && !currentMonth.isEmpty()) {
                doMonthlyReset();
            }
            currentMonth = month;
        }, 200L, 6000L);
    }

    private void scheduleRealTimeUpdate() {
        // Sync live data from managers every 2 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::syncFromManagers, 400L, 2400L);
    }

    // ─── Sync from Managers ───────────────────────────────────────────────────

    public void syncFromManagers() {
        try {
            GreenCreditManager gcm = plugin.getGreenCreditManager();
            EcoScoreManager esm = plugin.getEcoScoreManager();
            ViolationManager vm = plugin.getViolationManager();

            for (Player p : Bukkit.getOnlinePlayers()) {
                updateEntry(Category.GREEN_CREDITS, p.getUniqueId().toString(), p.getName(),
                    gcm.getBalance(p));
                updateEntry(Category.ECO_SCORE, p.getUniqueId().toString(), p.getName(),
                    esm.getEcoScore(p));
                updateEntry(Category.VIOLATIONS, p.getUniqueId().toString(), p.getName(),
                    vm.getViolations(p));
            }
            saveData();
        } catch (Exception e) {
            plugin.getLogger().warning("[Leaderboard] Sync error: " + e.getMessage());
        }
    }

    public void onQuestCompleted(Player player) {
        List<LeaderboardEntry> board = getBoard(Category.QUESTS_COMPLETED);
        LeaderboardEntry entry = findOrCreate(board, player.getUniqueId().toString(), player.getName());
        entry.value++;
        resort(board);
        saveData();
    }

    public void onTreePlanted(Player player) {
        List<LeaderboardEntry> board = getBoard(Category.TREES_PLANTED);
        LeaderboardEntry entry = findOrCreate(board, player.getUniqueId().toString(), player.getName());
        entry.value++;
        resort(board);
        saveData();
    }

    public void updateEntry(Category category, String uuid, String name, long value) {
        List<LeaderboardEntry> board = getBoard(category);
        LeaderboardEntry entry = findOrCreate(board, uuid, name);
        entry.value = value;
        entry.name = name; // update display name if changed
        resort(board);
    }

    private List<LeaderboardEntry> getBoard(Category category) {
        return boards.computeIfAbsent(category.id, k -> new ArrayList<>());
    }

    private LeaderboardEntry findOrCreate(List<LeaderboardEntry> board, String uuid, String name) {
        for (LeaderboardEntry e : board) {
            if (e.uuid.equals(uuid)) return e;
        }
        LeaderboardEntry entry = new LeaderboardEntry(uuid, name, 0);
        board.add(entry);
        return entry;
    }

    private void resort(List<LeaderboardEntry> board) {
        board.sort((a, b) -> Long.compare(b.value, a.value)); // highest first
        // Trim to top N
        while (board.size() > TOP_N) board.remove(board.size() - 1);
    }

    // ─── Display ──────────────────────────────────────────────────────────────

    public void displayLeaderboard(Player player, Category category) {
        List<LeaderboardEntry> board = getBoard(category);

        String title = category.displayName + " §2§lLeaderboard";
        player.sendMessage("§2§l╔══════════════════════════════╗");
        player.sendMessage("§2§l║  " + title);
        player.sendMessage("§2§l║  " + category.description);
        player.sendMessage("§2§l╠══════════════════════════════╣");

        if (board.isEmpty()) {
            player.sendMessage("§2§l║  §7No data yet. Be the first!");
        } else {
            String[] medals = {"§6①", "§7②", "§8③", "§8④", "§8⑤", "§8⑥", "§8⑦", "§8⑧", "§8⑨", "§8⑩"};
            for (int i = 0; i < Math.min(board.size(), TOP_N); i++) {
                LeaderboardEntry e = board.get(i);
                String medal = i < medals.length ? medals[i] : "§8" + (i + 1) + ".";
                String valueStr = formatValue(category, e.value);
                String row = "§2§l║  " + medal + " §r§f" + e.name + " §8— " + valueStr;
                player.sendMessage(row);
            }
        }

        player.sendMessage("§2§l╠══════════════════════════════╣");
        player.sendMessage("§2§l║  §7Month: §a" + currentMonth + (monthlyResetEnabled ? " §8(resets monthly)" : ""));
        player.sendMessage("§2§l╚══════════════════════════════╝");
    }

    private String formatValue(Category category, long value) {
        return switch (category) {
            case GREEN_CREDITS -> "§a" + value + " §2GC";
            case ECO_SCORE -> (value >= 0 ? "§a" : "§c") + value + " §7pts";
            case TREES_PLANTED -> "§2" + value + " §7trees";
            case QUESTS_COMPLETED -> "§e" + value + " §7quests";
            case VIOLATIONS -> "§c" + value + " §7violations";
        };
    }

    // ─── Monthly Reset & Rewards ──────────────────────────────────────────────

    private void doMonthlyReset() {
        plugin.getLogger().info("[Leaderboard] Month rolled over — processing rewards and resetting boards...");

        // Archive current boards
        archiveCurrentMonth();

        // Give rewards to top 3 in GREEN_CREDITS as main board
        List<LeaderboardEntry> gcBoard = getBoard(Category.GREEN_CREDITS);
        if (gcBoard.size() >= 1) giveMonthlyReward(gcBoard.get(0), 1);
        if (gcBoard.size() >= 2) giveMonthlyReward(gcBoard.get(1), 2);
        if (gcBoard.size() >= 3) giveMonthlyReward(gcBoard.get(2), 3);

        // Broadcast
        Bukkit.broadcastMessage("§2§l[Green Party] §aMonthly leaderboard reset! Top 3 have received their rewards.");
        Bukkit.broadcastMessage("§7§o\"The council has processed the monthly sustainability report. It was 94 pages.\"");

        // Clear boards
        for (Category cat : Category.values()) {
            getBoard(cat).clear();
        }
        saveData();
    }

    private void giveMonthlyReward(LeaderboardEntry entry, int rank) {
        int bonus;
        String title;
        switch (rank) {
            case 1 -> { bonus = plugin.getConfig().getInt("leaderboard.monthly-reset.rewards.first.credits", 100);
                title = "Green Champion"; }
            case 2 -> { bonus = plugin.getConfig().getInt("leaderboard.monthly-reset.rewards.second.credits", 50);
                title = "Eco Advocate"; }
            default -> { bonus = plugin.getConfig().getInt("leaderboard.monthly-reset.rewards.third.credits", 25);
                title = "Green Supporter"; }
        }

        // Give credits if player is online
        Player p = Bukkit.getPlayer(java.util.UUID.fromString(entry.uuid));
        if (p != null && p.isOnline()) {
            plugin.getGreenCreditManager().addCredits(p, bonus, "monthly leaderboard rank #" + rank);
            // Grant cosmetic title
            plugin.getCosmeticsManager().grantChatTitle(p, title);
            p.sendMessage("§2§l[Monthly Reward] §aYou placed #" + rank + " on the Green Credits leaderboard!");
            p.sendMessage("§a+" + bonus + " GC §7+ §2[" + title + "] §7title unlocked!");
        } else {
            // Offline: queue credits
            plugin.getGreenCreditManager().giveCreditsById(
                java.util.UUID.fromString(entry.uuid), bonus, "monthly-reward");
        }

        plugin.getLogger().info("[Leaderboard] Rank #" + rank + " reward given to " + entry.name
            + ": +" + bonus + " GC + " + title);
    }

    private void archiveCurrentMonth() {
        if (currentMonth.isEmpty()) return;
        try {
            archiveFile = new File(plugin.getDataFolder(), "leaderboard-archive-" + currentMonth + ".json");
            try (Writer w = new FileWriter(archiveFile)) {
                gson.toJson(boards, w);
            }
            plugin.getLogger().info("[Leaderboard] Archived month " + currentMonth);
        } catch (IOException e) {
            plugin.getLogger().warning("[Leaderboard] Could not archive: " + e.getMessage());
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "leaderboard.json");
        currentMonth = YearMonth.now().toString();

        if (!dataFile.exists()) return;

        try (Reader r = new FileReader(dataFile)) {
            JsonObject obj = gson.fromJson(r, JsonObject.class);
            if (obj == null) return;

            if (obj.has("month")) {
                currentMonth = obj.get("month").getAsString();
            }

            if (obj.has("boards")) {
                JsonObject boardsObj = obj.getAsJsonObject("boards");
                Type listType = new TypeToken<List<LeaderboardEntry>>() {}.getType();
                for (Category cat : Category.values()) {
                    if (boardsObj.has(cat.id)) {
                        List<LeaderboardEntry> entries = gson.fromJson(boardsObj.get(cat.id), listType);
                        if (entries != null) boards.put(cat.id, entries);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Leaderboard] Could not load data: " + e.getMessage());
        }

        plugin.getLogger().info("[Leaderboard] Loaded leaderboards for " + boards.size() + " categories.");
    }

    public void saveData() {
        try (Writer w = new FileWriter(dataFile)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("month", currentMonth);
            JsonObject boardsObj = new JsonObject();
            for (Map.Entry<String, List<LeaderboardEntry>> entry : boards.entrySet()) {
                boardsObj.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
            }
            obj.add("boards", boardsObj);
            gson.toJson(obj, w);
        } catch (IOException e) {
            plugin.getLogger().warning("[Leaderboard] Could not save: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public String getCurrentMonth() { return currentMonth; }

    public List<LeaderboardEntry> getTopEntries(Category category, int limit) {
        List<LeaderboardEntry> board = getBoard(category);
        return board.subList(0, Math.min(limit, board.size()));
    }

    public int getPlayerRank(Player player, Category category) {
        List<LeaderboardEntry> board = getBoard(category);
        for (int i = 0; i < board.size(); i++) {
            if (board.get(i).uuid.equals(player.getUniqueId().toString())) return i + 1;
        }
        return -1;
    }
}
