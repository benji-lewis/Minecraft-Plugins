package uk.greenparty.managers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * QuestChainManager — Multi-step quest chains for the Verdant Utopia.
 *
 * The Quest Committee approved these chains after 19 meetings, one recall vote,
 * and a heated dispute about whether "gathering signatures" counts as exercise.
 * It does. The motion passed 5-4.
 *
 * Each quest chain has multiple steps. Each step has requirements, rewards,
 * and a generous helping of Green Party bureaucracy.
 *
 * Version 1.3.0 — Phase 3 Quest Chains
 */
public class QuestChainManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // playerUUID -> active chain ID
    private final Map<UUID, String> activeChains = new HashMap<>();
    // playerUUID -> current step index in active chain
    private final Map<UUID, Integer> currentStep = new HashMap<>();
    // playerUUID -> step progress (requirement type -> count)
    private final Map<UUID, Map<String, Integer>> stepProgress = new HashMap<>();
    // playerUUID -> set of completed chain IDs
    private final Map<UUID, Set<String>> completedChains = new HashMap<>();

    private File dataFile;

    // ─── Quest Chain Definitions ──────────────────────────────────────────────

    private static final List<QuestChain> CHAINS = new ArrayList<>();

    static {
        // 1. "Petition the Council"
        CHAINS.add(new QuestChain(
            "petition_the_council",
            "§2§lPetition the Council",
            "§7The Green Council needs your help pushing through the landmark §aGreen Future Initiative§7. " +
            "Gather support, collect materials, and present your case to the Councillors.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Gather Paperwork",
                    "§7Collect §a32 paper§7 and §a4 books§7. The petition needs documentation. Lots of it.",
                    new String[]{"PAPER:32", "BOOK:4"},
                    10, 0, new String[]{"GREEN_DYE:8"},
                    "§aYou've assembled the paperwork! Councillor Wheatgrass approves. Mostly."
                ),
                new QuestStep(
                    "Step 2: Gather Signatures",
                    "§7Plant §a8 saplings§7 to demonstrate your commitment to the cause, then craft §a1 bookshelf§7 to house the signed petition.",
                    new String[]{"SAPLING_PLACE:8", "BOOKSHELF:1"},
                    15, 5, new String[]{"EMERALD:3", "OAK_SAPLING:16"},
                    "§aThe signatures are in! All 47 of them are from 'A. Concerned Citizen'."
                ),
                new QuestStep(
                    "Step 3: Present to Councillors",
                    "§7Visit §a3 different councillors§7 (right-click them) and place §a1 composter§7 near spawn to symbolise the initiative.",
                    new String[]{"NPC_INTERACT:3", "COMPOSTER_PLACE:1"},
                    25, 10, new String[]{"EMERALD:8", "WRITTEN_BOOK:1", "GREEN_WOOL:16"},
                    "§2§lMotion PASSED! §aThe Green Future Initiative is now law! The Councillors are thrilled. And suspicious."
                ),
            }
        ));

        // 2. "The Great Composting Revolution"
        CHAINS.add(new QuestChain(
            "composting_revolution",
            "§2§lThe Great Composting Revolution",
            "§7Elder Sage Fern has declared that the composting infrastructure is in crisis. " +
            "Only YOU can save the sacred compost cycle.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Survey the Damage",
                    "§7Break §a20 dirt blocks§7 to assess soil quality and collect §a16 leaves§7 for composting material.",
                    new String[]{"DIRT_BREAK:20", "LEAVES_BREAK:16"},
                    10, 0, new String[]{"COMPOSTER:2", "GREEN_DYE:4"},
                    "§aThe soil assessment is complete. It's very dirty. This is good, apparently."
                ),
                new QuestStep(
                    "Step 2: Build the Compost Network",
                    "§7Place §a3 composters§7 across the dimension and plant §a12 saplings§7 to feed the network.",
                    new String[]{"COMPOSTER_PLACE:3", "SAPLING_PLACE:12"},
                    20, 8, new String[]{"BONE_MEAL:32", "EMERALD:4"},
                    "§aThe Compost Network is live! Fern has cried three times. Happy tears. Probably."
                ),
                new QuestStep(
                    "Step 3: The Grand Compost Ceremony",
                    "§7Collect §a64 dirt§7 as sacred offering, plant §a5 more saplings§7, and place §a2 flower pots§7 as decorative tribute.",
                    new String[]{"DIRT_COLLECT:64", "SAPLING_PLACE:5", "FLOWER_POT:2"},
                    30, 15, new String[]{"EMERALD:10", "COMPOSTER:4", "GREEN_CONCRETE:32"},
                    "§2§lRevolution complete! §aThe Sacred Compost Cycle is restored. Fern has declared today a national holiday."
                ),
            }
        ));

        // 3. "Clean Energy Champion"
        CHAINS.add(new QuestChain(
            "clean_energy_champion",
            "§2§lClean Energy Champion",
            "§7Policy Officer Sedge has drafted a 247-page Clean Energy Transition Plan. " +
            "It's mostly diagrams of note blocks. Your job is to make it real.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Wind Farm Prototype",
                    "§7Place §a5 note blocks§7 (designated wind turbines) and §a8 white wool§7 to represent turbine blades.",
                    new String[]{"NOTE_BLOCK:5", "WHITE_WOOL:8"},
                    10, 5, new String[]{"NOTE_BLOCK:4", "GREEN_DYE:6"},
                    "§aPrototype approved! The council can't tell the difference from real turbines. Perfect."
                ),
                new QuestStep(
                    "Step 2: Eliminate Fossil Fuels",
                    "§7Destroy §a12 coal ore§7 and collect §a8 emeralds§7 (green energy funds the transition).",
                    new String[]{"MINE_COAL:12", "EMERALD_COLLECT:8"},
                    20, 8, new String[]{"EMERALD:6", "GREEN_STAINED_GLASS:8"},
                    "§aThe coal supply is disrupted! Councillor Briar is doing a victory lap. Loudly."
                ),
                new QuestStep(
                    "Step 3: Commission the Green Grid",
                    "§7Place §a8 note blocks§7 across the dimension, §a4 green concrete§7 foundations, and collect §a16 paper§7 for the environmental impact report.",
                    new String[]{"NOTE_BLOCK_PLACE:8", "GREEN_CONCRETE:4", "PAPER_COLLECT:16"},
                    30, 15, new String[]{"EMERALD:12", "NOTE_BLOCK:8", "GREEN_BANNER:1"},
                    "§2§lGreen Grid online! §aThe dimension is now 100% renewably powered. (This is a lie but nobody checked.)"
                ),
            }
        ));

        // 4. "Environmental Enforcement Officer"
        CHAINS.add(new QuestChain(
            "eco_enforcer",
            "§2§lEnvironmental Enforcement Officer",
            "§7Environmental Auditor Clover needs field agents. Someone has been breaking the rules " +
            "and the Greenness Index is DOWN 0.2 points. This cannot stand.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Document Violations",
                    "§7Collect §a16 paper§7 and §a4 ink sacs§7 to write up formal violation notices. The bureaucracy demands paperwork.",
                    new String[]{"PAPER:16", "INK_SAC:4"},
                    10, 0, new String[]{"GREEN_DYE:6", "PAPER:8"},
                    "§aViolation notices prepared! Clover has already filed an environmental impact assessment of the notices."
                ),
                new QuestStep(
                    "Step 2: Reclaim Defiled Land",
                    "§7Plant §a15 saplings§7 to restore deforested areas and place §a5 green wool§7 as territory markers.",
                    new String[]{"SAPLING_PLACE:15", "GREEN_WOOL:5"},
                    20, 10, new String[]{"EMERALD:5", "OAK_SAPLING:24"},
                    "§aTerritory marked and restored! The Greenness Index has recovered 0.1 point. Progress!"
                ),
                new QuestStep(
                    "Step 3: Deliver the Verdict",
                    "§7Destroy §a8 coal blocks§7 as direct action, visit §a2 councillors§7 to file your report, and place §a1 composter§7 as symbolic rehabilitation.",
                    new String[]{"MINE_COAL:8", "NPC_INTERACT:2", "COMPOSTER_PLACE:1"},
                    30, 12, new String[]{"EMERALD:10", "IRON_SWORD:1", "GREEN_CONCRETE:16"},
                    "§2§lCase closed! §aThe Greenness Index stands at 95.0. An all-time record! Clover is weeping with joy."
                ),
            }
        ));

        // 5. "The Manifesto Rewrite"
        CHAINS.add(new QuestChain(
            "manifesto_rewrite",
            "§2§lThe Manifesto Rewrite",
            "§7The Green Party Manifesto hasn't been updated since Tuesday. That's practically ancient. " +
            "Newly Elected Member Reed has proposed a full rewrite. The council voted 7-6 to allow it.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Research and Inspiration",
                    "§7Collect §a3 books§7 and §a24 paper§7 for the drafting committee. Also plant §a5 saplings§7 for inspiration.",
                    new String[]{"BOOK_COLLECT:3", "PAPER:24", "SAPLING_PLACE:5"},
                    10, 5, new String[]{"BOOK:2", "GREEN_DYE:8"},
                    "§aResearch complete! Reed has already filled 47 pages with diagrams of composters."
                ),
                new QuestStep(
                    "Step 2: Consultation Phase",
                    "§7Consult §a4 councillors§7 (right-click them) and place §a2 bookshelves§7 to establish the Manifesto Library.",
                    new String[]{"NPC_INTERACT:4", "BOOKSHELF:2"},
                    20, 8, new String[]{"EMERALD:6", "BOOKSHELF:1", "OAK_SAPLING:8"},
                    "§aConsultation complete! 4 councillors gave 4 completely different answers. The process works."
                ),
                new QuestStep(
                    "Step 3: Launch the New Manifesto",
                    "§7Craft §a1 written book§7 (any content), plant §a10 saplings§7 as celebration, and place §a3 green banners§7 for the grand unveiling.",
                    new String[]{"BOOK_WRITE:1", "SAPLING_PLACE:10", "BANNER_PLACE:3"},
                    30, 15, new String[]{"EMERALD:15", "WRITTEN_BOOK:2", "GREEN_BANNER:4", "GREEN_CONCRETE:32"},
                    "§2§lManifesto v74 published! §aIt now has 94 points. Points 47-73 are all about composting."
                ),
            }
        ));

        // 6. "The Carbon Neutral Campaign"
        CHAINS.add(new QuestChain(
            "carbon_neutral",
            "§2§lThe Carbon Neutral Campaign",
            "§7Sustainability Guru Moss has issued a challenge: make the dimension carbon neutral " +
            "by the end of the in-game day. It's physically impossible. The council approved it anyway.",
            new QuestStep[]{
                new QuestStep(
                    "Step 1: Carbon Audit",
                    "§7Collect §a8 emeralds§7 and §a16 paper§7 for the audit report. Break §a10 coal ore§7 to reduce stockpiles.",
                    new String[]{"EMERALD_COLLECT:8", "PAPER:16", "MINE_COAL:10"},
                    12, 0, new String[]{"GREEN_DYE:12", "EMERALD:4"},
                    "§aAudit complete! Carbon footprint: -0.2 tonnes. Moss claims credit for all of it."
                ),
                new QuestStep(
                    "Step 2: Reforestation Drive",
                    "§7Plant §a20 saplings§7 across the dimension. Each sapling offsets one bad decision by the council.",
                    new String[]{"SAPLING_PLACE:20"},
                    20, 10, new String[]{"OAK_SAPLING:32", "BONE_MEAL:16", "EMERALD:5"},
                    "§a20 saplings planted! The dimension's canopy coverage is up 3%. Moss approves."
                ),
                new QuestStep(
                    "Step 3: Carbon Offset Ceremony",
                    "§7Place §a5 composters§7, §a4 green concrete§7 foundation blocks, and plant §a10 more saplings§7 for the grand total.",
                    new String[]{"COMPOSTER_PLACE:5", "GREEN_CONCRETE:4", "SAPLING_PLACE:10"},
                    30, 20, new String[]{"EMERALD:20", "COMPOSTER:3", "GREEN_BANNER:2", "LIME_CONCRETE:64"},
                    "§2§lCarbon neutral achieved! §aThe dimension is now 100% carbon neutral. (Moss wrote a 94-page report about it.)"
                ),
            }
        ));
    }

    public QuestChainManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "playerdata/quest-chains.json");
        dataFile.getParentFile().mkdirs();
        loadData();
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public void handleCommand(Player player, String[] args) {
        if (args.length < 2) {
            showHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list"     -> listChains(player);
            case "start"    -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /quest start <quest_name>");
                } else {
                    String name = String.join("_", Arrays.copyOfRange(args, 2, args.length)).toLowerCase();
                    startChain(player, name);
                }
            }
            case "progress" -> showProgress(player);
            case "abandon"  -> abandonChain(player);
            default         -> showHelp(player);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§2§l===== Quest Chain Commands =====");
        player.sendMessage("§a/quest list §7— Show all available quest chains");
        player.sendMessage("§a/quest start <name> §7— Begin a quest chain");
        player.sendMessage("§a/quest progress §7— Show your current step");
        player.sendMessage("§a/quest abandon §7— Abandon your current quest");
        player.sendMessage("§8(The council recommends not abandoning. It's in bad taste.)");
    }

    public void listChains(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> completed = completedChains.getOrDefault(uuid, new HashSet<>());
        String active = activeChains.get(uuid);

        player.sendMessage("§2§l===== Quest Chains — The Verdant Utopia =====");
        for (QuestChain chain : CHAINS) {
            String status;
            if (completed.contains(chain.id)) {
                status = "§a✓ COMPLETE";
            } else if (chain.id.equals(active)) {
                int step = currentStep.getOrDefault(uuid, 0);
                status = "§e▶ IN PROGRESS (Step " + (step + 1) + "/" + chain.steps.length + ")";
            } else {
                status = "§7○ Available";
            }
            player.sendMessage("  " + chain.displayName + " §8— " + status);
            player.sendMessage("    §8" + chain.description.replaceAll("§.", ""));
        }
        player.sendMessage("§7Use §a/quest start <name>§7 to begin. Names use underscores.");
        player.sendMessage("§2§l==============================================");
    }

    public void startChain(Player player, String chainId) {
        UUID uuid = player.getUniqueId();

        if (activeChains.containsKey(uuid)) {
            String current = activeChains.get(uuid);
            QuestChain currentChain = getChain(current);
            String name = currentChain != null ? currentChain.displayName : current;
            player.sendMessage("§cYou already have an active quest: " + name);
            player.sendMessage("§7Use §c/quest abandon§7 first. §8(The council notes this as a lack of commitment.)");
            return;
        }

        if (completedChains.getOrDefault(uuid, new HashSet<>()).contains(chainId)) {
            player.sendMessage("§7You've already completed that quest chain! The council has filed it away.");
            player.sendMessage("§8(It's in the archive. Under 'Commendable Achievements'. Sub-folder 'B'.)");
            return;
        }

        QuestChain chain = getChain(chainId);
        if (chain == null) {
            player.sendMessage("§cUnknown quest chain: §7" + chainId);
            player.sendMessage("§7Use §a/quest list§7 to see available chains.");
            return;
        }

        activeChains.put(uuid, chainId);
        currentStep.put(uuid, 0);
        stepProgress.computeIfAbsent(uuid, k -> new HashMap<>()).clear();

        QuestStep firstStep = chain.steps[0];

        player.sendMessage("");
        player.sendMessage("§2§l╔════════════════════════════════════╗");
        player.sendMessage("§2§l║       QUEST CHAIN STARTED! 🌿       ║");
        player.sendMessage("§2§l╚════════════════════════════════════╝");
        player.sendMessage(chain.displayName);
        player.sendMessage("§7" + chain.description.replaceAll("§.", ""));
        player.sendMessage("");
        player.sendMessage("§a" + firstStep.name);
        player.sendMessage("  " + firstStep.description);
        showStepRequirements(player, firstStep);
        player.sendMessage("");
        player.sendMessage("§8(Good luck. The council is watching. Possibly composting.)");

        saveData();
    }

    public void showProgress(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activeChains.containsKey(uuid)) {
            player.sendMessage("§7You have no active quest chain. Use §a/quest list§7 to find one.");
            player.sendMessage("§8The council notes you are currently living without purpose.");
            return;
        }

        String chainId = activeChains.get(uuid);
        QuestChain chain = getChain(chainId);
        if (chain == null) {
            player.sendMessage("§cError: Active quest not found. The committee has been notified.");
            return;
        }

        int step = currentStep.getOrDefault(uuid, 0);
        QuestStep currentStepDef = chain.steps[step];
        Map<String, Integer> progress = stepProgress.getOrDefault(uuid, new HashMap<>());

        player.sendMessage("§2§l===== Quest Progress =====");
        player.sendMessage(chain.displayName + " §8— Step " + (step + 1) + "/" + chain.steps.length);
        player.sendMessage("");
        player.sendMessage("§a" + currentStepDef.name);
        player.sendMessage("  " + currentStepDef.description);
        player.sendMessage("");
        player.sendMessage("§7Requirements:");

        for (String req : currentStepDef.requirements) {
            String[] parts = req.split(":");
            String type = parts[0];
            int target = Integer.parseInt(parts[1]);
            int current = progress.getOrDefault(type, 0);
            String bar = buildBar(current, target, 10);
            String checkmark = current >= target ? "§a✓" : "§7○";
            player.sendMessage("  " + checkmark + " §7" + formatRequirement(type) + ": " + bar + " " + current + "/" + target);
        }

        if (step < chain.steps.length - 1) {
            player.sendMessage("");
            player.sendMessage("§8Next step preview: §7" + chain.steps[step + 1].name);
        }

        player.sendMessage("§2§l==========================");
    }

    public void abandonChain(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activeChains.containsKey(uuid)) {
            player.sendMessage("§7You have no active quest to abandon. §8(Not that you could anyway. Commitment is important.)");
            return;
        }

        String chainId = activeChains.get(uuid);
        QuestChain chain = getChain(chainId);
        String name = chain != null ? chain.displayName : chainId;

        activeChains.remove(uuid);
        currentStep.remove(uuid);
        stepProgress.getOrDefault(uuid, new HashMap<>()).clear();

        player.sendMessage("§cQuest abandoned: " + name);
        player.sendMessage("§7The council has noted your retreat. §8Motion 104b: Formal Disappointment. Passed 8-1.");
        saveData();
    }

    // ─── Progress Recording ───────────────────────────────────────────────────

    /**
     * Record progress for the active quest chain. Called by listeners.
     * @param player the player
     * @param requirementType the requirement type (e.g., "SAPLING_PLACE", "MINE_COAL")
     * @param amount how much to add
     */
    public void recordProgress(Player player, String requirementType, int amount) {
        UUID uuid = player.getUniqueId();
        if (!activeChains.containsKey(uuid)) return;

        String chainId = activeChains.get(uuid);
        QuestChain chain = getChain(chainId);
        if (chain == null) return;

        int step = currentStep.getOrDefault(uuid, 0);
        if (step >= chain.steps.length) return;

        QuestStep stepDef = chain.steps[step];
        Map<String, Integer> progress = stepProgress.computeIfAbsent(uuid, k -> new HashMap<>());

        // Check if this requirement type applies to any requirement in the current step
        boolean relevant = false;
        for (String req : stepDef.requirements) {
            if (req.startsWith(requirementType + ":")) {
                relevant = true;
                break;
            }
        }
        if (!relevant) return;

        // Update progress
        for (String req : stepDef.requirements) {
            if (req.startsWith(requirementType + ":")) {
                int target = Integer.parseInt(req.split(":")[1]);
                int current = progress.getOrDefault(requirementType, 0);
                int updated = Math.min(current + amount, target);
                progress.put(requirementType, updated);

                if (updated > current) {
                    // Milestone notification
                    if (updated == target || (updated % Math.max(1, target / 4) == 0)) {
                        player.sendMessage("§a[Quest] §7" + formatRequirement(requirementType) + ": §a" + updated + "/" + target);
                    }
                }
            }
        }

        // Check if step is complete
        checkStepCompletion(player, chain, step);
    }

    private void checkStepCompletion(Player player, QuestChain chain, int step) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> progress = stepProgress.getOrDefault(uuid, new HashMap<>());
        QuestStep stepDef = chain.steps[step];

        for (String req : stepDef.requirements) {
            String[] parts = req.split(":");
            String type = parts[0];
            int target = Integer.parseInt(parts[1]);
            if (progress.getOrDefault(type, 0) < target) return; // Not done yet
        }

        // Step complete!
        completeStep(player, chain, step);
    }

    private void completeStep(Player player, QuestChain chain, int step) {
        UUID uuid = player.getUniqueId();
        QuestStep stepDef = chain.steps[step];

        player.sendMessage("");
        player.sendMessage("§a§l✦ STEP COMPLETE! ✦");
        player.sendMessage("§2" + stepDef.name);
        player.sendMessage("§7" + stepDef.completionMessage.replaceAll("§.", ""));
        player.sendMessage("");

        // Rewards
        if (stepDef.greenCredits > 0) {
            plugin.getGreenCreditManager().addCredits(player, stepDef.greenCredits,
                "Quest step: " + stepDef.name);
            player.sendMessage("§a+" + stepDef.greenCredits + " Green Credits §7(the council pays, reluctantly)");
        }
        if (stepDef.ecoBoost > 0) {
            plugin.getEcoScoreManager().addScore(player, stepDef.ecoBoost);
            player.sendMessage("§a+" + stepDef.ecoBoost + " Eco-Score §7(your green credentials improve)");
        }
        for (String itemStr : stepDef.rewardItems) {
            try {
                String[] parts = itemStr.split(":");
                Material mat = Material.matchMaterial(parts[0]);
                int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                if (mat != null) {
                    player.getInventory().addItem(new ItemStack(mat, qty));
                    player.sendMessage("§a+" + qty + " §7" + parts[0].toLowerCase().replace("_", " "));
                }
            } catch (Exception ignored) {}
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        int nextStep = step + 1;
        if (nextStep >= chain.steps.length) {
            // Entire chain complete!
            completeChain(player, chain);
        } else {
            // Advance to next step
            currentStep.put(uuid, nextStep);
            stepProgress.getOrDefault(uuid, new HashMap<>()).clear();

            QuestStep next = chain.steps[nextStep];
            player.sendMessage("");
            player.sendMessage("§e§l▶ Next Step: §r§a" + next.name);
            player.sendMessage("  " + next.description);
            showStepRequirements(player, next);
        }

        saveData();
    }

    private void completeChain(Player player, QuestChain chain) {
        UUID uuid = player.getUniqueId();
        activeChains.remove(uuid);
        currentStep.remove(uuid);
        stepProgress.getOrDefault(uuid, new HashMap<>()).clear();
        completedChains.computeIfAbsent(uuid, k -> new HashSet<>()).add(chain.id);

        player.sendMessage("");
        player.sendMessage("§2§l╔══════════════════════════════════════════╗");
        player.sendMessage("§2§l║     QUEST CHAIN COMPLETE! 🌿🌿🌿          ║");
        player.sendMessage("§2§l╚══════════════════════════════════════════╝");
        player.sendMessage(chain.displayName);
        player.sendMessage("§7The Green Council is absolutely delighted.");
        player.sendMessage("§7(A motion to award you a commendation has been filed. 6-8 weeks.)");
        player.sendMessage("");

        // Bonus GC for completing entire chain
        int bonusGC = 30;
        plugin.getGreenCreditManager().addCredits(player, bonusGC, "Quest chain completion: " + chain.id);
        player.sendMessage("§2§l BONUS: §a+" + bonusGC + " Green Credits §7for completing the full chain!");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Broadcast
        World verdant = plugin.getDimensionManager().getVerdantWorld();
        if (verdant != null) {
            verdant.getPlayers().forEach(p ->
                p.sendMessage("§2🌿 §l" + player.getName() + "§r §2has completed the §l" +
                    chain.displayName.replaceAll("§.", "") + "§r §2quest chain! The council applauds!")
            );
        }

        saveData();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void showStepRequirements(Player player, QuestStep step) {
        player.sendMessage("§7Requirements:");
        for (String req : step.requirements) {
            String[] parts = req.split(":");
            player.sendMessage("  §7• " + formatRequirement(parts[0]) + ": §a" + parts[1]);
        }
        String rewardStr = "§a+" + step.greenCredits + " GC";
        if (step.ecoBoost > 0) rewardStr += "§7, §a+" + step.ecoBoost + " Eco-Score";
        player.sendMessage("§7Rewards: " + rewardStr);
    }

    private String formatRequirement(String type) {
        return switch (type) {
            case "SAPLING_PLACE"    -> "Plant saplings";
            case "COMPOSTER_PLACE"  -> "Place composters";
            case "MINE_COAL"        -> "Break coal ore";
            case "NOTE_BLOCK"       -> "Place note blocks";
            case "NOTE_BLOCK_PLACE" -> "Place note blocks";
            case "NPC_INTERACT"     -> "Talk to councillors";
            case "PAPER"            -> "Collect paper";
            case "PAPER_COLLECT"    -> "Collect paper";
            case "BOOK_COLLECT"     -> "Collect books";
            case "BOOK_WRITE"       -> "Write a book";
            case "BOOKSHELF"        -> "Place bookshelves";
            case "BANNER_PLACE"     -> "Place banners";
            case "DIRT_BREAK"       -> "Break dirt";
            case "LEAVES_BREAK"     -> "Break leaf blocks";
            case "DIRT_COLLECT"     -> "Collect dirt";
            case "GREEN_WOOL"       -> "Place green wool";
            case "GREEN_CONCRETE"   -> "Place green concrete";
            case "EMERALD_COLLECT"  -> "Collect emeralds";
            case "FLOWER_POT"       -> "Place flower pots";
            case "INK_SAC"          -> "Collect ink sacs";
            default                 -> type.replace("_", " ").toLowerCase();
        };
    }

    private String buildBar(int current, int target, int width) {
        int filled = Math.min(width, (int)((current / (double)target) * width));
        StringBuilder sb = new StringBuilder("§a[");
        for (int i = 0; i < width; i++) {
            if (i < filled) sb.append("█");
            else sb.append("§7░");
        }
        sb.append("§a]");
        return sb.toString();
    }

    private QuestChain getChain(String id) {
        return CHAINS.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
    }

    public boolean hasActiveChain(Player player) {
        return activeChains.containsKey(player.getUniqueId());
    }

    public int getCompletedChainsCount(Player player) {
        return completedChains.getOrDefault(player.getUniqueId(), new HashSet<>()).size();
    }

    public Set<String> getCompletedChains(Player player) {
        return Collections.unmodifiableSet(completedChains.getOrDefault(player.getUniqueId(), new HashSet<>()));
    }

    public List<QuestChain> getAllChains() {
        return Collections.unmodifiableList(CHAINS);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) return;

        try (Reader r = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(r, type);
            if (data == null) return;

            // Active chains
            Map<String, String> ac = (Map<String, String>) data.get("activeChains");
            if (ac != null) ac.forEach((k, v) -> {
                try { activeChains.put(UUID.fromString(k), v); } catch (Exception ignored) {}
            });

            // Current steps
            Map<String, Double> cs = (Map<String, Double>) data.get("currentStep");
            if (cs != null) cs.forEach((k, v) -> {
                try { currentStep.put(UUID.fromString(k), v.intValue()); } catch (Exception ignored) {}
            });

            // Step progress
            Map<String, Map<String, Double>> sp = (Map<String, Map<String, Double>>) data.get("stepProgress");
            if (sp != null) sp.forEach((k, v) -> {
                try {
                    UUID uuid = UUID.fromString(k);
                    Map<String, Integer> progress = new HashMap<>();
                    v.forEach((req, count) -> progress.put(req, count.intValue()));
                    stepProgress.put(uuid, progress);
                } catch (Exception ignored) {}
            });

            // Completed chains
            Map<String, List<String>> cc = (Map<String, List<String>>) data.get("completedChains");
            if (cc != null) cc.forEach((k, v) -> {
                try { completedChains.put(UUID.fromString(k), new HashSet<>(v)); } catch (Exception ignored) {}
            });

        } catch (IOException e) {
            plugin.getLogger().severe("[QuestChains] Failed to load data: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();

            Map<String, String> ac = new HashMap<>();
            activeChains.forEach((k, v) -> ac.put(k.toString(), v));
            data.put("activeChains", ac);

            Map<String, Integer> cs = new HashMap<>();
            currentStep.forEach((k, v) -> cs.put(k.toString(), v));
            data.put("currentStep", cs);

            Map<String, Map<String, Integer>> sp = new HashMap<>();
            stepProgress.forEach((k, v) -> sp.put(k.toString(), v));
            data.put("stepProgress", sp);

            Map<String, List<String>> cc = new HashMap<>();
            completedChains.forEach((k, v) -> cc.put(k.toString(), new ArrayList<>(v)));
            data.put("completedChains", cc);

            try (Writer w = new FileWriter(dataFile)) {
                gson.toJson(data, w);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[QuestChains] Failed to save data: " + e.getMessage());
        }
    }

    // ─── Inner Records ────────────────────────────────────────────────────────

    public static class QuestChain {
        public final String id;
        public final String displayName;
        public final String description;
        public final QuestStep[] steps;

        public QuestChain(String id, String displayName, String description, QuestStep[] steps) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.steps = steps;
        }
    }

    public static class QuestStep {
        public final String name;
        public final String description;
        public final String[] requirements; // e.g., "SAPLING_PLACE:10"
        public final int greenCredits;
        public final int ecoBoost;
        public final String[] rewardItems;
        public final String completionMessage;

        public QuestStep(String name, String description, String[] requirements,
                         int greenCredits, int ecoBoost, String[] rewardItems,
                         String completionMessage) {
            this.name = name;
            this.description = description;
            this.requirements = requirements;
            this.greenCredits = greenCredits;
            this.ecoBoost = ecoBoost;
            this.rewardItems = rewardItems;
            this.completionMessage = completionMessage;
        }
    }
}
