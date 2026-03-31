package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * DebateManager — The Green Council's premier political theatre.
 *
 * Every 30 minutes, two councillors square off near spawn and argue about
 * increasingly absurd green policy proposals. Other NPCs teleport to watch.
 * Players can vote to support a side and win (or lose) Green Credits.
 *
 * The debates are entirely non-binding. The council has never actually
 * changed policy based on one. They have filed motions about the debate format.
 *
 * Version 1.4.4 — Pre-debate announcement phase + configurable vote window
 */
public class DebateManager {

    private final GreenPartyPlugin plugin;
    private final Random random = new Random();

    // Current debate state
    private boolean debateActive = false;
    private boolean preDebateCountdown = false; // true during 2-min announcement window
    private String debatantA = null;
    private String debatantB = null;
    private String debateTitle = null;
    private int dialogueLine = 0;
    private final Map<UUID, String> playerVotes = new HashMap<>();
    private BukkitTask debateScheduleTask;
    private BukkitTask dialogueTask;
    private BukkitTask announcementTask;
    private BukkitTask countdownTask;

    private static final int DEBATE_INTERVAL_TICKS = 20 * 60 * 30; // 30 minutes
    private static final int ANNOUNCEMENT_OFFSET_TICKS = 20 * 60 * 2; // 2 min before

    // ─── Debate Definitions ───────────────────────────────────────────────────

    private record DebateTopic(String title, String[] dialogue) {}

    private static final DebateTopic[] DEBATE_TOPICS = {
        new DebateTopic(
            "MOTION 88c: Should All Blocks Be Renamed To Sound More Eco-Friendly?",
            new String[]{
                "§2[%A] §7I propose we rename cobblestone to 'Reclaimed Urban Grey' immediately!",
                "§a[%B] §7Absolutely NOT! Cobblestone is cobblestone. You can't just rename rocks, %A!",
                "§2[%A] §7I already have! I've submitted forms to the Block Naming Committee. They exist now.",
                "§a[%B] §7You invented a committee just to rename rocks??",
                "§2[%A] §7EVERYTHING has a committee! That's the whole point of governance, %B!",
                "§a[%B] §7...Fine. But 'Reclaimed Urban Grey' takes too long to say when building.",
            }
        ),
        new DebateTopic(
            "MOTION 91a: Should The Nether Be Banned On Environmental Grounds?",
            new String[]{
                "§c[%A] §7The Nether is a CLIMATE EMERGENCY waiting to happen. It must be banned!",
                "§6[%B] §7You can't BAN a dimension, %A. It doesn't work like that.",
                "§c[%A] §7I filed the paperwork. The Nether has been formally notified.",
                "§6[%B] §7You sent a letter... to the Nether.",
                "§c[%A] §7Via certified post. It returned slightly singed. I take this as acknowledgement.",
                "§6[%B] §7That's not how legal notices work!",
            }
        ),
        new DebateTopic(
            "MOTION 99b: Mandatory Tree Hugging — 3 Times Daily",
            new String[]{
                "§2[%A] §7Trees are sentient. The evidence is clear. The committee reviewed it for 11 hours.",
                "§a[%B] §7The 'evidence' was one player who said 'the oak looked at me funny', %A.",
                "§2[%A] §7That's anecdotal AND empirical! Both kinds of evidence!",
                "§a[%B] §7Those are the same thing—no, wait, they're OPPOSITE things!",
                "§2[%A] §7I've already scheduled Mandatory Tree Appreciation Mornings at 7, 10, and 2.",
                "§a[%B] §7...I actually don't hate this. Abstaining. But I'm not hugging the birch.",
            }
        ),
        new DebateTopic(
            "MOTION 103f: All Players Must Carry A Reusable Shopping Bag At All Times",
            new String[]{
                "§e[%A] §7I move that all players must carry a tote bag. For the environment.",
                "§d[%B] §7We've spent 47 emeralds on tote bags this quarter! We can't afford more!",
                "§e[%A] §7You can't put a price on sustainability, %B.",
                "§d[%B] §7I literally can. It's 47 emeralds. I have receipts. Many receipts.",
                "§e[%A] §7Those receipts should be in tote bags! For efficiency!",
                "§d[%B] §7I'm going to scream into a composter.",
            }
        ),
        new DebateTopic(
            "MOTION 107: Should Creepers Be Given Counselling Instead Of Being Fought?",
            new String[]{
                "§2[%A] §7Creepers are misunderstood creatures in crisis. They need §ahelp§7, not swords.",
                "§c[%B] §7They explode! They have exploded on me seven times, %A!",
                "§2[%A] §7That's a cry for help, %B. Have you considered that maybe YOU exploded at them first?",
                "§c[%B] §7I was STANDING THERE! They approached ME!",
                "§2[%A] §7Boundary violations can go both ways. The Creeper Welfare Sub-Committee will review.",
                "§c[%B] §7There is a Creeper Welfare Sub-Committee??",
            }
        ),
        new DebateTopic(
            "MOTION 112: Replacing All Cobblestone Roads With Compacted Compost Paths",
            new String[]{
                "§9[%A] §7Cobblestone roads are a scar on this dimension's natural beauty. Compost paths instead!",
                "§3[%B] §7Compost paths will... decompose, %A. That's the whole point of compost.",
                "§9[%A] §7They'll decompose sustainably! Into the earth! Full circle!",
                "§3[%B] §7So we'd be rebuilding roads constantly?",
                "§9[%A] §7That creates green jobs! It's a stimulus package AND environmental policy!",
                "§3[%B] §7I... actually that's not the worst idea you've had. I still vote no.",
            }
        ),
        new DebateTopic(
            "MOTION 117: All Torches Must Be Replaced With Bioluminescent Moss Lanterns",
            new String[]{
                "§2[%A] §7Coal-powered torches are contributing to the dimension's carbon load. Replace all with moss!",
                "§a[%B] §7Bioluminescent moss lanterns aren't a thing, %A.",
                "§2[%A] §7They will be once the Research Sub-Committee approves Willow's grant proposal.",
                "§a[%B] §7Willow's last proposal involved 'emotionally charging the emerald ore'.",
                "§2[%A] §7The science is EMERGING, %B! You can't put a timetable on innovation!",
                "§a[%B] §7You put a timetable on everything. It's on the notice board. Under 'mandatory schedules'.",
            }
        ),
    };

