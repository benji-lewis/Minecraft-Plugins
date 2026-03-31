package uk.greenparty.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.QuestManager;
import uk.greenparty.managers.RecyclingManager;

import java.util.*;

/**
 * Listens for player events in The Verdant Utopia.
 *
 * Key responsibilities:
 * - Welcome players to the dimension with appropriate fanfare
 * - Track quest progress
 * - Enforce the Green Code (no coal, no fire, no environmental recklessness)
 * - Update eco-scores in real time
 * - Track achievements and credit eligibility
 * - Issue environmental violations for block breaks
 * - Handle recycling bin item throws
 * - Enforce kidnap status (cannot /greenparty leave if conscripted)
 * - Generally be preachy about the environment at every opportunity
 */
public class PlayerListener implements Listener {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Thematic kidnap messages
    private static final String[] KIDNAP_MESSAGES = {
        "§2§l⚡ THE GREEN COUNCIL HAS SELECTED YOU. ⚡",
        "§2§l🌿 CONSCRIPTED INTO THE VERDANT REVOLUTION! 🌿",
        "§2§l📜 YOUR CARBON FOOTPRINT HAS BEEN NOTICED. 📜",
        "§2§l🌱 THE ARBOREAL COMMITTEE REQUIRES YOUR PRESENCE. 🌱",
        "§2§l♻️ YOU HAVE BEEN RECYCLED INTO THE GREEN CAUSE. ♻️",
    };

    private static final String[] KIDNAP_SUBTITLES = {
        "§aResistance is futile. And bad for the planet.",
        "§aYour composting skills are needed.",
        "§aThere are 47 agenda items. You're on all of them.",
        "§aWelcome, comrade. The trees are watching.",
        "§aThe dimension chose you. We accept no refunds.",
    };

    // Cooldown tracking to avoid spamming players
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 30_000L; // 30 seconds

    // Track which players have had recycling bins spawned for their session
    private boolean binsSpawned = false;

    private static final String[] GREEN_TIPS = {
        "§8[§2Green Tip§8] §7Did you know? The Verdant Utopia produces zero carbon. Because it's virtual. Still counts.",
        "§8[§2Green Tip§8] §7Trees in this dimension were planted with permission from the Arboreal Committee.",
        "§8[§2Green Tip§8] §7Coal ore is replaced with emerald here. The council considers this symbolic. And practical.",
        "§8[§2Green Tip§8] §7The Verdant Utopia has been certified 'Very Green' by the Green Rating Board (us).",
        "§8[§2Green Tip§8] §7Use §a/greenparty quest§7 to check your quest progress. The council is watching. Supportively.",
        "§8[§2Green Tip§8] §7Talk to the Green Councillors! They have MUCH to say. So much. So, so much.",
        "§8[§2Green Tip§8] §7This dimension is peaceful. Hostile mobs were banned by council decree. §8(Motion 47b, second reading.)",
        "§8[§2Green Tip§8] §7Use §a/greencredit balance§7 to check your Green Credits. The economy of the future.",
        "§8[§2Green Tip§8] §7Check your eco-score in the action bar! Plant saplings to raise it. Mine coal at your peril.",
        "§8[§2Green Tip§8] §7Use §a/achievements§7 to track your environmental heroism (or failure).",
        "§8[§2Green Tip§8] §7♻ §7Throw items at Recycling Bins to recycle them and earn §aGreen Credits§7!",
        "§8[§2Green Tip§8] §7Breaking blocks in this dimension may result in §cviolations§7. The council is watching.",
        "§8[§2Green Tip§8] §7Use §a/motion vote yes§7 to participate in the democratic process! §8(While there's a motion active.)",
        "§8[§2Green Tip§8] §7Recycling bins are scattered throughout the dimension. §a♻ §7Look for the glowing stands!",
    };

