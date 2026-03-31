package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.world.VerdantChunkGenerator;

import java.io.File;

/**
 * Manages "The Verdant Utopia" - a custom dimension so overwhelmingly green
 * that coal ore is replaced with emerald and the sky is permanently #00FF00.
 *
 * Note: The Green Party has passed a resolution declaring the Nether "environmentally
 * unacceptable" and it will not be included in this plugin.
 */
public class DimensionManager {

    private final GreenPartyPlugin plugin;
    private World verdantWorld;
    private static final String WORLD_NAME = "the_verdant_utopia";

    // Surface Y level from VerdantChunkGenerator — grass at Y=55, so safe spawn is Y=57
    private static final int SAFE_SPAWN_Y = 57;

    public DimensionManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialiseDimension() {
        plugin.getLogger().info("Initialising The Verdant Utopia...");
        plugin.getLogger().info("(The planning committee approved this after only 47 meetings)");

        // Check if world already exists (loaded or on disk)
        verdantWorld = Bukkit.getWorld(WORLD_NAME);
        if (verdantWorld != null) {
            plugin.getLogger().info("The Verdant Utopia already exists. It is very green. Very.");
            applyGreenSettings(verdantWorld);
            return;
        }

        // Check if the world folder exists on disk — load it properly
        File worldFolder = new File(Bukkit.getWorldContainer(), WORLD_NAME);
        if (worldFolder.exists()) {
            plugin.getLogger().info("Found existing Verdant Utopia folder on disk — loading it...");
        }

        // Create or load the world
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generator(new VerdantChunkGenerator());
        creator.generateStructures(false); // No structures — they require planning permission

        verdantWorld = creator.createWorld();

        if (verdantWorld != null) {
            applyGreenSettings(verdantWorld);
            // Set a safe spawn location right on the surface
            Location spawn = verdantWorld.getSpawnLocation();
            spawn.setY(SAFE_SPAWN_Y);
            verdantWorld.setSpawnLocation(spawn.getBlockX(), SAFE_SPAWN_Y, spawn.getBlockZ());
            plugin.getLogger().info("The Verdant Utopia has been created!");
            plugin.getLogger().info("Spawn location set to: " + spawn.getBlockX() + ", " + SAFE_SPAWN_Y + ", " + spawn.getBlockZ());
            plugin.getLogger().info("It is extremely green. Possibly too green. We're not sorry.");
        } else {
            plugin.getLogger().severe("Failed to create The Verdant Utopia!");
            plugin.getLogger().severe("The Green Party is filing a formal complaint with Mojang.");
        }
    }

    private void applyGreenSettings(World world) {
        // Maximum greenness settings
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(6000); // Permanent noon — maximum solar energy
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(false); // No storms — we believe in climate stability (ironic)
        world.setThundering(false);
        world.setDifficulty(Difficulty.PEACEFUL); // Peace and love, no monsters
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false); // Controlled environment
        world.setGameRule(GameRule.KEEP_INVENTORY, true); // We protect what matters
        world.setGameRule(GameRule.DO_FIRE_TICK, false); // No fire (bad for environment)
        world.setGameRule(GameRule.MOB_GRIEFING, false); // No griefing allowed, creepers
        world.setGameRule(GameRule.FALL_DAMAGE, false); // Safe dimension — we've passed H&S regs
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true); // Celebrate all green achievements
        world.setSpawnFlags(false, false); // No hostile or ambient spawning
    }

    public World getVerdantWorld() {
        // Re-check Bukkit registry in case the world was loaded after init
        if (verdantWorld == null) {
            verdantWorld = Bukkit.getWorld(WORLD_NAME);
        }
        return verdantWorld;
    }

    public boolean isVerdantWorld(World world) {
        return world != null && world.getName().equals(WORLD_NAME);
    }

    public void teleportToVerdantUtopia(Player player) {
        World world = getVerdantWorld();
        if (world == null) {
            plugin.getLogger().warning("Verdant Utopia not available — attempting re-initialisation for " + player.getName());
            initialiseDimension();
            world = getVerdantWorld();
        }

        if (world == null) {
            player.sendMessage("§cThe Verdant Utopia is temporarily unavailable.");
            player.sendMessage("§7(The dimension is in planning meetings. There are 47 agenda items.)");
            return;
        }

        // Use the world's spawn location; Y was set to SAFE_SPAWN_Y during init
        Location spawnLoc = world.getSpawnLocation().clone();

        // Always use the safe surface Y — avoids getHighestBlockYAt chunk-loading issues
        // If world spawn Y isn't set correctly, use the known constant
        if (spawnLoc.getY() < 50) {
            spawnLoc.setY(SAFE_SPAWN_Y);
        }

        plugin.getLogger().info("Teleporting " + player.getName() + " to Verdant Utopia at "
            + spawnLoc.getBlockX() + ", " + (int)spawnLoc.getY() + ", " + spawnLoc.getBlockZ());

        player.teleport(spawnLoc);
        displayWelcomeMessage(player);
    }

    public void teleportToOverworld(Player player) {
        World overworld = Bukkit.getWorlds().get(0);
        if (overworld == null) {
            player.sendMessage("§cCould not find the overworld. Has it been composted?");
            return;
        }

        Location spawnLoc = overworld.getSpawnLocation();
        player.teleport(spawnLoc);
        player.sendMessage(plugin.getConfig().getString("messages.leave",
            "§2You have left the Verdant Utopia. §7It files a formal objection."));
    }

    private void displayWelcomeMessage(Player player) {
        String welcome = plugin.getConfig().getString("messages.welcome",
            "§2§lWelcome to the Verdant Utopia!");

        // Handle multiline
        for (String line : welcome.split("\n")) {
            player.sendMessage(line);
        }

        // Title screen for extra drama
        player.sendTitle(
            "§2§l🌿 The Verdant Utopia 🌿",
            "§aWhere everything is green and the points are carbon credits",
            10, 70, 20
        );

        // Sound effect — because the environment has a soundtrack
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f), 10L);
    }

    public String getWorldName() {
        return WORLD_NAME;
    }
}
