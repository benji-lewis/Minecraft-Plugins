package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * HecklerManager — Real-time NPC commentary on player actions.
 *
 * The Green Council has introduced a "Passive Environmental Monitoring Programme"
 * (Motion 95d, passed 7-2, two abstentions from councillors who weren't paying attention).
 *
 * Under this programme, councillors will automatically comment on:
 * - Players breaking "bad" blocks (coal, logs, etc.) → insults
 * - Players planting trees → praise
 * - High-violation players → more frequent and more pointed commentary
 *
 * Heckling is rate-limited to once per 30 seconds per player to prevent
 * "council comment fatigue" (a real concern, according to Motion 97a).
 *
 * Version 1.3.0 — Phase 3 Heckler System
 */
public class HecklerManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // playerUUID -> last heckle timestamp (ms)
    private final Map<UUID, Long> lastHeckleTime = new HashMap<>();

    private static final long HECKLE_COOLDOWN_MS = 30_000L; // 30 seconds

    // Bad blocks that trigger insults
    private static final Set<Material> BAD_BLOCKS = EnumSet.of(
        Material.COAL_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.COAL_BLOCK,
        Material.OAK_LOG,
        Material.BIRCH_LOG,
        Material.SPRUCE_LOG,
        Material.JUNGLE_LOG,
        Material.ACACIA_LOG,
        Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG,
        Material.CHERRY_LOG,
        Material.OAK_WOOD,
        Material.BIRCH_WOOD,
        Material.SPRUCE_WOOD,
        Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD,
        Material.DARK_OAK_WOOD
    );

    // Good blocks that trigger praise
    private static final Set<Material> GOOD_PLANT_BLOCKS = EnumSet.of(
        Material.OAK_SAPLING,
        Material.BIRCH_SAPLING,
        Material.SPRUCE_SAPLING,
        Material.JUNGLE_SAPLING,
        Material.ACACIA_SAPLING,
        Material.DARK_OAK_SAPLING,
        Material.MANGROVE_PROPAGULE,
        Material.CHERRY_SAPLING,
        Material.COMPOSTER,
        Material.FLOWER_POT
    );

    // Standard insults (bad block breaks)
    private static final String[] INSULTS = {
        "§c%s: §7That's not very green of you, %s!",
        "§c%s: §7The council DISAPPROVES! This will be minuted.",
        "§c%s: §7MOTION: Ban %s immediately! §8(Motion 104g, emergency session)",
        "§c%s: §7Hmm. The Greenness Index just ticked down. I blame %s.",
        "§c%s: §7Environmental vandalism! I've already filed the paperwork, %s.",
        "§c%s: §7Every time %s does that, a sapling cries. Metaphorically. I hope.",
        "§c%s: §7I've seen coal miners with better green credentials than %s!",
        "§c%s: §7The council notes this with EXTREME displeasure, %s. You're on the list.",
        "§c%s: §7I'm not saying %s is the problem... but %s is definitely part of the problem.",
        "§c%s: §7Have you CONSIDERED the environmental impact of that? I have. It's bad.",
    };

    // Eco-specific insults for high-violation players
    private static final String[] HIGH_VIOLATION_INSULTS = {
        "§c§l%s: §r§c%s! §7Again?? The violations sub-committee has assembled JUST for you!",
        "§4%s: §7%s has shown a §lpattern§r§7 of environmental disregard. A PATTERN!",
        "§c%s: §7I have your violation dossier here, %s. It's 14 pages. Single-spaced.",
        "§4%s: §7Enough! I am personally escorting %s to a mandatory composting seminar.",
        "§c%s: §7%s's eco-score should be ashamed of itself. Does it KNOW?",
        "§c%s (absolutely furious): §7The council is considering a special session dedicated exclusively to %s's violations!",
    };

    // Standard praise (planting trees)
    private static final String[] PRAISE = {
        "§a%s: §7Excellent work for the planet, %s! The council approves!",
        "§a%s: §7The council applauds your efforts, %s! §8(officially, with a motion)",
        "§a%s: §7You're a true Green warrior, %s!",
        "§a%s: §7The Greenness Index just improved! That was you, %s. Well done.",
        "§a%s: §7At last! A player who understands the sacred duty of reforestation!",
        "§a%s: §7Every sapling is a vote for the planet! Thank you, %s!",
        "§a%s: §7%s is exhibiting exemplary environmental behaviour! Motion to commend: filed!",
        "§a%s: §7The trees FEEL this, %s. They feel it in their rings. This is not a scientific claim.",
        "§a%s: §7MAGNIFICENT work, %s! I'm recommending you for the Green Excellence Badge.",
        "§a%s: §7I almost cried watching you plant that, %s. Almost. It was a professional almost.",
    };

    // Context-aware praise for high eco-score players
    private static final String[] HIGH_ECO_PRAISE = {
        "§a§l%s: §r§a%s §7has exceeded the Green Standard! A commendation is being drafted!",
        "§a%s: §7I've never seen eco-score numbers like this. %s is basically the dimension now.",
        "§a%s (weeping slightly): §7%s. Thank you. On behalf of every composter in this dimension.",
    };

    public HecklerManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Public Trigger Methods ───────────────────────────────────────────────

    /**
     * Call this when a player breaks a block in the Verdant Utopia.
     */
    public void onBlockBreak(Player player, Material material) {
        if (!isEnabled()) return;
        if (!isInVerdantWorld(player)) return;
        if (!BAD_BLOCKS.contains(material)) return;
        if (isOnCooldown(player)) return;

        int violations = plugin.getViolationManager().getViolations(player);
        boolean isRepeatOffender = violations >= 5;

        String[] pool = isRepeatOffender ? HIGH_VIOLATION_INSULTS : INSULTS;

        // High-violation players: doubled heckle chance (chance applies once)
        int chance = isRepeatOffender ? 2 : 1; // 100% for repeat offenders with bad block
        if (chance < 2 && random.nextBoolean()) return; // 50% chance for normal players

        String councillorName = getRandomNpcName();
        String msg = String.format(
            pool[random.nextInt(pool.length)],
            councillorName, player.getName(), player.getName()
        );

        // Broadcast to all players in dimension
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            world.getPlayers().forEach(p -> p.sendMessage(msg));
        }

        markHeckled(player);
    }

    /**
     * Call this when a player places a sapling/composter/etc in the Verdant Utopia.
     */
    public void onGoodBlockPlace(Player player, Material material) {
        if (!isEnabled()) return;
        if (!isInVerdantWorld(player)) return;
        if (!GOOD_PLANT_BLOCKS.contains(material)) return;
        if (isOnCooldown(player)) return;

        // 60% chance to praise on good blocks (not every single sapling)
        if (random.nextInt(10) >= 6) return;

        int ecoScore = plugin.getEcoScoreManager().getEcoScore(player);
        boolean isChampion = ecoScore >= 100;

        String[] pool = isChampion ? HIGH_ECO_PRAISE : PRAISE;
        String councillorName = getRandomNpcName();
        String msg = String.format(
            pool[random.nextInt(pool.length)],
            councillorName, player.getName(), player.getName()
        );

        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            world.getPlayers().forEach(p -> p.sendMessage(msg));
        }

        markHeckled(player);
    }

    /**
     * General-purpose heckle for custom triggers (e.g., placing TNT, using lava).
     */
    public void hecklePlayer(Player player, String reason) {
        if (!isEnabled()) return;
        if (!isInVerdantWorld(player)) return;
        if (isOnCooldown(player)) return;

        String councillorName = getRandomNpcName();
        String msg = String.format(INSULTS[random.nextInt(INSULTS.length)],
            councillorName, player.getName(), player.getName());

        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            world.getPlayers().forEach(p -> p.sendMessage(msg));
        }

        markHeckled(player);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isOnCooldown(Player player) {
        long last = lastHeckleTime.getOrDefault(player.getUniqueId(), 0L);
        return (System.currentTimeMillis() - last) < HECKLE_COOLDOWN_MS;
    }

    private void markHeckled(Player player) {
        lastHeckleTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isInVerdantWorld(Player player) {
        World verdant = plugin.getDimensionManager().getVerdantWorld();
        return verdant != null && verdant.equals(player.getWorld());
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("heckler.enabled", true);
    }

    private String getRandomNpcName() {
        // Try to get a real NPC name from the world
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            List<Entity> npcs = new ArrayList<>();
            for (Entity e : world.getEntities()) {
                if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                    npcs.add(e);
                }
            }
            if (!npcs.isEmpty()) {
                Entity npc = npcs.get(random.nextInt(npcs.size()));
                String raw = npc.getCustomName();
                if (raw != null) return raw.replaceAll("§.", "");
            }
        }

        // Fallback names
        String[] fallback = {
            "Councillor Wheatgrass", "Recycling Evangelist Bramble",
            "Elder Composting Sage Fern", "Protest Coordinator Meadow",
            "Environmental Auditor Clover", "Sustainability Guru Moss"
        };
        return fallback[random.nextInt(fallback.length)];
    }

    public static Set<Material> getBadBlocks() {
        return Collections.unmodifiableSet(BAD_BLOCKS);
    }

    public static Set<Material> getGoodPlantBlocks() {
        return Collections.unmodifiableSet(GOOD_PLANT_BLOCKS);
    }
}
