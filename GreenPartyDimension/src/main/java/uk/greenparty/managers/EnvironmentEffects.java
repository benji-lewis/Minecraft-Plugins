package uk.greenparty.managers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * EnvironmentEffects — Phase 4 ambient dimension effects.
 *
 * Because a true Verdant Utopia should FEEL alive.
 * Leaf particles fall. Composters hiss. Lights shimmer.
 * The planet breathes. Probably.
 *
 * All effects configurable via config.yml.
 */
public class EnvironmentEffects {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Config toggles
    private boolean leafParticlesEnabled;
    private boolean composterSoundsEnabled;
    private boolean lightShowEnabled;
    private boolean timeDilationEnabled;
    private boolean weatherEffectsEnabled;
    private boolean bossBarEnabled;

    // Config frequencies
    private int leafParticleTicks;
    private int composterSoundTicks;
    private int lightShowTicks;
    private int weatherTicks;
    private double timeDilationRate;

    // Tasks
    private final List<BukkitTask> tasks = new ArrayList<>();

    // Boss bar (planet vitality)
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private double planetVitality = 0.5; // 0.0 to 1.0

    // Structure light show locations (populated from StructureManager)
    private final List<Location> lightShowLocations = new ArrayList<>();

    public EnvironmentEffects(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    private void loadConfig() {
        var cfg = plugin.getConfig();

        leafParticlesEnabled  = cfg.getBoolean("environment-effects.leaf-particles.enabled", true);
        composterSoundsEnabled = cfg.getBoolean("environment-effects.composter-sounds.enabled", true);
        lightShowEnabled      = cfg.getBoolean("environment-effects.light-show.enabled", true);
        timeDilationEnabled   = cfg.getBoolean("environment-effects.time-dilation.enabled", false);
        weatherEffectsEnabled = cfg.getBoolean("environment-effects.weather-effects.enabled", true);
        bossBarEnabled        = cfg.getBoolean("environment-effects.boss-bar.enabled", true);

        leafParticleTicks     = cfg.getInt("environment-effects.leaf-particles.interval-ticks", 40);
        composterSoundTicks   = cfg.getInt("environment-effects.composter-sounds.interval-ticks", 80);
        lightShowTicks        = cfg.getInt("environment-effects.light-show.interval-ticks", 60);
        weatherTicks          = cfg.getInt("environment-effects.weather-effects.interval-ticks", 100);
        timeDilationRate      = cfg.getDouble("environment-effects.time-dilation.rate", 2.0);
    }

    // ─── Start ────────────────────────────────────────────────────────────────

    public void startAll() {
        plugin.getLogger().info("[Effects] Starting environmental effects...");

        if (leafParticlesEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickLeafParticles, 20L, leafParticleTicks));
            plugin.getLogger().info("[Effects] Leaf particles: ON (every " + leafParticleTicks + " ticks)");
        }