    private static final Map<String, String> MINING_REACTIONS = new HashMap<>();
    static {
        MINING_REACTIONS.put("COAL_ORE", "§c[Green Council] §7Coal ore?! In §2The Verdant Utopia§7?! This will be added to the agenda immediately.");
        MINING_REACTIONS.put("DEEPSLATE_COAL_ORE", "§c[Green Council] §7Deepslate coal?! Even deeper treachery. Violation incoming.");
        MINING_REACTIONS.put("GRASS_BLOCK", "§e[Green Council] §7You've dug up sacred grass! The council notes this with §ldispleasure§r§7.");
        MINING_REACTIONS.put("OAK_LOG", "§e[Green Council] §7A tree has been felled. Please plant a replacement. §8Or two. The council prefers two.");
        MINING_REACTIONS.put("COMPOSTER", "§c[Green Council] §7You've destroyed a composter! This is a §lserious violation§r§7 of By-law 12.");
    }

    public PlayerListener(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getEcoScoreManager().loadPlayer(player);
        plugin.getGreenCreditManager().loadPlayer(player);
        plugin.getAchievementManager().loadPlayer(player);
        plugin.getViolationManager().loadPlayer(player);
        // Phase 4
        plugin.getCosmeticsManager().onPlayerJoin(player);
        plugin.getEnvironmentEffects().onPlayerJoin(player);

        // ─── Kidnap Handling ──────────────────────────────────────────────────
        boolean isKidnaped = plugin.isKidnapped(player.getUniqueId());

        // Also check config-based targets (first time kidnap)
        boolean isTarget = !plugin.getKidnapTargets().isEmpty()
            && plugin.getKidnapTargets().contains(player.getName().toLowerCase());

        if (isTarget && !isKidnaped) {
            // First time — add to persistent kidnapped list
            plugin.addKidnapped(player.getUniqueId());
            isKidnaped = true;
        }

        if (isKidnaped) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getAchievementManager().recordKidnap(player);
                    performKidnap(player);
                }
            }, 100L);
            return;
        }

        // Normal join
        if (plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            handleEnterDimension(player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage("§2§l[Green Party]§r §7Welcome, " + player.getName() + "! Type §a/greenparty§7 to access the Verdant Utopia.");
                player.sendMessage("§7Or type §a/greenparty teleport§7 to visit directly. The council is expecting you. Sort of.");
            }
        }, 60L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getEcoScoreManager().savePlayer(player);
        plugin.getGreenCreditManager().savePlayer(player);
        plugin.getAchievementManager().saveAll();
        plugin.getViolationManager().saveAll();
        // Phase 4
        plugin.getCosmeticsManager().onPlayerQuit(player);
        plugin.getEnvironmentEffects().onPlayerQuit(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        boolean isKidnapped = plugin.isKidnapped(player.getUniqueId());
        if (!isKidnapped) {
            isKidnapped = !plugin.getKidnapTargets().isEmpty()
                && plugin.getKidnapTargets().contains(player.getName().toLowerCase());
        }

        if (isKidnapped) {
            World verdantWorld = plugin.getDimensionManager().getVerdantWorld();
            if (verdantWorld == null) {
                plugin.getDimensionManager().initialiseDimension();
                verdantWorld = plugin.getDimensionManager().getVerdantWorld();
            }

            if (verdantWorld != null) {
                Location spawnLoc = verdantWorld.getSpawnLocation().clone();
                spawnLoc.setY(57);
                event.setRespawnLocation(spawnLoc);
            }

            final World finalWorld = verdantWorld;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    if (finalWorld != null && !plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
                        plugin.getDimensionManager().teleportToVerdantUtopia(player);
                    }
                    plugin.getAchievementManager().recordKidnap(player);
                    performKidnapEffects(player);
                }
            }, 20L);
        }
    }

    // ─── Kidnap Sequences ─────────────────────────────────────────────────────

    private void performKidnap(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 6, false, false));
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

        player.sendMessage("");
        player.sendMessage("§8§m                                                  ");
        player.sendMessage("§2§l  ☘ GREEN PARTY DIMENSION ☘");
        player.sendMessage("§7  You have been §2§lconscripted§r§7 into the Green Council.");
        player.sendMessage("§7  Resistance is not only futile — it's §cbad for the planet§7.");
        player.sendMessage("§8§m                                                  ");
        player.sendMessage("");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getDimensionManager().teleportToVerdantUtopia(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) performKidnapEffects(player);
                }, 5L);
            }
        }, 40L);
    }

    private void performKidnapEffects(Player player) {
        int msgIndex = random.nextInt(KIDNAP_MESSAGES.length);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        player.sendTitle(KIDNAP_MESSAGES[msgIndex], KIDNAP_SUBTITLES[msgIndex % KIDNAP_SUBTITLES.length], 10, 80, 30);

        player.sendMessage("");
        player.sendMessage("§2§l[Green Council]§r §7Welcome, §a" + player.getName() + "§7.");
        player.sendMessage("§7You have been added to §2§lMotion 48a: Involuntary Membership Drive§r§7.");
        player.sendMessage("§7Your vote on carbon credits is required. There are §c47 agenda items§7.");
        player.sendMessage("§7Snacks are provided. §8(Vegan only. Obviously.)");
        player.sendMessage("");

        plugin.getAchievementManager().recordCouncilMeeting(player);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.3f), 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.6f), 15L);

        player.spawnParticle(Particle.COMPOSTER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.1);

        plugin.getQuestManager().assignQuests(player);
    }

    // ─── World Change ─────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World toWorld = player.getWorld();
        World fromWorld = event.getFrom();

        if (plugin.getDimensionManager().isVerdantWorld(toWorld)) {
            handleEnterDimension(player);
        } else if (plugin.getDimensionManager().isVerdantWorld(fromWorld)) {
            handleLeaveDimension(player);
        }
    }

    private void handleEnterDimension(Player player) {
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            boolean hasNpcs = world.getEntitiesByClass(Villager.class).stream()
                .anyMatch(e -> e.hasMetadata("greenparty_npc"));
            if (!hasNpcs) {
                plugin.getNpcManager().spawnCouncilNpcs(world);
                plugin.getLogger().info("Spawned Green Council NPCs for player " + player.getName());
            }

            // Spawn recycling bins (once per server session)
            if (!binsSpawned) {
                plugin.getRecyclingManager().spawnBins(world);
                binsSpawned = true;
            }
        }

        plugin.getQuestManager().assignQuests(player);
        plugin.getAchievementManager().recordCouncilMeeting(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
                String tip = GREEN_TIPS[random.nextInt(GREEN_TIPS.length)];
                player.sendMessage(tip);
            }
        }, 100L);
    }

    private void handleLeaveDimension(Player player) {
        String leaveMsg = plugin.getConfig().getString("messages.leave",
            "§2You have left the Verdant Utopia. §7The dimension misses you already.");
        player.sendMessage(leaveMsg);
    }

    // ─── Block Events ─────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) return;

        Material type = event.getBlock().getType();
        String typeName = type.name();

        // ─── Quest tracking (legacy system) ───────────────────────────────────
        if (typeName.contains("COAL_ORE")) {
            plugin.getQuestManager().recordProgress(player, "MINE_COAL", 1);
        }
        if (type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.COARSE_DIRT) {
            plugin.getQuestManager().recordProgress(player, "DIRT", 1);
        }

        // ─── Quest chain tracking (Phase 3) ───────────────────────────────────
        if (typeName.contains("COAL_ORE")) {
            plugin.getQuestChainManager().recordProgress(player, "MINE_COAL", 1);
        }
        if (type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.COARSE_DIRT) {
            plugin.getQuestChainManager().recordProgress(player, "DIRT_BREAK", 1);
            plugin.getQuestChainManager().recordProgress(player, "DIRT_COLLECT", 1);
        }
        if (typeName.contains("LEAVES")) {
            plugin.getQuestChainManager().recordProgress(player, "LEAVES_BREAK", 1);
        }

        // ─── Mining reaction messages ──────────────────────────────────────────
        if (MINING_REACTIONS.containsKey(typeName) && canMessage(player)) {
            player.sendMessage(MINING_REACTIONS.get(typeName));
        }

        // ─── VIOLATION CHECK ──────────────────────────────────────────────────
        plugin.getViolationManager().checkBlockBreak(player, type);

        // ─── Eco-Score ────────────────────────────────────────────────────────
        plugin.getEcoScoreManager().onBlockBreak(player, type);

        // ─── Heckler (Phase 3) ────────────────────────────────────────────────
        plugin.getHecklerManager().onBlockBreak(player, type);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) return;

        Material type = event.getBlockPlaced().getType();
        String typeName = type.name();

        if (typeName.contains("SAPLING")) {
            plugin.getQuestManager().recordProgress(player, "SAPLING_PLACE", 1);
            plugin.getQuestChainManager().recordProgress(player, "SAPLING_PLACE", 1);
            plugin.getAchievementManager().incrementTreesPlanted(player);
            // Phase 4: leaderboard + cosmetics
            plugin.getLeaderboardManager().onTreePlanted(player);
            int treeTotal = plugin.getAchievementManager().getTreesPlanted(player);
            plugin.getCosmeticsManager().onTreePlanted(player, treeTotal);
            // Planet vitality boost
            plugin.getEnvironmentEffects().increasePlanetVitality(0.005);
            if (canMessage(player)) {
                player.sendMessage("§2[Green Council] §7A sapling planted! §8The arboreal committee has noted this positively.");
            }
            // Heckler praise for planting
            plugin.getHecklerManager().onGoodBlockPlace(player, type);
        }

        if (type == Material.NOTE_BLOCK) {
            plugin.getQuestManager().recordProgress(player, "NOTE_BLOCK", 1);
            plugin.getQuestChainManager().recordProgress(player, "NOTE_BLOCK", 1);
            plugin.getQuestChainManager().recordProgress(player, "NOTE_BLOCK_PLACE", 1);
            if (canMessage(player)) {
                player.sendMessage("§2[Green Council] §7A wind turbine! §8(It's a note block, but the council accepts the metaphor.)");
            }
        }

        if (type == Material.GREEN_CONCRETE || type == Material.LIME_CONCRETE) {
            plugin.getQuestManager().recordProgress(player, "GREEN_CONCRETE", 1);
            plugin.getQuestChainManager().recordProgress(player, "GREEN_CONCRETE", 1);
        }

        if (type == Material.COMPOSTER) {
            plugin.getQuestChainManager().recordProgress(player, "COMPOSTER_PLACE", 1);
            plugin.getHecklerManager().onGoodBlockPlace(player, type);
            // Phase 4: progression tracking + cosmetics
            plugin.getProgressionManager().onComposterPlaced(player);
            plugin.getEnvironmentEffects().increasePlanetVitality(0.003);
        }

        if (type == Material.FLOWER_POT) {
            plugin.getQuestChainManager().recordProgress(player, "FLOWER_POT", 1);
        }

        if (typeName.contains("BANNER")) {
            plugin.getQuestChainManager().recordProgress(player, "BANNER_PLACE", 1);
        }

        if (type == Material.BOOKSHELF) {
            plugin.getQuestChainManager().recordProgress(player, "BOOKSHELF", 1);
        }

        if (type == Material.GREEN_WOOL || type == Material.GREEN_STAINED_GLASS ||
            type == Material.GREEN_CARPET || type == Material.GREEN_BANNER) {
            if (canMessage(player)) {
                player.sendMessage("§2[Green Council] §aExcellent colour choice. §7Very on-brand. The council approves.");
            }
        }

        if (type == Material.COAL_BLOCK || type == Material.COAL_ORE) {
            player.sendMessage("§c[Green Council] §7COAL?! In §2The Verdant Utopia§7?! §lUnacceptable!§r §7This has been logged.");
            plugin.getHecklerManager().hecklePlayer(player, "coal_place");
        }

        plugin.getEcoScoreManager().onBlockPlace(player, type);
    }

    // ─── Recycling: Item Throw Detection ─────────────────────────────────────

    /**
     * When a player throws (drops) an item, check if it lands near a recycling bin.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) return;

        Item droppedItem = event.getItemDrop();
        double range = plugin.getRecyclingManager().getBinPickupRange();

        // Check immediately and again in 5 ticks (to let physics settle)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!droppedItem.isValid() || droppedItem.isDead()) return;

            Entity bin = plugin.getRecyclingManager().getNearbyBin(droppedItem.getLocation(), range);
            if (bin != null) {
                plugin.getRecyclingManager().processRecycle(player, droppedItem);
            }
        }, 5L);
    }

    // ─── NPC Interaction ─────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (!plugin.getNpcManager().isGreenPartyNpc(entity)) return;

        event.setCancelled(true);

        // Use contextual dialogue (Phase 3) with fallback to random
        String dialogue = plugin.getNpcManager().getContextualDialogue(entity, player);
        String name = plugin.getNpcManager().getNpcName(entity);

        if (dialogue != null && name != null) {
            player.sendMessage("");
            player.sendMessage(name + "§7:");
            player.sendMessage("  §7\"" + dialogue + "\"");
            // Show zone info from schedule manager
            String npcNameClean = name.replaceAll("§.", "");
            String zone = plugin.getNpcScheduleManager().getNpcCurrentZone(npcNameClean);
            player.sendMessage("  §8[Currently at: " + zone + "]");
            player.sendMessage("");
        }

        // Record legacy quest progress
        plugin.getQuestManager().recordProgress(player, "SURVEY_MOB", 1);

        // Record quest chain NPC interaction progress (Phase 3)
        plugin.getQuestChainManager().recordProgress(player, "NPC_INTERACT", 1);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.2f);
    }

    // ─── Item Pickup ──────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) return;

        Material type = event.getItem().getItemStack().getType();
        int amount = event.getItem().getItemStack().getAmount();

        // Legacy quest
        if (type == Material.PAPER) {
            plugin.getQuestManager().recordProgress(player, "PAPER", amount);
        }

        // Phase 3 quest chains
        if (type == Material.PAPER) {
            plugin.getQuestChainManager().recordProgress(player, "PAPER", amount);
            plugin.getQuestChainManager().recordProgress(player, "PAPER_COLLECT", amount);
        }
        if (type == Material.BOOK || type == Material.WRITTEN_BOOK || type == Material.ENCHANTED_BOOK) {
            plugin.getQuestChainManager().recordProgress(player, "BOOK_COLLECT", amount);
        }
        if (type == Material.WRITTEN_BOOK) {
            plugin.getQuestChainManager().recordProgress(player, "BOOK_WRITE", amount);
        }
        if (type == Material.EMERALD) {
            plugin.getQuestChainManager().recordProgress(player, "EMERALD_COLLECT", amount);
        }
        if (type == Material.INK_SAC) {
            plugin.getQuestChainManager().recordProgress(player, "INK_SAC", amount);
        }
        if (type == Material.DIRT || type == Material.GRASS_BLOCK) {
            plugin.getQuestChainManager().recordProgress(player, "DIRT_COLLECT", amount);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean canMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastMessageTime.get(player.getUniqueId());
        if (last == null || now - last >= MESSAGE_COOLDOWN_MS) {
            lastMessageTime.put(player.getUniqueId(), now);
            return true;
        }
        return false;
    }
}
