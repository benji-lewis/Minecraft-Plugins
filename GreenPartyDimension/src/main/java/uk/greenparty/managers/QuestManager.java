package uk.greenparty.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * Manages quests in The Verdant Utopia.
 *
 * Quest philosophy: "Every task is a metaphor for environmental stewardship."
 * Quest reward philosophy: "The real reward was the trees we planted along the way."
 *
 * The quest board is maintained by Councillor Wheatgrass, who updates it after
 * every meeting of the Quest Committee (meets every third Tuesday, weather permitting,
 * unless there's a protest on, in which case all members are unavailable).
 */
public class QuestManager {

    private final GreenPartyPlugin plugin;

    // Player quest data: playerUUID -> (questId -> progress)
    private final Map<UUID, Map<String, Integer>> playerProgress = new HashMap<>();
    private final Map<UUID, Set<String>> completedQuests = new HashMap<>();
    private final Map<UUID, List<String>> activeQuests = new HashMap<>();

    // Quest definitions — the sacred texts of the Green Council
    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    static {
        // These quests have been approved by the Quest Committee after 14 meetings
        QUESTS.put("compost_crisis", new QuestDefinition(
            "compost_crisis",
            "§2The Great Compost Crisis of Chunk 17",
            "§7Something has disrupted the sacred composting cycle. Collect §a64 dirt blocks§7 to restore balance to the Verdant Plains.",
            "DIRT",
            64,
            500,
            new String[]{"GREEN_DYE:32", "COMPOSTER:1"}
        ));

        QUESTS.put("tree_planting", new QuestDefinition(
            "tree_planting",
            "§2One Tree for Every Slogan",
            "§7Plant §a10 saplings§7. The council recommends oak. Birch is acceptable. The committee is still debating pine.",
            "SAPLING_PLACE",
            10,
            750,
            new String[]{"OAK_SAPLING:64", "EMERALD:5"}
        ));

        QUESTS.put("fossil_fuel_protest", new QuestDefinition(
            "fossil_fuel_protest",
            "§2Down With Fossil Fuels (The Coal Blocks)",
            "§7Destroy §a16 coal ore blocks§7. §8(This quest has been condemned by the Coal Ore Liberation Front, a splinter group of 2 people.)",
            "MINE_COAL",
            16,
            600,
            new String[]{"COAL:32", "GREEN_WOOL:16"}
        ));

        QUESTS.put("renewable_energy", new QuestDefinition(
            "renewable_energy",
            "§2Wind Power or Bust",
            "§7Place §a5 note blocks§7 in the dimension (they represent wind turbines). The council cannot tell the difference.",
            "NOTE_BLOCK",
            5,
            400,
            new String[]{"NOTE_BLOCK:10", "GREEN_DYE:16"}
        ));

        QUESTS.put("biodiversity_survey", new QuestDefinition(
            "biodiversity_survey",
            "§2Emergency Biodiversity Survey of the Verdant Plains",
            "§7The council needs numbers. Right-click §a10 animals§7 to 'survey' them. They will not enjoy it.",
            "SURVEY_MOB",
            10,
            1000,
            new String[]{"EMERALD:10"}
        ));

        QUESTS.put("green_infrastructure", new QuestDefinition(
            "green_infrastructure",
            "§2Phase 1 of the Green Infrastructure Plan (Phase 2 Is In Committee)",
            "§7Build a §a3x3 platform of green concrete§7. Submit planning permission (just type /greenparty quest submit).",
            "GREEN_CONCRETE",
            9,
            800,
            new String[]{"GREEN_CONCRETE:64", "LIME_DYE:32"}
        ));

        QUESTS.put("petition_drive", new QuestDefinition(
            "petition_drive",
            "§2The Petition (We Need More Signatures)",
            "§7Collect §a32 paper§7 for the ongoing petition. What is it for? The committee is still deciding.",
            "PAPER",
            32,
            300,
            new String[]{"PAPER:64", "BOOK:5"}
        ));
    }

    public QuestManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    public void assignQuests(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> quests = activeQuests.computeIfAbsent(uuid, k -> new ArrayList<>());

        if (quests.size() >= 3) {
            player.sendMessage("§7You already have §a3 active quests§7. The council says that's quite enough for now.");
            return;
        }

        Set<String> completed = completedQuests.getOrDefault(uuid, new HashSet<>());
        List<String> available = new ArrayList<>();
        for (String questId : QUESTS.keySet()) {
            if (!completed.contains(questId) && !quests.contains(questId)) {
                available.add(questId);
            }
        }

        if (available.isEmpty()) {
            player.sendMessage("§2Extraordinary! You have completed all available quests!");
            player.sendMessage("§7The council is filing a motion to create more quests. It's in committee.");
            return;
        }

        // Assign up to 3-quests worth
        Collections.shuffle(available);
        int toAssign = Math.min(3 - quests.size(), available.size());
        for (int i = 0; i < toAssign; i++) {
            quests.add(available.get(i));
        }

        player.sendMessage("§2§lQuest Board Update!§r §aYou've been assigned " + toAssign + " quest(s):");
        for (int i = quests.size() - toAssign; i < quests.size(); i++) {
            QuestDefinition quest = QUESTS.get(quests.get(i));
            if (quest != null) {
                player.sendMessage("  §7• " + quest.name);
                player.sendMessage("    §8" + quest.description);
            }
        }
        player.sendMessage("§7Use §a/greenparty quest§7 to check progress.");
    }

    public void showQuestStatus(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> quests = activeQuests.getOrDefault(uuid, new ArrayList<>());
        Map<String, Integer> progress = playerProgress.getOrDefault(uuid, new HashMap<>());
        Set<String> completed = completedQuests.getOrDefault(uuid, new HashSet<>());

        player.sendMessage("§2§l===== YOUR QUEST BOARD =====");

        if (quests.isEmpty()) {
            player.sendMessage("§7No active quests. Use §a/greenparty quest assign§7 to get some.");
            player.sendMessage("§7The council has " + QUESTS.size() + " quests available. " + completed.size() + " completed.");
        } else {
            for (String questId : quests) {
                QuestDefinition quest = QUESTS.get(questId);
                if (quest == null) continue;
                int prog = progress.getOrDefault(questId, 0);
                int percent = (int)((prog / (double)quest.target) * 100);
                String bar = buildProgressBar(prog, quest.target);
                player.sendMessage(quest.name);
                player.sendMessage("  " + bar + " §7" + prog + "/" + quest.target + " (" + percent + "%)");
            }
        }

        player.sendMessage("§8Completed: " + completed.size() + "/" + QUESTS.size() + " quests");
        player.sendMessage("§2§l=============================");
    }

    public void recordProgress(Player player, String questType, int amount) {
        UUID uuid = player.getUniqueId();
        List<String> quests = activeQuests.getOrDefault(uuid, new ArrayList<>());
        Map<String, Integer> progress = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());

        for (String questId : new ArrayList<>(quests)) {
            QuestDefinition quest = QUESTS.get(questId);
            if (quest == null || !quest.trackType.equals(questType)) continue;

            int current = progress.getOrDefault(questId, 0);
            int newProgress = Math.min(current + amount, quest.target);
            progress.put(questId, newProgress);

            if (newProgress >= quest.target && current < quest.target) {
                completeQuest(player, questId, quest);
            } else if (newProgress > current) {
                // Show progress update (but not too often)
                if (newProgress % Math.max(1, quest.target / 5) == 0 || newProgress == quest.target - 1) {
                    player.sendMessage("§aQuest progress: §2" + quest.name.replace("§2", "").replace("§a","").trim());
                    player.sendMessage("  §7" + newProgress + "/" + quest.target + " " + buildProgressBar(newProgress, quest.target));
                }
            }
        }
    }

    private void completeQuest(Player player, String questId, QuestDefinition quest) {
        UUID uuid = player.getUniqueId();
        activeQuests.getOrDefault(uuid, new ArrayList<>()).remove(questId);
        completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(questId);

        // Fanfare!
        player.sendMessage("");
        player.sendMessage("§2§l╔══════════════════════════════════╗");
        player.sendMessage("§2§l║       QUEST COMPLETE! 🌿          ║");
        player.sendMessage("§2§l╚══════════════════════════════════╝");
        player.sendMessage("§a" + quest.name.replaceAll("§.", ""));
        player.sendMessage("§7The Green Council applauds your efforts. Politely.");
        player.sendMessage("");

        // Give XP
        player.giveExp(quest.rewardXp);
        player.sendMessage("§a+" + quest.rewardXp + " XP§7 (The council considers this adequate compensation)");

        // Give items
        for (String itemStr : quest.rewardItems) {
            try {
                String[] parts = itemStr.split(":");
                Material mat = Material.matchMaterial(parts[0]);
                int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                if (mat != null) {
                    player.getInventory().addItem(new ItemStack(mat, qty));
                    player.sendMessage("§a+" + qty + " §7" + formatMaterialName(parts[0]));
                }
            } catch (Exception ignored) {}
        }

        // Award Green Credits for quest completion
        plugin.getGreenCreditManager().rewardQuestCompletion(player);

        // Sound effect
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Broadcast (if they want fame)
        int completed = completedQuests.getOrDefault(uuid, new HashSet<>()).size();
        if (completed % 3 == 0) {
            Bukkit.broadcastMessage("§2🌿 " + player.getName() + " has completed " + completed + " Green Party quests! The council is pleased!");
        }
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerProgress.remove(uuid);
        completedQuests.remove(uuid);
        activeQuests.remove(uuid);
        player.sendMessage("§aYour Green Party progress has been reset. The council calls this 'starting fresh'. Like compost.");
    }

    private String buildProgressBar(int current, int target) {
        int bars = 20;
        int filled = (int)((current / (double)target) * bars);
        StringBuilder sb = new StringBuilder("§a[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) sb.append("█");
            else sb.append("§7░");
        }
        sb.append("§a]");
        return sb.toString();
    }

    private String formatMaterialName(String material) {
        return material.replace("_", " ").toLowerCase();
    }

    public Map<String, QuestDefinition> getAllQuests() {
        return Collections.unmodifiableMap(QUESTS);
    }

    // Inner class for quest definitions
    public static class QuestDefinition {
        public final String id;
        public final String name;
        public final String description;
        public final String trackType;
        public final int target;
        public final int rewardXp;
        public final String[] rewardItems;

        public QuestDefinition(String id, String name, String description,
                               String trackType, int target, int rewardXp, String[] rewardItems) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.trackType = trackType;
            this.target = target;
            this.rewardXp = rewardXp;
            this.rewardItems = rewardItems;
        }
    }
}