        if (composterSoundsEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickComposterSounds, 40L, composterSoundTicks));
            plugin.getLogger().info("[Effects] Composter sounds: ON");
        }

        if (lightShowEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickLightShow, 60L, lightShowTicks));
            plugin.getLogger().info("[Effects] Light show: ON");
        }

        if (timeDilationEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickTimeDilation, 1L, 1L));
            plugin.getLogger().info("[Effects] Time dilation: ON (x" + timeDilationRate + ")");
        }

        if (weatherEffectsEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickWeatherEffects, 100L, weatherTicks));
            plugin.getLogger().info("[Effects] Weather effects: ON");
        }

        if (bossBarEnabled) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                this::tickBossBar, 200L, 200L));
            plugin.getLogger().info("[Effects] Planet Vitality boss bar: ON");
        }

        plugin.getLogger().info("[Effects] All configured effects started.");
    }

    public void stopAll() {
        for (BukkitTask task : tasks) task.cancel();
        tasks.clear();
        // Remove boss bars
        for (Map.Entry<UUID, BossBar> entry : playerBossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                p.hideBossBar(entry.getValue());
            }
        }
        playerBossBars.clear();
    }

    // ─── Leaf Particles ───────────────────────────────────────────────────────

    private void tickLeafParticles() {
        World world = getVerdantWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            // Spawn falling leaves in a radius around the player
            for (int i = 0; i < 5; i++) {
                double ox = (random.nextDouble() - 0.5) * 20;
                double oz = (random.nextDouble() - 0.5) * 20;
                double oy = random.nextDouble() * 8 + 4;
                Location particleLoc = loc.clone().add(ox, oy, oz);
                world.spawnParticle(Particle.FALLING_DUST, particleLoc, 1, 0, 0, 0, 0,
                    Material.GRASS_BLOCK.createBlockData());
            }
        }
    }

    // ─── Composter Sounds ─────────────────────────────────────────────────────

    private void tickComposterSounds() {
        World world = getVerdantWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            if (random.nextInt(3) == 0) {
                Location loc = player.getLocation().clone().add(
                    (random.nextDouble() - 0.5) * 15,
                    0,
                    (random.nextDouble() - 0.5) * 15);
                world.playSound(loc, Sound.BLOCK_COMPOSTER_FILL, 0.3f, 0.8f + random.nextFloat() * 0.4f);
            }
        }
    }

    // ─── Light Show ───────────────────────────────────────────────────────────

    public void addLightShowLocation(Location loc) {
        lightShowLocations.add(loc.clone());
    }

    private void tickLightShow() {
        World world = getVerdantWorld();
        if (world == null) return;

        // Bonfire/particle effect at each registered structure location
        for (Location base : lightShowLocations) {
            if (!base.getWorld().equals(world)) continue;

            // Ring of particles around the location
            double radius = 3.0;
            int points = 12;
            double angleStep = (2 * Math.PI) / points;
            long tick = world.getGameTime();

            for (int i = 0; i < points; i++) {
                double angle = angleStep * i + (tick * 0.05);
                double x = base.getX() + radius * Math.cos(angle);
                double z = base.getZ() + radius * Math.sin(angle);
                double y = base.getY() + 1 + Math.sin(tick * 0.1 + i) * 0.5;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 200, 50), 1.0f));
            }

            // Occasional sparkle burst
            if (random.nextInt(10) == 0) {
                world.spawnParticle(Particle.FIREWORK, base.clone().add(0, 2, 0), 10, 1, 1, 1, 0.05);
            }
        }

        // Also emit from all online players' feet occasionally
        for (Player player : world.getPlayers()) {
            if (random.nextInt(20) == 0) {
                world.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 3, 0.5, 0.5, 0.5);
            }
        }
    }

    // ─── Time Dilation ────────────────────────────────────────────────────────

    private long lastTimeCheck = -1;

    private void tickTimeDilation() {
        World world = getVerdantWorld();
        if (world == null) return;

        if (!world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)) return;

        long currentTime = world.getTime();
        if (lastTimeCheck >= 0) {
            long expected = lastTimeCheck + 1;
            long extra = (long) (timeDilationRate - 1.0);
            if (extra > 0) {
                world.setTime(currentTime + extra);
            }
        }
        lastTimeCheck = world.getTime();
    }

    // ─── Weather Effects ──────────────────────────────────────────────────────

    private void tickWeatherEffects() {
        World world = getVerdantWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();

            // Aurora borealis: coloured particles high above
            for (int i = 0; i < 8; i++) {
                double ox = (random.nextDouble() - 0.5) * 30;
                double oz = (random.nextDouble() - 0.5) * 30;
                double oy = 15 + random.nextDouble() * 5;
                Location aurora = loc.clone().add(ox, oy, oz);

                // Cycle through green/cyan/white
                Color[] auroraColors = {
                    Color.fromRGB(0, 255, 0),
                    Color.fromRGB(0, 200, 150),
                    Color.fromRGB(100, 255, 200),
                    Color.fromRGB(200, 255, 150)
                };
                Color c = auroraColors[random.nextInt(auroraColors.length)];
                world.spawnParticle(Particle.DUST, aurora, 1, 0.5, 0.1, 0.5, 0,
                    new Particle.DustOptions(c, 2.0f));
            }

            // Occasional green sparkle "rain"
            if (random.nextInt(4) == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (random.nextDouble() - 0.5) * 10;
                    double oz = (random.nextDouble() - 0.5) * 10;
                    Location rainLoc = loc.clone().add(ox, 6 + random.nextDouble() * 3, oz);
                    Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(0, 200, 50), 0.8f);
                    world.spawnParticle(Particle.DUST, rainLoc, 1, 0, -0.2, 0, 0, greenDust);
                }
            }
        }
    }

    // ─── Boss Bar (Planet Vitality) ───────────────────────────────────────────

    private void tickBossBar() {
        World world = getVerdantWorld();
        if (world == null) return;

        // Slowly increase vitality over time (green actions increase it faster)
        planetVitality = Math.min(1.0, planetVitality + 0.002);

        BossBar.Color barColor = planetVitality > 0.7 ? BossBar.Color.GREEN
            : planetVitality > 0.4 ? BossBar.Color.YELLOW : BossBar.Color.RED;

        String title = String.format("§2🌍 Planet Vitality: §a%.0f%%", planetVitality * 100);
        Component titleComp = Component.text(title).color(NamedTextColor.GREEN);

        for (Player player : world.getPlayers()) {
            UUID uuid = player.getUniqueId();
            BossBar bar = playerBossBars.computeIfAbsent(uuid, k ->
                BossBar.bossBar(titleComp, (float) planetVitality, barColor, BossBar.Overlay.PROGRESS));

            bar.name(titleComp);
            bar.progress((float) Math.max(0.0, Math.min(1.0, planetVitality)));
            bar.color(barColor);
            player.showBossBar(bar);
        }

        // Remove bars from players who left the dimension
        for (UUID uuid : new HashSet<>(playerBossBars.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || !world.equals(p.getWorld())) {
                if (p != null && p.isOnline()) p.hideBossBar(playerBossBars.get(uuid));
                playerBossBars.remove(uuid);
            }
        }
    }

    public void increasePlanetVitality(double amount) {
        planetVitality = Math.min(1.0, planetVitality + amount);
    }

    public void decreasePlanetVitality(double amount) {
        planetVitality = Math.max(0.0, planetVitality - amount);
    }

    public double getPlanetVitality() { return planetVitality; }

    public void onPlayerJoin(Player player) {
        // Bossbar will be auto-shown on next tick if they're in the dimension
    }

    public void onPlayerQuit(Player player) {
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private World getVerdantWorld() {
        return plugin.getDimensionManager().getVerdantWorld();
    }
}
