package uk.greenparty.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * ViolationManager — because the Verdant Utopia has RULES.
 *
 * Environmental violations are tracked per player. Each violation incurs a fine
 * of 10 Green Credits (configurable). If a player can't pay? Exile. Immediately.
 * The council calls it "involuntary environmental rehabilitation."
 *
 * Violations trigger on: coal mining, leaf destruction, wood breaking, and other
 * crimes against the Green Code (as defined in By-law 7c, subsection 4, paragraph 2,
 * bullet point 3(ii) — see the attached 89-page document).
 */
public class ViolationManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // UUID -> violation count
    private final Map<UUID, Integer> violations = new HashMap<>();

    // Config-driven violation triggers: material name -> violation message
    private final Map<String, String> violationTriggers = new LinkedHashMap<>();

    private int fineAmount;
    private long weeklyResetTicks;
    private File dataFile;
    private BukkitTask resetTask;

    public ViolationManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        scheduleWeeklyReset();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        fineAmount        = plugin.getConfig().getInt("violations.fine-amount", 10);
        weeklyResetTicks  = plugin.getConfig().getLong("violations.reset-interval-ticks", 8_400_000L); // 7 days

        // Default violation triggers
        violationTriggers.put("COAL_ORE",         "Coal mining detected! The council is furious.");
        violationTriggers.put("DEEPSLATE_COAL_ORE","Deepslate coal mining! Even worse. A second violation.");
        violationTriggers.put("OAK_LOG",           "Tree felled without a permit! The arboreal committee weeps.");
        violationTriggers.put("BIRCH_LOG",         "Birch tree destroyed! Permit required. Always.");
        violationTriggers.put("SPRUCE_LOG",        "Spruce felled! The council is noting this down.");
        violationTriggers.put("JUNGLE_LOG",        "Jungle tree destroyed! This is a diplomatic incident.");
        violationTriggers.put("ACACIA_LOG",        "Acacia felled! The plains weep.");
        violationTriggers.put("DARK_OAK_LOG",      "Dark oak destroyed! A particularly shameful choice.");
        violationTriggers.put("MANGROVE_LOG",      "Mangrove destroyed! The wetlands are devastated.");
        violationTriggers.put("CHERRY_LOG",        "Cherry tree felled! The blossom committee is inconsolable.");
        violationTriggers.put("OAK_LEAVES",        "Leaf block destroyed! Have you no shame?");
        violationTriggers.put("BIRCH_LEAVES",      "Leaves? Destroyed? The council demands an apology.");
        violationTriggers.put("SPRUCE_LEAVES",     "Needles on the floor. Metaphorically. Violation issued.");
        violationTriggers.put("JUNGLE_LEAVES",     "Jungle canopy breached! Environmental crime.");
        violationTriggers.put("ACACIA_LEAVES",     "Acacia leaves! The savanna is watching.");
        violationTriggers.put("DARK_OAK_LEAVES",   "Dark oak foliage removed! Noted with extreme displeasure.");
        violationTriggers.put("MANGROVE_LEAVES",   "Mangrove leaves! Wetland crime.");
        violationTriggers.put("CHERRY_LEAVES",     "Cherry blossoms! Destroyed! The council is weeping.");
        violationTriggers.put("AZALEA_LEAVES",     "Flowering azalea leaves! That was beautiful. Was.");
        violationTriggers.put("FLOWERING_AZALEA_LEAVES", "Flowering azalea! How could you.");
        violationTriggers.put("GRASS_BLOCK",       "Sacred grass destroyed without a permit. Violation.");
        violationTriggers.put("COMPOSTER",         "Composter destroyed! That's a serious by-law violation.");
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata/violations.json");
        dataFile.getParentFile().mkdirs();

        if (!dataFile.exists()) {
            plugin.getLogger().info("[Violations] No data file — starting clean. All players at 0 violations.");
            return;
        }

        try (Reader r = new FileReader(dataFile)) {
            Type t = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> raw = gson.fromJson(r, t);
            if (raw != null) {
                for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                    try {
                        violations.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[Violations] Failed to load data: " + e.getMessage());
        }

        plugin.getLogger().info("[Violations] Loaded violation data for " + violations.size() + " players.");
    }

    public void saveAll() {
        Map<String, Integer> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> e : violations.entrySet()) {
            raw.put(e.getKey().toString(), e.getValue());
        }
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(raw, w);
        } catch (IOException e) {
            plugin.getLogger().severe("[Violations] Failed to save: " + e.getMessage());
        }
    }

    public void loadPlayer(Player player) {
        violations.computeIfAbsent(player.getUniqueId(), k -> 0);
    }

    // ─── Weekly Reset ─────────────────────────────────────────────────────────

    private void scheduleWeeklyReset() {
        if (weeklyResetTicks <= 0) return;

        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = violations.size();
            violations.clear();
            saveAll();

            // Broadcast to all players in the dimension
            plugin.getLogger().info("[Violations] Weekly reset complete — " + count + " violation records cleared.");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                    p.sendMessage("§2§l[Green Council] §7📋 Weekly violation amnesty granted! All records cleared. Don't make us regret this.");
                }
            }
        }, weeklyResetTicks, weeklyResetTicks);
    }

    // ─── Core API ─────────────────────────────────────────────────────────────

    /**
     * Called when a player breaks a block in the Verdant Utopia.
     * Checks if the block is a violation trigger.
     */
    public void checkBlockBreak(Player player, Material material) {
        String key = material.name();
        String message = violationTriggers.get(key);
        if (message == null) return;

        // Issue the violation
        issueViolation(player, message);
    }

    /**
     * Manually issue a violation to a player with a custom reason.
     */
    public void issueViolation(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        int newCount = violations.merge(uuid, 1, Integer::sum);

        // Actionbar notification
        String actionBar = "§c⚠ VIOLATION #" + newCount + " §7— §c-" + fineAmount + " GC §7| " + reason;
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(actionBar));

        // Chat notification
        player.sendMessage("§c§l[VIOLATION] §r§c" + reason);
        player.sendMessage("§7Fine: §c" + fineAmount + " Green Credits §7(Violation #" + newCount + " on your record)");

        // Deduct credits
        boolean paid = plugin.getGreenCreditManager().deductCredits(player, fineAmount);

        if (!paid) {
            // Can't pay — exile to spawn
            int balance = plugin.getGreenCreditManager().getBalance(player);
            player.sendMessage("§c§l[Green Council] §r§cINSUFFICIENT FUNDS! Balance: §a" + balance
                + " GC§c, Fine: §a" + fineAmount + " GC");
            player.sendMessage("§c§lENFORCED EXILE INITIATED. §rThe council thanks you for your service.");

            // Title
            player.sendTitle(
                "§c§l⚠ EXILE ⚠",
                "§cInsufficient Green Credits",
                10, 60, 20
            );

            // Teleport to spawn after brief delay (drama)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    World verdant = plugin.getDimensionManager().getVerdantWorld();
                    if (verdant != null) {
                        Location spawn = verdant.getSpawnLocation().clone();
                        player.teleport(spawn);
                        player.sendMessage("§7§o(You have been relocated to spawn pending your Green Credits debt resolution.)");
                    }
                }
            }, 40L);
        } else {
            int remaining = plugin.getGreenCreditManager().getBalance(player);
            player.sendMessage("§7Remaining balance: §a" + remaining + " GC");
        }

        plugin.getLogger().info("[Violations] " + player.getName() + " issued violation #" + newCount + " for: " + reason);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public int getViolations(Player player) {
        return violations.getOrDefault(player.getUniqueId(), 0);
    }

    public int getViolationsByUUID(UUID uuid) {
        return violations.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> getAllViolations() {
        return Collections.unmodifiableMap(violations);
    }

    public int getFineAmount() {
        return fineAmount;
    }

    public void cancelTasks() {
        if (resetTask != null) resetTask.cancel();
    }
}
