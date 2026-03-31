package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * AnnouncementManager — the propaganda arm of the Verdant Utopia.
 *
 * At random intervals (configurable, default 5-10 minutes), the council
 * broadcasts an important announcement to all players in the dimension.
 *
 * These announcements are accompanied by:
 * - Title display (large text)
 * - Sound (ENTITY_VILLAGER_AMBIENT — the classic)
 * - Light show (particles and fireworks near councillor NPCs)
 *
 * The announcements are configurable in config.yml.
 * The council has many things to say. So, so many things.
 */
public class AnnouncementManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Config
    private long minIntervalTicks;
    private long maxIntervalTicks;
    private List<String> announcements;

    private BukkitTask scheduledTask;

    // Default announcements (used if config is empty)
    private static final String[] DEFAULT_ANNOUNCEMENTS = {
        "EMERGENCY: Compost shortage detected in Sector 7!",
        "Motion passed: All players must plant a tree immediately.",
        "The council is convening. Attend immediately! Snacks provided (vegan only).",
        "ALERT: Unauthorised coal block detected. Offender will be fined.",
        "Reminder: Recycling bins are available throughout the dimension. Use them!",
        "ANNOUNCEMENT: The Arboreal Committee has counted 247 new saplings. Well done.",
        "URGENT: The composting rota has not been followed. Violations to follow.",
        "The Green Party has filed its 14th environmental impact assessment. Progress!",
        "NOTICE: A motion to ban cobblestone is under consideration. Please advise.",
        "Breaking: Emerald reserves at an all-time high. The council celebrates (sustainably).",
        "COUNCIL DECREE: All players must say 'reduce, reuse, recycle' before logging off.",
        "Urgent memo from the Policy Committee: We need more tote bags. Urgently.",
        "The dimension has been certified 'Extremely Green' by the Green Rating Board (us).",
        "WARNING: Carbon footprint levels approaching acceptable thresholds. Act now!",
        "ANNOUNCEMENT: The Ceremonial Spoon has been polished. A solemn occasion.",
        "MOTION TABLED: Should all blocks be renamed? The committee will discuss.",
        "REMINDER: The Nether is banned on environmental grounds. Do not attempt access.",
        "BREAKING: The council has voted to commission another 89-page policy document.",
        "ALERT: A player has been spotted near coal. The council is dispatching inspectors.",
        "NOTICE: The Green Party has updated its manifesto. Point 15c is particularly important.",
    };

    public AnnouncementManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        scheduleNextAnnouncement();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        // Default: 5 min = 6000 ticks, 10 min = 12000 ticks
        minIntervalTicks = plugin.getConfig().getLong("announcements.min-interval-ticks", 6000L);
        maxIntervalTicks = plugin.getConfig().getLong("announcements.max-interval-ticks", 12000L);

        List<String> configList = plugin.getConfig().getStringList("announcements.list");
        if (configList != null && !configList.isEmpty()) {
            announcements = new ArrayList<>(configList);
        } else {
            announcements = Arrays.asList(DEFAULT_ANNOUNCEMENTS);
        }

        plugin.getLogger().info("[Announcements] Loaded " + announcements.size() + " announcements. "
            + "Interval: " + minIntervalTicks + "-" + maxIntervalTicks + " ticks.");
    }

    // ─── Scheduling ───────────────────────────────────────────────────────────

    private void scheduleNextAnnouncement() {
        long range = Math.max(1, maxIntervalTicks - minIntervalTicks);
        long delay = minIntervalTicks + (long)(random.nextDouble() * range);

        scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastAnnouncement();
            scheduleNextAnnouncement(); // Schedule the next one
        }, delay);
    }

    // ─── Core Broadcast ───────────────────────────────────────────────────────

    private void broadcastAnnouncement() {
        // Find players in the dimension
        List<Player> inDimension = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                inDimension.add(p);
            }
        }

        if (inDimension.isEmpty()) {
            plugin.getLogger().info("[Announcements] No players in dimension — skipping announcement.");
            return;
        }

        // Pick a random announcement
        String announcement = announcements.get(random.nextInt(announcements.size()));

        plugin.getLogger().info("[Announcements] Broadcasting: " + announcement);

        // Format title (first 40 chars, then subtitle if longer)
        String title, subtitle;
        if (announcement.length() <= 40) {
            title = "§2§l📋 COUNCIL ANNOUNCEMENT";
            subtitle = "§a" + announcement;
        } else {
            // Split at first sentence boundary or colon
            int splitAt = findSplitPoint(announcement);
            if (splitAt > 0 && splitAt < announcement.length()) {
                title = "§2§l📋 " + announcement.substring(0, splitAt).trim();
                subtitle = "§a" + announcement.substring(splitAt).trim();
            } else {
                title = "§2§l📋 COUNCIL ANNOUNCEMENT";
                subtitle = "§a" + announcement.substring(0, Math.min(50, announcement.length()));
            }
        }

        // Broadcast to all players in dimension
        for (Player p : inDimension) {
            // Title display
            p.sendTitle(title, subtitle, 10, 100, 20);

            // Chat message
            p.sendMessage("§2§l[📋 GREEN COUNCIL] §r§a" + announcement);

            // Sound
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 0.8f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.8f, 1.0f);
                }
            }, 10L);
        }

        // Light show near any councillor NPCs or near spawn
        triggerLightShow(inDimension);
    }

    /**
     * Trigger a light show — particles and fireworks near councillors / spawn.
     */
    private void triggerLightShow(List<Player> players) {
        World verdant = plugin.getDimensionManager().getVerdantWorld();
        if (verdant == null) return;

        // Find councillor NPCs
        List<Location> showLocations = new ArrayList<>();

        for (Entity e : verdant.getEntitiesByClass(Villager.class)) {
            if (e.hasMetadata("greenparty_npc")) {
                showLocations.add(e.getLocation().clone().add(0, 1, 0));
            }
        }

        // Fallback to spawn if no NPCs
        if (showLocations.isEmpty()) {
            showLocations.add(verdant.getSpawnLocation().clone().add(0, 1, 0));
        }

        // Particles at each location
        for (Location loc : showLocations) {
            verdant.spawnParticle(Particle.FIREWORK, loc, 30, 0.5, 1.0, 0.5, 0.1);
            verdant.spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5, 0.05);
            verdant.spawnParticle(Particle.COMPOSTER, loc, 15, 0.3, 0.5, 0.3, 0.05);
        }

        // Fireworks at random NPC locations
        int fireworkCount = Math.min(3, showLocations.size());
        List<Location> shuffled = new ArrayList<>(showLocations);
        Collections.shuffle(shuffled);

        for (int i = 0; i < fireworkCount; i++) {
            final Location fwLoc = shuffled.get(i);
            final int delay = i * 10;
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnAnnounceFirework(fwLoc), delay);
        }

        // Ambient sound at spawn area
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            verdant.playSound(verdant.getSpawnLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
            verdant.playSound(verdant.getSpawnLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        }, 5L);
    }

    private void spawnAnnounceFirework(Location loc) {
        try {
            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withColor(Color.LIME)
                .withFade(Color.YELLOW)
                .with(org.bukkit.FireworkEffect.Type.STAR)
                .trail(true)
                .flicker(false)
                .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("[Announcements] Could not spawn firework: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Find a natural split point in an announcement string for title/subtitle display.
     */
    private int findSplitPoint(String text) {
        // Try colon first (e.g. "EMERGENCY: Something happened")
        int colon = text.indexOf(':');
        if (colon > 3 && colon < 35) return colon + 1;

        // Try period
        int period = text.indexOf('.');
        if (period > 3 && period < 35) return period + 1;

        // Try space at ~35 chars
        int space = text.lastIndexOf(' ', 35);
        if (space > 5) return space;

        return Math.min(35, text.length());
    }

    // ─── Manual Trigger ───────────────────────────────────────────────────────

    /**
     * Immediately trigger an announcement (admin/testing use).
     */
    public void triggerNow() {
        broadcastAnnouncement();
    }

    /**
     * Add a custom announcement to the live list.
     */
    public void addAnnouncement(String text) {
        announcements.add(text);
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    public void cancelAll() {
        if (scheduledTask != null) scheduledTask.cancel();
    }
}
