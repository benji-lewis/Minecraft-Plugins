package uk.greenparty.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * EcoScoreManager — tracks how environmentally virtuous (or terrible) each player is.
 *
 * The Green Council has determined that all actions must be scored.
 * Plant a tree: hero. Mine coal: villain. The algorithm is unambiguous.
 * (The algorithm was written in 20 minutes. But it cares deeply about the planet.)
 */
public class EcoScoreManager {

    private final GreenPartyPlugin plugin;
    private final Map<UUID, Integer> ecoScores = new HashMap<>();

    private File dataFile;
    private YamlConfiguration dataConfig;

    // Points awarded/deducted per action (can be overridden by config)
    private int pointsPlantTree;
    private int pointsComposter;
    private int pointsLeafBreak;
    private int pointsCoalMine;
    private int pointsNatureDestroy;

    // Block sets for nature-friendly / harmful detection
    private final Set<String> harmfulBlocks = new HashSet<>();
    private final Set<String> ecoFriendlyBlocks = new HashSet<>();

    public EcoScoreManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        startActionbarTask();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        pointsPlantTree    = plugin.getConfig().getInt("eco-score.points.plant-tree",     5);
        pointsComposter    = plugin.getConfig().getInt("eco-score.points.place-composter", 3);
        pointsLeafBreak    = plugin.getConfig().getInt("eco-score.points.break-leaves",    2);
        pointsCoalMine     = plugin.getConfig().getInt("eco-score.points.mine-coal",      -10);
        pointsNatureDestroy= plugin.getConfig().getInt("eco-score.points.destroy-nature", -5);

        List<String> harmful = plugin.getConfig().getStringList("eco-score.harmful-blocks");
        harmfulBlocks.addAll(harmful.isEmpty() ? defaultHarmfulBlocks() : harmful);

        List<String> eco = plugin.getConfig().getStringList("eco-score.eco-friendly-blocks");
        ecoFriendlyBlocks.addAll(eco.isEmpty() ? defaultEcoFriendlyBlocks() : eco);
    }

    private List<String> defaultHarmfulBlocks() {
        return Arrays.asList(
            "OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG",
            "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "BAMBOO_BLOCK",
            "OAK_LEAVES", "BIRCH_LEAVES", "SPRUCE_LEAVES", "JUNGLE_LEAVES",
            "ACACIA_LEAVES", "DARK_OAK_LEAVES", "MANGROVE_LEAVES", "CHERRY_LEAVES"
        );
    }

    private List<String> defaultEcoFriendlyBlocks() {
        return Arrays.asList(
            "OAK_SAPLING", "BIRCH_SAPLING", "SPRUCE_SAPLING", "JUNGLE_SAPLING",
            "ACACIA_SAPLING", "DARK_OAK_SAPLING", "MANGROVE_PROPAGULE", "CHERRY_SAPLING",
            "COMPOSTER", "BEE_NEST", "BEEHIVE", "MOSS_BLOCK", "MOSS_CARPET"
        );
    }

    // ─── Persistence ────────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata/eco-scores.yml");
        dataFile.getParentFile().mkdirs();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ecoScores.put(uuid, dataConfig.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {}
        }

        plugin.getLogger().info("[EcoScore] Loaded eco-scores for " + ecoScores.size() + " players.");
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!ecoScores.containsKey(uuid)) {
            int saved = dataConfig.getInt(uuid.toString(), 0);
            ecoScores.put(uuid, saved);
        }
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        dataConfig.set(uuid.toString(), ecoScores.getOrDefault(uuid, 0));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[EcoScore] Failed to save score for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Integer> entry : ecoScores.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[EcoScore] Failed to save all eco-scores: " + e.getMessage());
        }
    }

    // ─── Score API ───────────────────────────────────────────────────────────────

    public int getEcoScore(Player player) {
        return ecoScores.getOrDefault(player.getUniqueId(), 0);
    }

    public void addScore(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int newScore = ecoScores.getOrDefault(uuid, 0) + amount;
        ecoScores.put(uuid, newScore);

        // Forward to achievement system
        plugin.getAchievementManager().checkTreesPlanted(player);
    }

    // ─── Event Hooks ─────────────────────────────────────────────────────────────

    /**
     * Call this when a player places a block. Awards eco-score based on block type.
     */
    public void onBlockPlace(Player player, Material material) {
        String name = material.name();

        // Planting a sapling → tree planting score
        if (name.contains("SAPLING") || name.equals("MANGROVE_PROPAGULE") || name.equals("BAMBOO")) {
            addScore(player, pointsPlantTree);
            plugin.getAchievementManager().incrementTreesPlanted(player);
        }

        // Placing a composter
        else if (material == Material.COMPOSTER) {
            addScore(player, pointsComposter);
        }
    }

    /**
     * Call this when a player breaks a block. Awards or deducts eco-score.
     */
    public void onBlockBreak(Player player, Material material) {
        String name = material.name();

        // Mining coal → environmental crime
        if (name.contains("COAL_ORE")) {
            addScore(player, pointsCoalMine);
        }

        // Breaking leaves (gently, presumably) → small bonus
        else if (name.contains("_LEAVES")) {
            addScore(player, pointsLeafBreak);
        }

        // Breaking wood or nature blocks → deduction
        else if (harmfulBlocks.contains(name) && !name.contains("_LEAVES")) {
            addScore(player, pointsNatureDestroy);
        }
    }

    // ─── Actionbar Display ───────────────────────────────────────────────────────

    private void startActionbarTask() {
        // Every 200 ticks = 10 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendEcoScoreBar(player);
            }
        }, 200L, 200L);
    }

    private void sendEcoScoreBar(Player player) {
        int score = getEcoScore(player);
        String rating = getEcoRating(score);

        Component bar = net.kyori.adventure.text.Component.text()
            .append(net.kyori.adventure.text.Component.text("🌿 Eco-Score: ").color(NamedTextColor.DARK_GREEN))
            .append(net.kyori.adventure.text.Component.text(score)
                .color(score >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
            .append(net.kyori.adventure.text.Component.text("  " + rating).color(NamedTextColor.GRAY))
            .build();

        player.sendActionBar(bar);
    }

    private String getEcoRating(int score) {
        if (score >= 200) return "★ Verdant Champion";
        if (score >= 100) return "★ Eco Warrior";
        if (score >= 50)  return "★ Green Advocate";
        if (score >= 10)  return "★ Environmentally Aware";
        if (score >= 0)   return "★ Neutral (try harder)";
        if (score >= -20) return "☠ Environmental Menace";
        return "☠ Planet Destroyer";
    }
}
