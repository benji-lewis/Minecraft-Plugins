package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * RecyclingManager — because waste is a choice, and the Verdant Utopia has CHOSEN.
 *
 * Players throw items near a Recycling Bin (glowing armour stand), and in return
 * receive a randomised "recycled" item from the weighted drop table.
 *
 * Each successful recycle earns 5 Green Credits (configurable).
 * Tracks recycling toward the "Green Tycoon" achievement.
 *
 * The council calls this "circular economy infrastructure." We call it an armour stand
 * with a nice name. The effect is the same.
 *
 * Recycling bins are placed automatically throughout the dimension at startup,
 * or can be located via config.yml.
 */
public class RecyclingManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Active recycling bin UUIDs (for quick lookup)
    private final Set<UUID> recyclingBins = new HashSet<>();

    // Metadata key for identifying bins
    public static final String BIN_META_KEY = "greenparty_recycling_bin";

    // Config
    private int creditsPerRecycle;
    private List<RecycleEntry> dropTable;

    // Bin positions relative to spawn (or absolute if configured)
    private static final int[][] DEFAULT_BIN_OFFSETS = {
        {0, 0}, {15, 10}, {-15, 10}, {10, -15}, {-10, -15},
        {25, 0}, {-25, 0}, {0, 25}, {0, -25}, {20, 20}
    };

    public RecyclingManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        creditsPerRecycle = plugin.getConfig().getInt("recycling.credits-per-recycle", 5);

        dropTable = new ArrayList<>();

        // Default weighted table (loaded from config if present, else hardcoded defaults)
        List<Map<?, ?>> configTable = plugin.getConfig().getMapList("recycling.drop-table");

        if (configTable != null && !configTable.isEmpty()) {
            for (Map<?, ?> entry : configTable) {
                String materialName = (String) entry.get("material");
                int weight = entry.containsKey("weight") ? ((Number) entry.get("weight")).intValue() : 10;
                int amount = entry.containsKey("amount") ? ((Number) entry.get("amount")).intValue() : 1;
                try {
                    Material mat = Material.valueOf(materialName.toUpperCase());
                    dropTable.add(new RecycleEntry(mat, weight, amount, null));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Recycling] Unknown material in drop table: " + materialName);
                }
            }
        }

        // Fallback defaults if table is empty
        if (dropTable.isEmpty()) {
            buildDefaultDropTable();
        }
    }

    private void buildDefaultDropTable() {
        // Dyes (common)
        dropTable.add(new RecycleEntry(Material.GREEN_DYE,        30, 2, null));
        dropTable.add(new RecycleEntry(Material.LIME_DYE,         25, 2, null));
        dropTable.add(new RecycleEntry(Material.YELLOW_DYE,       20, 2, null));
        dropTable.add(new RecycleEntry(Material.BLUE_DYE,         20, 2, null));
        dropTable.add(new RecycleEntry(Material.WHITE_DYE,        15, 3, null));
        dropTable.add(new RecycleEntry(Material.CYAN_DYE,         15, 2, null));
        dropTable.add(new RecycleEntry(Material.PURPLE_DYE,       10, 2, null));
        dropTable.add(new RecycleEntry(Material.RED_DYE,          10, 2, null));
        // Seeds (uncommon)
        dropTable.add(new RecycleEntry(Material.WHEAT_SEEDS,      20, 4, null));
        dropTable.add(new RecycleEntry(Material.MELON_SEEDS,      15, 3, null));
        dropTable.add(new RecycleEntry(Material.PUMPKIN_SEEDS,    15, 3, null));
        dropTable.add(new RecycleEntry(Material.BEETROOT_SEEDS,   12, 3, null));
        dropTable.add(new RecycleEntry(Material.OAK_SAPLING,      10, 1, null));
        dropTable.add(new RecycleEntry(Material.BIRCH_SAPLING,    10, 1, null));
        // Minerals (rare)
        dropTable.add(new RecycleEntry(Material.COAL,              8, 2, null));
        dropTable.add(new RecycleEntry(Material.IRON_INGOT,        6, 1, null));
        dropTable.add(new RecycleEntry(Material.GOLD_INGOT,        4, 1, null));
        dropTable.add(new RecycleEntry(Material.EMERALD,           3, 1, null));
        dropTable.add(new RecycleEntry(Material.DIAMOND,           1, 1, null));
        // Special: custom named items (very rare)
        dropTable.add(new RecycleEntry(Material.PAPER,             5, 1, "§aGreen Party Manifesto (Recycled Edition)"));
        dropTable.add(new RecycleEntry(Material.GOLD_NUGGET,       3, 1, "§6Eco Badge"));
        dropTable.add(new RecycleEntry(Material.EMERALD,           2, 1, "§2Recycling Champion Medal"));
        dropTable.add(new RecycleEntry(Material.COMPARATOR,        1, 1, "§bCouncil Approval Certificate"));
    }

    // ─── Ground Detection ─────────────────────────────────────────────────────

    /** Fallback Y if no solid ground is found (matches dimension floor). */
    private static final int BIN_GROUND_Y_FALLBACK = 57;

    /**
     * Returns the Y of the topmost solid block at (x, z).
     * Skips air, water, lava, flowers, grass, and any other non-solid block so
     * that bins land on actual grass/dirt rather than floating above vegetation.
     */
    private int getSolidGroundY(World world, int x, int z) {
        int startY = Math.min(world.getMaxHeight() - 1, 130);
        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) continue;
            if (type == Material.WATER || type == Material.LAVA) continue;
            if (!type.isSolid()) continue; // catches flowers, SHORT_GRASS, TALL_GRASS, ferns, etc.
            return y; // topmost solid block — bin sits on top (y+1)
        }
        plugin.getLogger().warning("[Recycling] Could not find solid ground at " + x + ", " + z
            + " — using fallback Y=" + BIN_GROUND_Y_FALLBACK);
        return BIN_GROUND_Y_FALLBACK;
    }

    // ─── Bin Spawning ─────────────────────────────────────────────────────────

    /**
     * Place recycling bins throughout the dimension.
     * Called once the world is ready (from PlayerListener.onPlayerChangedWorld, first entry).
     */
    public void spawnBins(World world) {
        if (!recyclingBins.isEmpty()) return; // Already spawned

        // Remove any existing bins from a previous session (armor stands tagged with our meta)
        for (Entity e : world.getEntitiesByClass(ArmorStand.class)) {
            if (e.hasMetadata(BIN_META_KEY)) {
                recyclingBins.add(e.getUniqueId());
                plugin.getLogger().info("[Recycling] Found existing bin at " + e.getLocation().toVector());
            }
        }

        if (!recyclingBins.isEmpty()) {
            plugin.getLogger().info("[Recycling] Loaded " + recyclingBins.size() + " existing recycling bins.");
            return;
        }

        // Spawn bins at configured or default locations
        Location spawnLoc = world.getSpawnLocation().clone();

        // Check for configured locations first
        List<?> configLocations = plugin.getConfig().getList("recycling.bin-locations");
        if (configLocations != null && !configLocations.isEmpty()) {
            int i = 0;
            for (Object obj : configLocations) {
                if (obj instanceof Map<?, ?> locMap) {
                    try {
                        int x = ((Number) locMap.get("x")).intValue();
                        int z = ((Number) locMap.get("z")).intValue();
                        // Use provided Y if explicitly set, otherwise detect solid ground
                        int y = locMap.containsKey("y")
                            ? ((Number) locMap.get("y")).intValue()
                            : getSolidGroundY(world, x, z) + 1;
                        spawnBin(new Location(world, x, y, z));
                        i++;
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Recycling] Invalid bin location in config: " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info("[Recycling] Spawned " + i + " configured recycling bins.");
        } else {
            // Auto-generate around spawn using solid-ground detection
            int binsToSpawn = plugin.getConfig().getInt("recycling.auto-bin-count", 7);
            int spawnedCount = 0;
            for (int i = 0; i < Math.min(binsToSpawn, DEFAULT_BIN_OFFSETS.length); i++) {
                int bx = spawnLoc.getBlockX() + DEFAULT_BIN_OFFSETS[i][0];
                int bz = spawnLoc.getBlockZ() + DEFAULT_BIN_OFFSETS[i][1];
                int by = getSolidGroundY(world, bx, bz) + 1; // +1 so bin stands ON the ground block
                spawnBin(new Location(world, bx, by, bz));
                spawnedCount++;
            }
            plugin.getLogger().info("[Recycling] Auto-spawned " + spawnedCount + " recycling bins around spawn.");
        }
    }

    private void spawnBin(Location loc) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setCustomName("§a♻ §2§lRecycling Bin §a♻");
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setVisible(true);
        stand.setSmall(false);
        stand.setGlowing(true);

        // Mark as recycling bin
        stand.setMetadata(BIN_META_KEY, new FixedMetadataValue(plugin, true));
        recyclingBins.add(stand.getUniqueId());

        plugin.getLogger().info("[Recycling] Spawned bin at " + loc.toVector());
    }

    // ─── Recycling Logic ──────────────────────────────────────────────────────

    /**
     * Returns true if the given entity is a recycling bin.
     */
    public boolean isRecyclingBin(Entity entity) {
        return entity instanceof ArmorStand && entity.hasMetadata(BIN_META_KEY);
    }

    /**
     * Returns true if any recycling bin is within RANGE blocks of the given location.
     * Returns the bin entity if found, null otherwise.
     */
    public Entity getNearbyBin(Location loc, double range) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (isRecyclingBin(e)) return e;
        }
        return null;
    }

    /**
     * Process a player throwing an item near a recycling bin.
     * The thrown item is consumed; the player receives a random recycled item.
     */
    public void processRecycle(Player player, Item thrownItem) {
        // Remove the thrown item
        thrownItem.remove();

        // Pick a random recycled item
        ItemStack reward = pickReward();

        if (reward == null) {
            player.sendMessage("§7[Recycling] §cThe recycling bin jammed. Please try again.");
            return;
        }

        // Drop the item at the bin location (theatrics)
        Location binLoc = thrownItem.getLocation();
        binLoc.getWorld().dropItemNaturally(binLoc, reward);

        // Award credits
        plugin.getGreenCreditManager().addCredits(player, creditsPerRecycle,
            "recycling " + thrownItem.getItemStack().getType().name().toLowerCase().replace('_', ' '));

        // Notify player
        String rewardName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
            ? reward.getItemMeta().getDisplayName()
            : reward.getType().name().toLowerCase().replace('_', ' ');

        player.sendMessage("§2§l[♻ Recycling] §r§7You recycled §a"
            + thrownItem.getItemStack().getType().name().toLowerCase().replace('_', ' ')
            + " §7→ §a" + rewardName
            + " §7(+" + creditsPerRecycle + " GC)");

        // Actionbar
        try {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§2♻ Recycled! +" + creditsPerRecycle + " GC"));
        } catch (Exception ignored) {}

        // Particles
        binLoc.getWorld().spawnParticle(Particle.COMPOSTER, binLoc.clone().add(0, 1, 0),
            20, 0.3, 0.5, 0.3, 0.05);
        binLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, binLoc.clone().add(0, 1.5, 0),
            10, 0.3, 0.3, 0.3, 0.05);

        // Sound
        player.playSound(binLoc, Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1.0f, 1.2f);

        // Track toward "Green Tycoon" achievement via credit check
        plugin.getAchievementManager().checkGreenCredits(player,
            plugin.getGreenCreditManager().getBalance(player));

        // Increment recycled counter toward Green Tycoon
        plugin.getAchievementManager().incrementCounter(player, "ITEMS_RECYCLED", 1);

        plugin.getLogger().info("[Recycling] " + player.getName() + " recycled item for " + rewardName);
    }

    // ─── Weighted Drop Table ──────────────────────────────────────────────────

    private ItemStack pickReward() {
        if (dropTable.isEmpty()) return new ItemStack(Material.GREEN_DYE, 1);

        int totalWeight = dropTable.stream().mapToInt(e -> e.weight).sum();
        int roll = random.nextInt(totalWeight);
        int running = 0;

        for (RecycleEntry entry : dropTable) {
            running += entry.weight;
            if (roll < running) {
                ItemStack item = new ItemStack(entry.material, entry.amount);
                if (entry.customName != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(entry.customName);
                        item.setItemMeta(meta);
                    }
                }
                return item;
            }
        }

        return new ItemStack(Material.GREEN_DYE, 1); // Fallback
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    public static class RecycleEntry {
        public final Material material;
        public final int weight;
        public final int amount;
        public final String customName; // null = no custom name

        public RecycleEntry(Material material, int weight, int amount, String customName) {
            this.material = material;
            this.weight = weight;
            this.amount = amount;
            this.customName = customName;
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public int getCreditsPerRecycle() {
        return creditsPerRecycle;
    }

    public Set<UUID> getBinUUIDs() {
        return Collections.unmodifiableSet(recyclingBins);
    }

    public double getBinPickupRange() {
        return plugin.getConfig().getDouble("recycling.bin-pickup-range", 3.0);
    }
}