    public DebateManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;
        scheduleNextDebate();
    }

    // ─── Config helpers ───────────────────────────────────────────────────────

    /** Returns the pre-debate announcement window in ticks (config: debates.announcement-offset-ticks, default 2400). */
    private int getAnnouncementOffsetTicks() {
        return plugin.getConfig().getInt("debates.announcement-offset-ticks", ANNOUNCEMENT_OFFSET_TICKS);
    }

    // ─── Scheduling ───────────────────────────────────────────────────────────

    private void scheduleNextDebate() {
        int intervalTicks = plugin.getConfig().getInt("debates.interval-ticks", DEBATE_INTERVAL_TICKS);
        int offsetTicks   = getAnnouncementOffsetTicks();

        // Announce 2 minutes before debate
        announcementTask = Bukkit.getScheduler().runTaskLater(plugin, this::announceDebate,
            intervalTicks - offsetTicks);
    }

    private void announceDebate() {
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null || world.getPlayers().isEmpty()) {
            // Reschedule without announcing
            Bukkit.getScheduler().runTaskLater(plugin, this::startDebate, getAnnouncementOffsetTicks());
            return;
        }

        DebateTopic topic = DEBATE_TOPICS[random.nextInt(DEBATE_TOPICS.length)];
        debateTitle = topic.title;

        List<Entity> npcs = getNpcs(world);
        if (npcs.size() >= 2) {
            Collections.shuffle(npcs);
            debatantA = stripColor(npcs.get(0).getCustomName());
            debatantB = stripColor(npcs.get(1).getCustomName());
        } else {
            debatantA = "Councillor Wheatgrass";
            debatantB = "Recycling Evangelist Bramble";
        }

        broadcastDebateAnnouncement(world, false);

        preDebateCountdown = true;

        // Start debate after the announcement window
        int offsetTicks = getAnnouncementOffsetTicks();
        Bukkit.getScheduler().runTaskLater(plugin, this::startDebate, offsetTicks);
    }

    /**
     * Broadcast the pre-debate announcement to all players in the world.
     * @param emergency true → "EMERGENCY" variant (used by /debate trigger)
     */
    private void broadcastDebateAnnouncement(World world, boolean emergency) {
        int waitMinutes = getAnnouncementOffsetTicks() / (20 * 60);

        String titleLine   = emergency ? "§c§lEMERGENCY COUNCIL DEBATE" : "§2§lCOUNCIL DEBATE";
        String headerLine  = emergency
            ? "§2§l║       EMERGENCY COUNCIL DEBATE SOON!       ║"
            : "§2§l║   COUNCIL DEBATE STARTING IN " + waitMinutes + " MINUTES!   ║";

        world.getPlayers().forEach(p -> {
            p.sendTitle(titleLine, "§e" + debateTitle.substring(0, Math.min(debateTitle.length(), 40)), 10, 60, 10);
            p.sendMessage("");
            p.sendMessage("§2§l╔═══════════════════════════════════════════╗");
            p.sendMessage(headerLine);
            p.sendMessage("§2§l╚═══════════════════════════════════════════╝");
            p.sendMessage("§e" + debateTitle);
            p.sendMessage("§a" + debatantA + " §7vs §c" + debatantB);
            p.sendMessage("§7Use §a/debate vote <councillor_name>§7 to support a side!");
            p.sendMessage("§7Winner: §a+20 Green Credits§7 | Loser: §c-5 Green Credits");
            p.sendMessage("");
        });
    }

    private void startDebate() {
        preDebateCountdown = false;
        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null) {
            scheduleNextDebate();
            return;
        }

        // Prepare topic again if no pre-announce happened
        if (debatantA == null) {
            DebateTopic topic = DEBATE_TOPICS[random.nextInt(DEBATE_TOPICS.length)];
            debateTitle = topic.title;
            debatantA = "Councillor Wheatgrass";
            debatantB = "Recycling Evangelist Bramble";
        }

        DebateTopic topic = getTopicByTitle(debateTitle);
        if (topic == null) {
            topic = DEBATE_TOPICS[random.nextInt(DEBATE_TOPICS.length)];
            debateTitle = topic.title;
        }

        debateActive = true;
        dialogueLine = 0;
        playerVotes.clear();

        // Move debating NPCs near spawn
        Location spawnLoc = world.getSpawnLocation();
        moveDebatantToSpawn(world, debatantA, spawnLoc.clone().add(-3, 0, 0));
        moveDebatantToSpawn(world, debatantB, spawnLoc.clone().add(3, 0, 0));

        // Gather audience NPCs (all others teleport nearby)
        gatherAudience(world, spawnLoc);

        final DebateTopic finalTopic = topic;

        // Announce debate start
        world.getPlayers().forEach(p -> {
            p.sendTitle("§2§lDEBATE BEGINS!", "§e" + debateTitle.substring(0, Math.min(debateTitle.length(), 40)), 10, 60, 10);
            p.sendMessage("§2§l===== THE DEBATE HAS BEGUN! =====");
            p.sendMessage("§a" + debatantA + " §7vs §c" + debatantB);
            p.sendMessage("§7Cast your vote now: §a/debate vote <councillor_name>");
            p.sendMessage("§2§l=================================");
        });

        // Play dialogue every 4 seconds
        dialogueTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dialogueLine >= finalTopic.dialogue.length) {
                endDebate();
                return;
            }

            String line = finalTopic.dialogue[dialogueLine]
                .replace("%A", debatantA)
                .replace("%B", debatantB);

            world.getPlayers().forEach(p -> p.sendMessage(line));
            dialogueLine++;

        }, 20L, 80L); // 4 seconds between lines
    }

    private void endDebate() {
        if (dialogueTask != null) {
            dialogueTask.cancel();
            dialogueTask = null;
        }

        World world = plugin.getDimensionManager().getVerdantWorld();

        // Count votes
        int votesA = 0, votesB = 0;
        for (String vote : playerVotes.values()) {
            if (vote.equals("A")) votesA++;
            else if (vote.equals("B")) votesB++;
        }

        // Determine winner (random if tie)
        String winner, loser;
        if (votesA > votesB) {
            winner = debatantA;
            loser = debatantB;
        } else if (votesB > votesA) {
            winner = debatantB;
            loser = debatantA;
        } else {
            // Tie — random winner
            boolean aWins = random.nextBoolean();
            winner = aWins ? debatantA : debatantB;
            loser = aWins ? debatantB : debatantA;
        }

        final String finalWinner = winner;
        final String finalLoser = loser;
        final int finalVotesA = votesA;
        final int finalVotesB = votesB;

        if (world != null) {
            world.getPlayers().forEach(p -> {
                p.sendMessage("");
                p.sendMessage("§2§l===== DEBATE CONCLUDED! =====");
                p.sendMessage("§aWinner: §2§l" + finalWinner + " §r§a(" + (finalWinner.equals(debatantA) ? finalVotesA : finalVotesB) + " votes)");
                p.sendMessage("§cLoser: §7" + finalLoser + " (" + (finalLoser.equals(debatantA) ? finalVotesA : finalVotesB) + " votes)");
                p.sendMessage("§7The council has noted the result. §8(It will be ignored in policy.)");
                p.sendMessage("§2§l=============================");
            });

            // Reward/penalise voters
            int winnerReward = plugin.getConfig().getInt("debates.winner-reward", 20);
            int loserPenalty = plugin.getConfig().getInt("debates.loser-penalty", 5);
            for (Map.Entry<UUID, String> entry : playerVotes.entrySet()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p == null) continue;
                boolean votedForA = entry.getValue().equals("A");
                String votedFor = votedForA ? debatantA : debatantB;
                if (votedFor.equals(finalWinner)) {
                    plugin.getGreenCreditManager().addCredits(p, winnerReward, "Debate: backed the winner");
                    p.sendMessage("§a+" + winnerReward + " Green Credits! §7You backed the right councillor.");
                } else {
                    plugin.getGreenCreditManager().deductCredits(p, loserPenalty);
                    p.sendMessage("§c-" + loserPenalty + " Green Credits. §7Backing the losing side has consequences.");
                }
            }
        }

        debateActive = false;
        debatantA = null;
        debatantB = null;
        debateTitle = null;
        playerVotes.clear();

        // Schedule next
        scheduleNextDebate();
    }

    // ─── Player Vote Command ──────────────────────────────────────────────────

    public void handleVoteCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /debate vote <councillor_name>");
            return;
        }

        if (!debateActive) {
            if (preDebateCountdown) {
                player.sendMessage("§7The debate hasn't started yet — voting opens when it begins. §8Stand by.");
            } else {
                player.sendMessage("§7There's no debate in progress. §8Check the announcement schedule.");
            }
            return;
        }

        String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).toLowerCase();

        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world == null || !world.equals(player.getWorld())) {
            player.sendMessage("§cYou must be in the Verdant Utopia to vote in debates!");
            return;
        }

        if (playerVotes.containsKey(player.getUniqueId())) {
            player.sendMessage("§7You've already voted in this debate. §8The council admires your decisiveness.");
            return;
        }

        String side;
        if (debatantA != null && debatantA.toLowerCase().contains(query)) {
            side = "A";
        } else if (debatantB != null && debatantB.toLowerCase().contains(query)) {
            side = "B";
        } else {
            player.sendMessage("§cCouncillor not found in current debate: §7" + query);
            player.sendMessage("§7Debating: §a" + debatantA + " §7vs §c" + debatantB);
            return;
        }

        playerVotes.put(player.getUniqueId(), side);
        String votedFor = side.equals("A") ? debatantA : debatantB;
        player.sendMessage("§aVote cast for §2" + votedFor + "§a!");
        player.sendMessage("§7The council notes your political alignment. §8For the record.");

        // Notify others
        World verdant = plugin.getDimensionManager().getVerdantWorld();
        if (verdant != null) {
            verdant.getPlayers().stream()
                .filter(p -> !p.equals(player))
                .forEach(p -> p.sendMessage("§8[Debate] " + player.getName() + " has voted for " + votedFor + "!"));
        }
    }

    // ─── NPC Audience ─────────────────────────────────────────────────────────

    private void gatherAudience(World world, Location center) {
        List<Entity> npcs = getNpcs(world);
        for (Entity npc : npcs) {
            String name = stripColor(npc.getCustomName());
            if (name != null && (name.equals(debatantA) || name.equals(debatantB))) continue;

            // Teleport audience member near spawn
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 5 + random.nextDouble() * 4;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            int y = world.getHighestBlockYAt((int)x, (int)z) + 1;

            npc.teleport(new Location(world, x, y, z));
        }
    }

    private void moveDebatantToSpawn(World world, String npcName, Location target) {
        for (Entity npc : getNpcs(world)) {
            String name = stripColor(npc.getCustomName());
            if (npcName.equals(name)) {
                int y = world.getHighestBlockYAt(target.getBlockX(), target.getBlockZ()) + 1;
                target.setY(y);
                npc.teleport(target);
                break;
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private DebateTopic getTopicByTitle(String title) {
        if (title == null) return null;
        for (DebateTopic t : DEBATE_TOPICS) {
            if (t.title().equals(title)) return t;
        }
        return null;
    }

    private List<Entity> getNpcs(World world) {
        List<Entity> result = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.VILLAGER && e.hasMetadata("greenparty_npc")) {
                result.add(e);
            }
        }
        return result;
    }

    private String stripColor(String s) {
        if (s == null) return null;
        return s.replaceAll("§.", "");
    }

    public boolean isDebateActive() { return debateActive; }
    public boolean isPreDebateCountdown() { return preDebateCountdown; }
    public String getDebatantA()   { return debatantA; }
    public String getDebatantB()   { return debatantB; }
    public String getDebateTitle() { return debateTitle; }

    /**
     * Manually trigger a debate — used by /debate trigger or /debate start.
     *
     * Announces the debate with the configured countdown window (default 2 minutes),
     * then starts the actual debate and voting after that window elapses.
     *
     * Returns false if a debate or countdown is already in progress.
     */
    public boolean triggerNow() {
        if (debateActive || preDebateCountdown) return false;

        // Cancel pending scheduled announcement if any
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }

        // Pick a random topic and councillors
        DebateTopic topic = DEBATE_TOPICS[random.nextInt(DEBATE_TOPICS.length)];
        debateTitle = topic.title;

        World world = plugin.getDimensionManager().getVerdantWorld();
        if (world != null) {
            List<Entity> npcs = getNpcs(world);
            if (npcs.size() >= 2) {
                Collections.shuffle(npcs);
                debatantA = stripColor(npcs.get(0).getCustomName());
                debatantB = stripColor(npcs.get(1).getCustomName());
            } else {
                debatantA = "Councillor Wheatgrass";
                debatantB = "Recycling Evangelist Bramble";
            }

            // Broadcast the announcement with the countdown window
            broadcastDebateAnnouncement(world, true);
        } else {
            debatantA = "Councillor Wheatgrass";
            debatantB = "Recycling Evangelist Bramble";
        }

        preDebateCountdown = true;

        // Start debate after the configured announcement window
        int offsetTicks = getAnnouncementOffsetTicks();
        Bukkit.getScheduler().runTaskLater(plugin, this::startDebate, offsetTicks);
        return true;
    }

    public void cancelAll() {
        if (debateScheduleTask != null) debateScheduleTask.cancel();
        if (dialogueTask != null) dialogueTask.cancel();
        if (announcementTask != null) announcementTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
    }
}
