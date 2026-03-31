package uk.greenparty.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * GreenCreditManager — the official currency of the Verdant Utopia.
 *
 * Green Credits (GC) cannot be spent on fossil fuels.
 * They cannot, in fact, be spent on anything yet.
 * But they accumulate, and that is what matters.
 * The council is working on a shop. It has been in committee since 2023.
 */
public class GreenCreditManager {

    private final GreenPartyPlugin plugin;
    private final Map<UUID, Integer> creditBalances = new HashMap<>();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File dataFile;

    // Credit amounts for various actions (from config)
    private int creditsCouncilTask;
    private int creditsQuest;
    private int creditsVoting;

    public GreenCreditManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        creditsCouncilTask = plugin.getConfig().getInt("green-credits.rewards.council-task", 10);
        creditsQuest       = plugin.getConfig().getInt("green-credits.rewards.quest",        5);
        creditsVoting      = plugin.getConfig().getInt("green-credits.rewards.voting",        2);
    }

    // ─── Persistence (JSON) ───────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata/green-credits.json");
        dataFile.getParentFile().mkdirs();

        if (!dataFile.exists()) {
            plugin.getLogger().info("[GreenCredits] No data file found — starting fresh. All players begin with 0 GC.");
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> raw = gson.fromJson(reader, type);
            if (raw != null) {
                for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                    try {
                        creditBalances.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[GreenCredits] Failed to load data: " + e.getMessage());
        }

        plugin.getLogger().info("[GreenCredits] Loaded balances for " + creditBalances.size() + " players.");
    }

    public void saveAll() {
        Map<String, Integer> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : creditBalances.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(raw, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("[GreenCredits] Failed to save data: " + e.getMessage());
        }
    }

    public void savePlayer(Player player) {
        // For efficiency we save all on server stop; individual saves only on quit
        saveAll();
    }

    public void loadPlayer(Player player) {
        // Ensure player has an entry
        creditBalances.computeIfAbsent(player.getUniqueId(), k -> 0);
    }

    // ─── Balance API ──────────────────────────────────────────────────────────

    public int getBalance(Player player) {
        return creditBalances.getOrDefault(player.getUniqueId(), 0);
    }

    public void addCredits(Player player, int amount, String reason) {
        if (amount <= 0) return;
        UUID uuid = player.getUniqueId();
        int newBalance = creditBalances.getOrDefault(uuid, 0) + amount;
        creditBalances.put(uuid, newBalance);

        player.sendMessage("§2[Green Credits] §a+" + amount + " GC §7(" + reason + ") §8→ Balance: §a" + newBalance + " GC");

        // Check achievement
        plugin.getAchievementManager().checkGreenCredits(player, newBalance);
    }

    public boolean deductCredits(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = creditBalances.getOrDefault(uuid, 0);
        if (current < amount) return false;
        creditBalances.put(uuid, current - amount);
        return true;
    }

    /**
     * Admin give credits. Returns false if player not found.
     */
    public void giveCreditsById(UUID uuid, int amount, String byName) {
        int newBalance = creditBalances.getOrDefault(uuid, 0) + amount;
        creditBalances.put(uuid, newBalance);
        plugin.getLogger().info("[GreenCredits] " + byName + " gave " + amount + " GC to " + uuid + ". New balance: " + newBalance);
    }

    // ─── Reward Shortcuts ─────────────────────────────────────────────────────

    public void rewardQuestCompletion(Player player) {
        addCredits(player, creditsQuest, "quest completed");
    }

    public void rewardCouncilTask(Player player) {
        addCredits(player, creditsCouncilTask, "council task");
    }

    public void rewardVoting(Player player) {
        addCredits(player, creditsVoting, "voted");
    }

    // ─── Formatting ──────────────────────────────────────────────────────────

    public String formatBalance(Player player) {
        int bal = getBalance(player);
        return "§a" + bal + " §2Green Credits§7";
    }
}
