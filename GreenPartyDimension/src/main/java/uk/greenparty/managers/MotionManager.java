package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * MotionManager — the democratic heart of the Verdant Utopia.
 *
 * Any councillor (player with greenparty.council permission) may raise a Motion for a vote.
 * All players in the dimension may cast a vote (yes/no) for a configurable duration.
 * If the motion passes, dramatic consequences are applied to the dimension.
 *
 * The council takes these votes extremely seriously.
 * The outcomes, less so.
 *
 * Tracks vote participation toward the "Dedicated Attendee" achievement.
 */
public class MotionManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Current active motion (only one at a time — the council lacks the bandwidth for parallel democracy)
    private ActiveMotion currentMotion = null;

    // Config
    private long voteDurationTicks;

    public MotionManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // ─── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        // Default: 2 minutes = 2400 ticks
        voteDurationTicks = plugin.getConfig().getLong("motions.vote-duration-ticks", 2400L);
    }

    // ─── Motion API ───────────────────────────────────────────────────────────

    public boolean hasActiveMotion() {
        return currentMotion != null;
    }

    public ActiveMotion getCurrentMotion() {
        return currentMotion;
    }

    /**
     * Start a new motion. Returns false if a motion is already active.
     */
    public boolean startMotion(Player initiator, String title) {
        if (currentMotion != null) {
            return false;
        }

        currentMotion = new ActiveMotion(title, initiator.getUniqueId(), initiator.getName());

        // Broadcast to all players in dimension
        String timeStr = formatDuration(voteDurationTicks);
        broadcastToDimension("§2§l[MOTION RAISED] §r§7\"§a" + title + "§7\"");
        broadcastToDimension("§7Motion raised by §a" + initiator.getName() + "§7. Vote with §a/motion vote yes§7 or §a/motion vote no§7.");
        broadcastToDimension("§7Voting closes in §a" + timeStr + "§7. The council awaits your democratic input.");

        // Title to all dimension players
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                p.sendTitle("§2§l📋 MOTION RAISED", "§a\"" + title + "\"", 10, 80, 20);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
        }

        // Schedule end of vote
        BukkitTask endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> resolveMotion(), voteDurationTicks);
        currentMotion.setEndTask(endTask);

        plugin.getLogger().info("[Motions] Motion started: \"" + title + "\" by " + initiator.getName()
            + " (duration: " + voteDurationTicks + " ticks)");
        return true;
    }

    /**
     * Cast a vote. Returns VoteResult indicating outcome.
     */
    public VoteResult castVote(Player player, boolean voteYes) {
        if (currentMotion == null) {
            return VoteResult.NO_MOTION;
        }

        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            return VoteResult.NOT_IN_DIMENSION;
        }

        UUID uuid = player.getUniqueId();
        if (currentMotion.hasVoted(uuid)) {
            return VoteResult.ALREADY_VOTED;
        }

        currentMotion.addVote(uuid, voteYes);

        // Award credits and track achievement
        plugin.getGreenCreditManager().rewardVoting(player);
        plugin.getAchievementManager().recordCouncilMeeting(player);

        return voteYes ? VoteResult.VOTED_YES : VoteResult.VOTED_NO;
    }

    /**
     * Resolve the current motion.
     */
    private void resolveMotion() {
        if (currentMotion == null) return;

        ActiveMotion motion = currentMotion;
        currentMotion = null;

        int yes = motion.getYesVotes();
        int no = motion.getNoVotes();
        int total = yes + no;

        boolean passed = yes > no;

        // Announce results
        broadcastToDimension("§2§l[MOTION CLOSED] §r§7\"§a" + motion.getTitle() + "§7\"");
        broadcastToDimension("§7Results: §a" + yes + " YES §7/ §c" + no + " NO §7(§7Total: §a" + total + " votes§7)");

        if (total == 0) {
            broadcastToDimension("§7§o(No one voted. The council is disappointed. But not surprised.)");
        } else if (passed) {
            broadcastToDimension("§a§l✔ MOTION PASSED! §r§7\"" + motion.getTitle() + "§7\"");
            applyMotionEffect(motion.getTitle(), true);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                    p.sendTitle("§a§l✔ PASSED", "§a\"" + motion.getTitle() + "\"", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        } else {
            broadcastToDimension("§c§l✗ MOTION FAILED. §r§7\"" + motion.getTitle() + "§7\"");
            broadcastToDimension("§7§o(The council records this failure with a mixture of disappointment and a new motion.)");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                    p.sendTitle("§c§l✗ FAILED", "§c\"" + motion.getTitle() + "\"", 10, 60, 20);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
            }
        }

        plugin.getLogger().info("[Motions] Motion \"" + motion.getTitle() + "\" resolved: "
            + (passed ? "PASSED" : "FAILED") + " (" + yes + " yes, " + no + " no)");
    }

    /**
     * Apply dimension-wide effect based on motion title keywords.
     */
    private void applyMotionEffect(String title, boolean passed) {
        if (!passed) return;

        String titleLower = title.toLowerCase();
        World verdant = plugin.getDimensionManager().getVerdantWorld();
        if (verdant == null) return;

        List<Player> inDimension = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                inDimension.add(p);
            }
        }

        if (titleLower.contains("dark") || titleLower.contains("darkness") || titleLower.contains("shadow")) {
            // "Ban all dark blocks" — apply Darkness effect to all players for 30s
            broadcastToDimension("§8§l[MOTION EFFECT] §rDimension entering DARKNESS protocol for 30 seconds...");
            for (Player p : inDimension) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DARKNESS, 600, 0, false, false));
                p.sendMessage("§8[Motion] §7Darkness descends! The council has spoken.");
            }

        } else if (titleLower.contains("tree") || titleLower.contains("plant") || titleLower.contains("sapling")) {
            // "Mandatory tree planting" — spawn saplings randomly near players
            broadcastToDimension("§2§l[MOTION EFFECT] §rMandatory tree planting enacted! Saplings raining down...");
            for (Player p : inDimension) {
                Location loc = p.getLocation();
                for (int i = 0; i < 5; i++) {
                    int dx = random.nextInt(10) - 5;
                    int dz = random.nextInt(10) - 5;
                    Location drop = new Location(verdant, loc.getBlockX() + dx, loc.getBlockY() + 2, loc.getBlockZ() + dz);
                    verdant.dropItemNaturally(drop, new org.bukkit.inventory.ItemStack(Material.OAK_SAPLING, 1));
                }
                p.sendMessage("§2[Motion] §7Saplings dispatched to your vicinity. Plant them immediately.");
            }

        } else if (titleLower.contains("compost") || titleLower.contains("festival") || titleLower.contains("waste")) {
            // "Compost festival" — particles rain down, compost event
            broadcastToDimension("§2§l[MOTION EFFECT] §rCOMPOST FESTIVAL enacted! Particles incoming!");
            for (Player p : inDimension) {
                Bukkit.getScheduler().runTaskTimer(plugin, t -> {
                    if (!p.isOnline()) { t.cancel(); return; }
                    Location loc = p.getLocation().add(0, 3, 0);
                    verdant.spawnParticle(Particle.COMPOSTER, loc, 30, 2.0, 1.0, 2.0, 0.05);
                    verdant.spawnParticle(Particle.HAPPY_VILLAGER, loc.subtract(0, 1, 0), 15, 1.5, 1.0, 1.5, 0.02);
                    p.playSound(p.getLocation(), Sound.BLOCK_COMPOSTER_FILL, 0.5f, 1.2f);
                }, 0L, 40L);
                // Cancel after 30 seconds (600 ticks / 40 ticks per trigger = 15 triggers)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) p.sendMessage("§2[Compost Festival] §7The festival has concluded. The council hopes you enjoyed the particles.");
                }, 600L);
            }

        } else if (titleLower.contains("ban") || titleLower.contains("coal") || titleLower.contains("fossil")) {
            // Coal ban reinforced — darkness briefly
            broadcastToDimension("§c§l[MOTION EFFECT] §rCOAL BAN ENFORCED! Darkness descends upon polluters!");
            for (Player p : inDimension) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 100, 0, false, false));
                p.sendMessage("§c[Motion] §7The coal ban has been reinforced. Avoid all coal. Forever.");
            }

        } else if (titleLower.contains("recycle") || titleLower.contains("reuse") || titleLower.contains("resource")) {
            // Recycling motion — bonus items
            broadcastToDimension("§2§l[MOTION EFFECT] §rRecycling drive! Green Credits bonus active!");
            for (Player p : inDimension) {
                plugin.getGreenCreditManager().addCredits(p, 10, "recycling motion bonus");
                p.sendMessage("§2[Motion] §7+10 Green Credits for supporting the recycling motion!");
            }

        } else {
            // Generic passed motion — fireworks and cheer
            broadcastToDimension("§2§l[MOTION EFFECT] §rThe council celebrates this historic vote!");
            for (Player p : inDimension) {
                spawnCelebrationFirework(p.getLocation());
                verdant.spawnParticle(Particle.HAPPY_VILLAGER,
                    p.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    private void spawnCelebrationFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
            .withColor(Color.GREEN)
            .withColor(Color.LIME)
            .withFade(Color.WHITE)
            .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void broadcastToDimension(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getDimensionManager().isVerdantWorld(p.getWorld())) {
                p.sendMessage(message);
            }
        }
    }

    private String formatDuration(long ticks) {
        long seconds = ticks / 20;
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }

    // ─── Inner Types ──────────────────────────────────────────────────────────

    public enum VoteResult {
        NO_MOTION,
        NOT_IN_DIMENSION,
        ALREADY_VOTED,
        VOTED_YES,
        VOTED_NO
    }

    public static class ActiveMotion {
        private final String title;
        private final UUID initiatorUUID;
        private final String initiatorName;
        private final Map<UUID, Boolean> votes = new LinkedHashMap<>();
        private BukkitTask endTask;

        public ActiveMotion(String title, UUID initiatorUUID, String initiatorName) {
            this.title = title;
            this.initiatorUUID = initiatorUUID;
            this.initiatorName = initiatorName;
        }

        public String getTitle() { return title; }
        public String getInitiatorName() { return initiatorName; }

        public boolean hasVoted(UUID uuid) { return votes.containsKey(uuid); }

        public void addVote(UUID uuid, boolean yes) { votes.put(uuid, yes); }

        public int getYesVotes() { return (int) votes.values().stream().filter(v -> v).count(); }
        public int getNoVotes() { return (int) votes.values().stream().filter(v -> !v).count(); }
        public int getTotalVotes() { return votes.size(); }

        public void setEndTask(BukkitTask task) { this.endTask = task; }
        public void cancelTask() { if (endTask != null) endTask.cancel(); }
    }

    public void cancelAll() {
        if (currentMotion != null) currentMotion.cancelTask();
    }
}
