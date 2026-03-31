package uk.greenparty.managers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * CosmeticsManager — Phase 4 player personalisation system.
 *
 * Earn cosmetics through achievements. Wear them with pride.
 * The council has approved all cosmetics. (It took 3 meetings.)
 *
 * Types:
 * - ARMOR_COLOR — dyed leather armour sets
 * - PARTICLE_TRAIL — follows player when moving
 * - CHAT_TITLE — prefix in chat
 */
public class CosmeticsManager {

    private final GreenPartyPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File dataFile;

    // ─── Cosmetic Definitions ─────────────────────────────────────────────────

    public enum CosmeticType { ARMOR_COLOR, PARTICLE_TRAIL, CHAT_TITLE }

    public static class CosmeticDefinition {
        public final String id;
        public final String displayName;
        public final CosmeticType type;
        public final String unlockDescription;
        public final String data; // color hex for armor, particle name for trails, prefix string for titles

        public CosmeticDefinition(String id, String displayName, CosmeticType type,
                                  String unlockDescription, String data) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.unlockDescription = unlockDescription;
            this.data = data;
        }
    }

    private static final List<CosmeticDefinition> ALL_COSMETICS = new ArrayList<>();

    static {
        // Armor colors (hex colors)
        ALL_COSMETICS.add(new CosmeticDefinition("verdant_armor", "§aVerdant Armour",
            CosmeticType.ARMOR_COLOR,
            "Earn 50 Eco-Score", "#1DB954"));
        ALL_COSMETICS.add(new CosmeticDefinition("emerald_armor", "§2Emerald Armour",
            CosmeticType.ARMOR_COLOR,
            "Earn 500 Green Credits", "#00AA00"));
        ALL_COSMETICS.add(new CosmeticDefinition("moss_armor", "§2Mossy Armour",
            CosmeticType.ARMOR_COLOR,
            "Plant 20 trees", "#4A7C59"));
        ALL_COSMETICS.add(new CosmeticDefinition("council_armor", "§6Council Armour",
            CosmeticType.ARMOR_COLOR,
            "Complete 5 quest chains", "#C8A832"));

        // Particle trails
        ALL_COSMETICS.add(new CosmeticDefinition("green_dust", "§aGreen Dust Trail",
            CosmeticType.PARTICLE_TRAIL,
            "Earn 100 Eco-Score", "REDSTONE:0,170,0"));
        ALL_COSMETICS.add(new CosmeticDefinition("leaf_trail", "§2Leaf Trail",
            CosmeticType.PARTICLE_TRAIL,
            "Plant 50 trees", "FALLING_DUST:GRASS_BLOCK"));
        ALL_COSMETICS.add(new CosmeticDefinition("composter_trail", "§aComposter Trail",
            CosmeticType.PARTICLE_TRAIL,
            "Place 10 composters", "COMPOSTER_FILL_ATTEMPT"));
        ALL_COSMETICS.add(new CosmeticDefinition("sparkle_trail", "§eEmerald Sparkle Trail",
            CosmeticType.PARTICLE_TRAIL,
            "Earn 1000 Green Credits", "CRIT:MAGIC"));
        ALL_COSMETICS.add(new CosmeticDefinition("villager_trail", "§aHappy Villager Trail",
            CosmeticType.PARTICLE_TRAIL,
            "Complete 10 quests", "VILLAGER_HAPPY"));

        // Chat titles
        ALL_COSMETICS.add(new CosmeticDefinition("green_champion", "§2[Green Champion]",
            CosmeticType.CHAT_TITLE,
            "Win monthly leaderboard #1", "§2[Green Champion] "));
        ALL_COSMETICS.add(new CosmeticDefinition("eco_advocate", "§a[Eco Advocate]",
            CosmeticType.CHAT_TITLE,
            "Win monthly leaderboard #2", "§a[Eco Advocate] "));
        ALL_COSMETICS.add(new CosmeticDefinition("green_supporter", "§2[Green Supporter]",
            CosmeticType.CHAT_TITLE,
            "Win monthly leaderboard #3", "§2[Green Supporter] "));
        ALL_COSMETICS.add(new CosmeticDefinition("eco_warrior", "§a[Eco Warrior]",
            CosmeticType.CHAT_TITLE,
            "Reach 200 Eco-Score", "§a[Eco Warrior] "));
        ALL_COSMETICS.add(new CosmeticDefinition("composter", "§8[Master Composter]",
            CosmeticType.CHAT_TITLE,
            "Place 25 composters", "§8[Master Composter] "));
        ALL_COSMETICS.add(new CosmeticDefinition("councillor", "§6[Councillor]",
            CosmeticType.CHAT_TITLE,
            "Awarded by admins", "§6[Councillor] "));
        ALL_COSMETICS.add(new CosmeticDefinition("rebel", "§c[Environmental Rebel]",
            CosmeticType.CHAT_TITLE,
            "Accumulate 50 violations", "§c[Environmental Rebel] "));
    }

    // ─── Player Data ──────────────────────────────────────────────────────────

    public static class PlayerCosmeticData {
        public Set<String> unlocked = new HashSet<>();
        public String equippedTitle = "";
        public String equippedTrail = "";
        public String equippedArmor = "";
    }

    private final Map<UUID, PlayerCosmeticData> playerData = new HashMap<>();
    private final Map<UUID, BukkitTask> trailTasks = new HashMap<>();

    private int trailIntervalTicks;

    public CosmeticsManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    private void loadConfig() {
        trailIntervalTicks = plugin.getConfig().getInt("cosmetics.particle-trail-interval-ticks", 2);
    }

    // ─── API ─────────────────────────────────────────────────────────────────

    public void grantCosmetic(Player player, String cosmeticId) {
        CosmeticDefinition def = getDefinition(cosmeticId);
        if (def == null) return;

        PlayerCosmeticData data = getData(player);
        if (data.unlocked.contains(cosmeticId)) return;

        data.unlocked.add(cosmeticId);
        saveData();

        player.sendMessage("§2§l[Cosmetic Unlocked!] §r§a" + def.displayName);
        player.sendMessage("§7Use §a/cosmetics equip " + cosmeticId + "§7 to apply it.");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
    }

    public void grantChatTitle(Player player, String titleName) {
        // Find matching chat title cosmetic
        for (CosmeticDefinition def : ALL_COSMETICS) {
            if (def.type == CosmeticType.CHAT_TITLE &&
                def.displayName.toLowerCase().contains(titleName.toLowerCase())) {
                grantCosmetic(player, def.id);
                return;
            }
        }
        // Try by id
        grantCosmetic(player, titleName.toLowerCase().replace(" ", "_"));
    }

    public void equipCosmetic(Player player, String cosmeticId) {
        CosmeticDefinition def = getDefinition(cosmeticId);
        if (def == null) {
            player.sendMessage("§cUnknown cosmetic: §7" + cosmeticId);
            return;
        }

        PlayerCosmeticData data = getData(player);
        if (!data.unlocked.contains(cosmeticId)) {
            player.sendMessage("§cYou haven't unlocked §7" + def.displayName + "§c yet.");
            player.sendMessage("§7Unlock requirement: §f" + def.unlockDescription);
            return;
        }

        switch (def.type) {
            case ARMOR_COLOR -> {
                data.equippedArmor = cosmeticId;
                applyArmorColor(player, def.data);
                player.sendMessage("§2[Cosmetics] §aArmour colour applied: " + def.displayName);
            }
            case PARTICLE_TRAIL -> {
                data.equippedTrail = cosmeticId;
                startTrail(player, def.data);
                player.sendMessage("§2[Cosmetics] §aParticle trail equipped: " + def.displayName);
            }
            case CHAT_TITLE -> {
                data.equippedTitle = cosmeticId;
                player.sendMessage("§2[Cosmetics] §aChat title equipped: " + def.displayName);
            }
        }
        saveData();
    }

    public void unequipCosmetic(Player player, String cosmeticId) {
        CosmeticDefinition def = getDefinition(cosmeticId);
        if (def == null) {
            player.sendMessage("§cUnknown cosmetic: §7" + cosmeticId);
            return;
        }

        PlayerCosmeticData data = getData(player);
        switch (def.type) {
            case ARMOR_COLOR -> {
                data.equippedArmor = "";
                player.sendMessage("§2[Cosmetics] §7Armour colour removed.");
            }
            case PARTICLE_TRAIL -> {
                data.equippedTrail = "";
                stopTrail(player);
                player.sendMessage("§2[Cosmetics] §7Particle trail removed.");
            }
            case CHAT_TITLE -> {
                data.equippedTitle = "";
                player.sendMessage("§2[Cosmetics] §7Chat title removed.");
            }
        }
        saveData();
    }

    public void listCosmetics(Player player) {
        PlayerCosmeticData data = getData(player);
        player.sendMessage("§2§l===== YOUR COSMETICS =====");

        CosmeticType[] types = CosmeticType.values();
        String[] typeLabels = {"§6⚔ Armour Colours", "§b✨ Particle Trails", "§a💬 Chat Titles"};

        for (int t = 0; t < types.length; t++) {
            CosmeticType type = types[t];
            player.sendMessage(typeLabels[t]);
            for (CosmeticDefinition def : ALL_COSMETICS) {
                if (def.type != type) continue;
                boolean unlocked = data.unlocked.contains(def.id);
                boolean equipped = def.id.equals(data.equippedArmor)
                    || def.id.equals(data.equippedTrail)
                    || def.id.equals(data.equippedTitle);

                String status = equipped ? "§a[EQUIPPED]" : unlocked ? "§7[UNLOCKED]" : "§8[LOCKED]";
                player.sendMessage("  " + status + " " + def.displayName
                    + (unlocked ? "" : " §8— " + def.unlockDescription));
            }
        }
        player.sendMessage("§7Use §a/cosmetics equip <id>§7 or §a/cosmetics unequip <id>");
        player.sendMessage("§2§l==========================");
    }

    // ─── Armor Color ─────────────────────────────────────────────────────────

    private void applyArmorColor(Player player, String hexColor) {
        Color color = hexToColor(hexColor);
        Material[] parts = {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
        String[] names = {"Green Helmet", "Green Chestplate", "Green Leggings", "Green Boots"};

        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack item = new ItemStack(parts[i]);
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            meta.setDisplayName("§a" + names[i]);
            item.setItemMeta(meta);
            armor[i] = item;
        }

        player.getInventory().setHelmet(armor[0]);
        player.getInventory().setChestplate(armor[1]);
        player.getInventory().setLeggings(armor[2]);
        player.getInventory().setBoots(armor[3]);
    }

    private Color hexToColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return Color.GREEN;
        }
    }

    // ─── Particle Trails ──────────────────────────────────────────────────────

    private void startTrail(Player player, String trailData) {
        stopTrail(player); // cancel existing

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopTrail(player);
                return;
            }
            // Only spawn particles when player moves (check if in dimension or anywhere)
            spawnTrailParticle(player, trailData);
        }, 0L, trailIntervalTicks);

        trailTasks.put(player.getUniqueId(), task);
    }

    private void spawnTrailParticle(Player player, String trailData) {
        org.bukkit.Location loc = player.getLocation();
        try {
            if (trailData.startsWith("REDSTONE:")) {
                String[] parts = trailData.substring(9).split(",");
                int r = Integer.parseInt(parts[0]);
                int g = Integer.parseInt(parts[1]);
                int b = Integer.parseInt(parts[2]);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(r, g, b), 1.0f);
                player.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.2, 0, 0.2, 0, dust);
            } else if (trailData.startsWith("FALLING_DUST:")) {
                Material mat = Material.valueOf(trailData.substring(13));
                player.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 2, 0.3, 0, 0.3, 0, mat.createBlockData());
            } else if (trailData.equals("COMPOSTER_FILL_ATTEMPT")) {
                player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 4, 0.3, 0, 0.3, 0);
            } else if (trailData.equals("CRIT:MAGIC")) {
                player.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.2, 0, 0.2, 0);
            } else if (trailData.equals("VILLAGER_HAPPY")) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.2, 0, 0.2, 0);
            } else {
                // Generic particle by name
                Particle p = Particle.valueOf(trailData);
                player.getWorld().spawnParticle(p, loc, 2, 0.2, 0, 0.2, 0);
            }
        } catch (Exception ignored) {
            // Invalid particle — silently skip
        }
    }

    private void stopTrail(Player player) {
        BukkitTask task = trailTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void onPlayerQuit(Player player) {
        stopTrail(player);
    }

    public void onPlayerJoin(Player player) {
        PlayerCosmeticData data = getData(player);
        // Re-apply equipped cosmetics
        if (!data.equippedArmor.isEmpty()) {
            CosmeticDefinition def = getDefinition(data.equippedArmor);
            if (def != null) applyArmorColor(player, def.data);
        }
        if (!data.equippedTrail.isEmpty()) {
            CosmeticDefinition def = getDefinition(data.equippedTrail);
            if (def != null) startTrail(player, def.data);
        }
    }

    public void cancelAll() {
        for (BukkitTask task : trailTasks.values()) task.cancel();
        trailTasks.clear();
    }

    // ─── Chat Title ───────────────────────────────────────────────────────────

    public String getChatPrefix(Player player) {
        PlayerCosmeticData data = getData(player);
        if (data.equippedTitle.isEmpty()) return "";
        CosmeticDefinition def = getDefinition(data.equippedTitle);
        if (def == null) return "";
        return def.data; // the prefix string
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private PlayerCosmeticData getData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> new PlayerCosmeticData());
    }

    private CosmeticDefinition getDefinition(String id) {
        for (CosmeticDefinition def : ALL_COSMETICS) {
            if (def.id.equalsIgnoreCase(id)) return def;
        }
        return null;
    }

    public List<CosmeticDefinition> getAllCosmetics() { return Collections.unmodifiableList(ALL_COSMETICS); }

    public PlayerCosmeticData getPlayerData(Player player) { return getData(player); }

    // ─── Auto-unlock checks ───────────────────────────────────────────────────

    public void checkUnlocks(Player player) {
        EcoScoreManager esm = plugin.getEcoScoreManager();
        GreenCreditManager gcm = plugin.getGreenCreditManager();
        ViolationManager vm = plugin.getViolationManager();

        int eco = esm.getEcoScore(player);
        int gc = gcm.getBalance(player);
        int violations = vm.getViolations(player);

        if (eco >= 50) grantCosmetic(player, "verdant_armor");
        if (gc >= 500) grantCosmetic(player, "emerald_armor");
        if (eco >= 100) grantCosmetic(player, "green_dust");
        if (eco >= 200) grantCosmetic(player, "eco_warrior");
        if (violations >= 50) grantCosmetic(player, "rebel");
    }

    public void onTreePlanted(Player player, int total) {
        if (total >= 20) grantCosmetic(player, "moss_armor");
        if (total >= 50) grantCosmetic(player, "leaf_trail");
    }

    public void onComposterPlaced(Player player, int total) {
        if (total >= 10) grantCosmetic(player, "composter_trail");
        if (total >= 25) grantCosmetic(player, "composter");
    }

    public void onQuestChainCompleted(Player player, int chainsCompleted) {
        if (chainsCompleted >= 5) grantCosmetic(player, "council_armor");
        if (chainsCompleted >= 10) grantCosmetic(player, "sparkle_trail");
    }

    public void onQuestCompleted(Player player, int totalQuests) {
        if (totalQuests >= 10) grantCosmetic(player, "villager_trail");
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata/cosmetics.json");
        dataFile.getParentFile().mkdirs();

        if (!dataFile.exists()) return;

        try (Reader r = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, PlayerCosmeticData>>() {}.getType();
            Map<String, PlayerCosmeticData> raw = gson.fromJson(r, type);
            if (raw != null) {
                for (Map.Entry<String, PlayerCosmeticData> entry : raw.entrySet()) {
                    try {
                        playerData.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Cosmetics] Could not load data: " + e.getMessage());
        }

        plugin.getLogger().info("[Cosmetics] Loaded cosmetic data for " + playerData.size() + " players.");
    }

    public void saveData() {
        Map<String, PlayerCosmeticData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerCosmeticData> entry : playerData.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(raw, w);
        } catch (IOException e) {
            plugin.getLogger().warning("[Cosmetics] Could not save data: " + e.getMessage());
        }
    }
}
