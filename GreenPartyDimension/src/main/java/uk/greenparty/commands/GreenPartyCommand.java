package uk.greenparty.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.CustomItemManager;
import uk.greenparty.managers.QuestManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main /greenparty command handler.
 *
 * Usage: /greenparty <subcommand>
 * Aliases: /gp, /greens
 *
 * Available subcommands:
 * - teleport  : Travel to/from The Verdant Utopia
 * - leave     : Return to the overworld (blocked if conscripted)
 * - info      : Display information about the dimension
 * - quest     : Manage your Green Party quests
 * - manifesto : Read the Green Party Dimension Manifesto
 * - council   : Information about the Green Council NPCs
 * - release   : Release a conscripted player (admin only)
 * - help      : Show this help
 *
 * Phase 2 additions:
 * - "leave" now checks conscription/kidnap status
 * - "release <player>" admin command to free conscripted players
 */
public class GreenPartyCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public GreenPartyCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            // Allow "release" from console
            if (args.length >= 2 && args[0].equalsIgnoreCase("release")) {
                handleReleaseConsole(sender, args);
                return true;
            }
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "teleport", "tp", "travel", "visit" -> handleTeleport(player);
            case "info", "about", "dimension" -> handleInfo(player);
            case "quest", "quests", "q" -> handleQuest(player, args);
            case "manifesto", "policy", "values" -> handleManifesto(player);
            case "help", "?" -> sendHelp(player);
            case "leave", "exit", "back" -> handleLeave(player);
            case "council", "npcs" -> handleCouncilInfo(player);
            case "release" -> handleRelease(player, args);
            case "give" -> handleGive(player, args);
            case "stats" -> handleStats(player, args);
            case "dimension-info", "diminfo" -> handleDimensionInfo(player);
            default -> {
                player.sendMessage("§cUnknown subcommand: §7" + args[0]);
                player.sendMessage("§7The council is disappointed. Try §a/greenparty help§7.");
            }
        }

        return true;
    }

    // ─── Teleport ─────────────────────────────────────────────────────────────

    private void handleTeleport(Player player) {
        if (!player.hasPermission("greenparty.teleport")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission",
                "§cYou don't have permission! Have you considered writing a strongly-worded letter?"));
            return;
        }

        if (plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            player.sendMessage("§7You're already in §2The Verdant Utopia§7!");
            player.sendMessage("§7Use §a/greenparty leave§7 to return to the overworld.");
            return;
        }

        player.sendMessage("§2Preparing your environmentally-approved inter-dimensional journey...");
        plugin.getDimensionManager().teleportToVerdantUtopia(player);
    }

    // ─── Leave (with kidnap check) ────────────────────────────────────────────

    private void handleLeave(Player player) {
        if (!plugin.getDimensionManager().isVerdantWorld(player.getWorld())) {
            player.sendMessage("§7You're not in §2The Verdant Utopia§7. You can't leave somewhere you aren't.");
            player.sendMessage("§7§o(The council would like you to reconsider your spatial awareness.)");
            return;
        }

        // ─── Kidnap check ─────────────────────────────────────────────────────
        boolean isKidnapped = plugin.isKidnapped(player.getUniqueId());
        if (!isKidnapped) {
            // Also check config targets
            isKidnapped = !plugin.getKidnapTargets().isEmpty()
                && plugin.getKidnapTargets().contains(player.getName().toLowerCase());
        }

        if (isKidnapped) {
            player.sendMessage("§c§l[Green Council] §r§cYou've been conscripted! You cannot leave the Green Council.");
            player.sendMessage("§7§o\"Motion 48a: Involuntary Membership applies. Your request to leave has been logged, denied, and filed.\"");
            player.sendMessage("§7Contact an administrator for a §a/greenparty release§7 if you believe this is an error.");
            player.sendMessage("§7§o(It is not an error. The council is certain.)");

            // Dramatic sound
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendTitle("§c§l⛔ CONSCRIPTED", "§cYou cannot leave the Green Council.", 10, 60, 20);
            return;
        }

        plugin.getDimensionManager().teleportToOverworld(player);
    }

    // ─── Release Command ──────────────────────────────────────────────────────

    private void handleRelease(Player sender, String[] args) {
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage("§cOnly admins can release conscripted players.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: §a/greenparty release <player>");
            return;
        }

        releasePlayer(sender, args[1]);
    }

    private void handleReleaseConsole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: greenparty release <player>");
            return;
        }
        releasePlayer(sender, args[1]);
    }

    private void releasePlayer(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        java.util.UUID targetUUID = null;
        String resolvedName = targetName;

        if (target != null) {
            targetUUID = target.getUniqueId();
            resolvedName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (offline.hasPlayedBefore()) {
                targetUUID = offline.getUniqueId();
                resolvedName = offline.getName() != null ? offline.getName() : targetName;
            }
        }

        if (targetUUID == null) {
            sender.sendMessage("§cPlayer not found: §7" + targetName);
            return;
        }

        boolean wasKidnapped = plugin.isKidnapped(targetUUID);
        plugin.removeKidnapped(targetUUID);

        if (wasKidnapped) {
            sender.sendMessage("§2[Green Party] §a" + resolvedName + " §7has been released from conscription.");
            sender.sendMessage("§7§o(The council has grudgingly approved this release. Motion 49c: Compassionate Grounds.)");
        } else {
            sender.sendMessage("§7" + resolvedName + " §7was not conscripted.");
        }

        // Notify the player if online
        if (target != null && target.isOnline()) {
            target.sendMessage("§2§l[Green Council] §r§aYou have been released from conscription by §a" + sender.getName() + "§a.");
            target.sendMessage("§7You are now free to leave The Verdant Utopia.");
            target.sendMessage("§7§o(The council notes this with mixed feelings.)");
            target.playSound(target.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
        }
    }

    // ─── Give (Admin: bestow custom items) ───────────────────────────────────

    private static final List<String> ITEM_NAMES = Arrays.asList("manifesto", "badge", "spoon");

    private void handleGive(Player sender, String[] args) {
        // Permission check — admin only
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage(ChatColor.RED + "✗ You lack the administrative authority to distribute Green Party artefacts.");
            sender.sendMessage(ChatColor.DARK_RED + "  (The council has noted your insubordination.)");
            return;
        }

        // Usage: /greenparty give <player> <item>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.GREEN + "╔═══════════════════════════════════════╗");
            sender.sendMessage(ChatColor.GREEN + "║     " + ChatColor.BOLD + "Green Party Custom Items" + ChatColor.GREEN + "          ║");
            sender.sendMessage(ChatColor.GREEN + "╚═══════════════════════════════════════╝");
            sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/greenparty give <player> <item_name>");
            sender.sendMessage(ChatColor.YELLOW + "Items: " + ChatColor.GREEN + "manifesto" + ChatColor.GRAY + ", " +
                    ChatColor.GREEN + "badge" + ChatColor.GRAY + ", " + ChatColor.GREEN + "spoon");
            sender.sendMessage(ChatColor.GRAY + "Example: /greenparty give yupBenji manifesto");
            return;
        }

        String targetName = args[1];
        String itemName   = args[2].toLowerCase();

        if (!ITEM_NAMES.contains(itemName)) {
            sender.sendMessage(ChatColor.RED + "✗ Invalid item: " + ChatColor.YELLOW + args[2]);
            sender.sendMessage(ChatColor.GRAY + "  Valid items: " + ChatColor.GREEN + "manifesto" +
                    ChatColor.GRAY + ", " + ChatColor.GREEN + "badge" +
                    ChatColor.GRAY + ", " + ChatColor.GREEN + "spoon");
            return;
        }

        // Find target player (online only)
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(targetName)) { target = p; break; }
            }
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "✗ Player not found: " + ChatColor.YELLOW + targetName);
            sender.sendMessage(ChatColor.GRAY + "  (They must be online to receive Green Party artefacts.)");
            return;
        }

        CustomItemManager cim = plugin.getCustomItemManager();
        ItemStack item;
        String prettyName;

        switch (itemName) {
            case "manifesto" -> { item = cim.createManifesto();               prettyName = "Green Party Manifesto"; }
            case "badge"     -> { item = cim.createGreenPartyBadge();         prettyName = "Green Party Badge"; }
            case "spoon"     -> { item = cim.createCompostCeremonialSpoon();  prettyName = "Compost Ceremonial Spoon"; }
            default          -> { sender.sendMessage(ChatColor.RED + "✗ Unexpected error: item lookup failed."); return; }
        }

        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(ChatColor.RED + "✗ " + target.getName() + "'s inventory is full!");
            target.sendMessage(ChatColor.YELLOW + "⚠ An admin tried to give you a " + ChatColor.GREEN + prettyName +
                    ChatColor.YELLOW + " but your inventory is full!");
            return;
        }

        target.getInventory().addItem(item);

        sender.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + prettyName +
                ChatColor.GREEN + " given to " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + ".");

        target.sendMessage(ChatColor.GREEN + "╔═════════════════════════════════════╗");
        target.sendMessage(ChatColor.GREEN + "║  " + ChatColor.BOLD + "Green Party Artefact Received" + ChatColor.GREEN + "       ║");
        target.sendMessage(ChatColor.GREEN + "╚═════════════════════════════════════╝");
        target.sendMessage(ChatColor.WHITE + "You have received: " + ChatColor.GREEN + ChatColor.BOLD + prettyName);
        target.sendMessage(ChatColor.GRAY + "Bestowed by " + ChatColor.YELLOW + sender.getName() +
                ChatColor.GRAY + " on behalf of the Green Council.");
        target.sendMessage(ChatColor.DARK_GREEN + "Handle with ideological responsibility.");

        plugin.getLogger().info("[CustomItems] Admin " + sender.getName() + " gave " + prettyName + " to " + target.getName());
    }

    // ─── Info ─────────────────────────────────────────────────────────────────

    private void handleInfo(Player player) {
        player.sendMessage("§2§l===== THE VERDANT UTOPIA =====");
        player.sendMessage("§7A dimension of extraordinary greenness, maintained by the §2Green Council§7.");
        player.sendMessage("");
        player.sendMessage("§aFeatures:");
        player.sendMessage("  §7• §aPermanent midday sun §8(solar energy, obviously)");
        player.sendMessage("  §7• §aNo hostile mobs §8(they were asked to leave)");
        player.sendMessage("  §7• §aNo fall damage §8(H&S regulation 4.7b)");
        player.sendMessage("  §7• §aNo hunger §8(universal basic sustenance, policy 12)");
        player.sendMessage("  §7• §a6 Green Councillors §8(very talkative, very passionate)");
        player.sendMessage("  §7• §a7 available quests §8(approved after 14 committee meetings)");
        player.sendMessage("  §7• §aEmerald ore everywhere §8(symbolic prosperity)");
        player.sendMessage("  §7• §aZero coal §8(banned, see manifesto §4)");
        player.sendMessage("  §7• §a♻ Recycling bins §8(throw items in, get items back)");
        player.sendMessage("  §7• §aDemocratic motions §8(vote on dimension-affecting policies)");
        player.sendMessage("  §7• §aViolation enforcement §8(break rules, pay the price)");
        player.sendMessage("");
        boolean inDimension = plugin.getDimensionManager().isVerdantWorld(player.getWorld());
        if (inDimension) {
            player.sendMessage("§aYou are currently IN the Verdant Utopia. 🌿");
            if (plugin.isKidnapped(player.getUniqueId())) {
                player.sendMessage("§c§o(You are conscripted. You may not leave. The council is pleased.)");
            }
        } else {
            player.sendMessage("§7Use §a/greenparty teleport§7 to visit.");
        }
        player.sendMessage("§2§l==============================");
    }

    // ─── Quest ────────────────────────────────────────────────────────────────

    private void handleQuest(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getQuestManager().showQuestStatus(player);
            player.sendMessage("§7Subcommands: §alist§7, §aassign§7, §astatus");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list", "all" -> showAllQuests(player);
            case "assign", "get", "new" -> plugin.getQuestManager().assignQuests(player);
            case "status", "progress" -> plugin.getQuestManager().showQuestStatus(player);
            case "submit" -> {
                player.sendMessage("§2[Quest System] §7Submission received. §8The council will review this in 6-8 weeks.");
            }
            default -> plugin.getQuestManager().showQuestStatus(player);
        }
    }

    private void showAllQuests(Player player) {
        player.sendMessage("§2§l===== AVAILABLE QUESTS =====");
        for (QuestManager.QuestDefinition quest : plugin.getQuestManager().getAllQuests().values()) {
            player.sendMessage("§a▸ §r" + quest.name);
            player.sendMessage("  §8" + quest.description.replace("§7", "§8"));
            player.sendMessage("  §7Reward: §a" + quest.rewardXp + " XP§7 + items");
            player.sendMessage("");
        }
        player.sendMessage("§7Use §a/greenparty quest assign§7 to get quests assigned to you.");
        player.sendMessage("§2§l============================");
    }

    // ─── Manifesto ────────────────────────────────────────────────────────────

    private void handleManifesto(Player player) {
        String[] lines = {
            "§2§l===== THE GREEN PARTY DIMENSION MANIFESTO =====§r",
            "§7§o\"Adopted at the Third Extraordinary Meeting of the Green Council\"",
            "",
            "§a§lARTICLE I: THE GREENNESS",
            "§71. Everything shall be green.",
            "§72. Coal is banned.",
            "§73. Emeralds are the official currency of environmental virtue",
            "",
            "§a§lARTICLE II: THE ECOSYSTEM",
            "§74. All meat blocks must be renamed 'legacy protein'",
            "§75. Creepers are a protected species. §c§o(we accept no liability for explosions)",
            "§76. Dirt is sacred. Handle with reverence and a permit.",
            "§77. Trees may not be felled without planting two replacements",
            "",
            "§a§lARTICLE III: GOVERNANCE",
            "§79. TNT requires a full environmental impact assessment (14 weeks)",
            "§710. All building projects require planning permission (6-8 weeks)",
            "§711. The Nether is banned on environmental grounds. §8Too hot.",
            "",
            "§a§lARTICLE IV: PERSONAL RESPONSIBILITY",
            "§713. Say 'reduce, reuse, recycle' before logging off",
            "§714. Composters must be placed within 20 blocks of any settlement",
            "§715. Smelting coal is permitted only with written apology to the furnace",
            "",
            "§a§lARTICLE V: PHASE 2 AMENDMENTS",
            "§716. Environmental violations are subject to mandatory fines.",
            "§717. Recycling is encouraged and rewarded.",
            "§718. All motions shall be voted on democratically. §8(By those present.)",
            "§719. Conscripted members may not leave without written council approval.",
            "",
            "§7§o\"This manifesto has been printed on recycled pixels.\"",
            "§2§l================================================="
        };

        for (String line : lines) player.sendMessage(line);
    }

    // ─── Council Info ─────────────────────────────────────────────────────────

    private void handleCouncilInfo(Player player) {
        player.sendMessage("§2§l===== THE GREEN COUNCIL =====");
        player.sendMessage("§7The elected (self-appointed) council of The Verdant Utopia:");
        player.sendMessage("");
        player.sendMessage("  §2Councillor Wheatgrass §8— Chair, Composting Committee");
        player.sendMessage("  §aRecycling Evangelist Bramble §8— Head of Material Reuse");
        player.sendMessage("  §2Elder Composting Sage Fern §8— Ancient Wisdom (and compost)");
        player.sendMessage("  §eProtest Coordinator Meadow §8— Direct Action (mostly placards)");
        player.sendMessage("  §bPolicy Officer Sedge §8— 247-page policy documents");
        player.sendMessage("  §6Fundraising Captain Sorrel §8— Tote bags, exclusively");
        player.sendMessage("");
        player.sendMessage("§7Find them near the spawn point in The Verdant Utopia.");
        player.sendMessage("§7Right-click to interact. §8They will talk for a long time.");
        player.sendMessage("§2§l=============================");
    }

    // ─── Help ─────────────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage("§2§l===== GREEN PARTY DIMENSION =====");
        player.sendMessage("§7§o\"A plugin approved by the Environment Committee\"");
        player.sendMessage("");
        player.sendMessage("§a/greenparty teleport §7- Travel to The Verdant Utopia");
        player.sendMessage("§a/greenparty leave §7- Return to the overworld");
        player.sendMessage("§a/greenparty info §7- Dimension information");
        player.sendMessage("§a/greenparty quest §7- View your quests");
        player.sendMessage("§a/greenparty quest assign §7- Get new quests");
        player.sendMessage("§a/greenparty quest list §7- See all available quests");
        player.sendMessage("§a/greenparty manifesto §7- Read the manifesto");
        player.sendMessage("§a/greenparty council §7- Meet the Green Council");
        player.sendMessage("");
        player.sendMessage("§7New in v1.2.0:");
        player.sendMessage("§a/violations [player] §7- Check environmental violations");
        player.sendMessage("§a/motion start <title> §7- Raise a democratic motion");
        player.sendMessage("§a/motion vote <yes|no> §7- Vote on active motion");
        player.sendMessage("§a/motion results §7- Check vote status");
        player.sendMessage("");
        player.sendMessage("§7Aliases: §a/gp§7, §a/greens");
        if (player.hasPermission("greenparty.admin")) {
            player.sendMessage("");
            player.sendMessage("§c[Admin] §a/greenportal §7- Create a portal");
            player.sendMessage("§c[Admin] §a/greenreset [player] §7- Reset progress");
            player.sendMessage("§c[Admin] §a/greenparty release <player> §7- Free a conscripted player");
            player.sendMessage("§c[Admin] §a/greenparty give <player> <manifesto|badge|spoon> §7- Bestow an artefact");
        }
        player.sendMessage("§2§l==================================");
    }

    // ─── Stats (Phase 4) ──────────────────────────────────────────────────────

    private void handleStats(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2 && player.hasPermission("greenparty.admin")) {
            Player found = Bukkit.getPlayerExact(args[1]);
            if (found != null) target = found;
            else { player.sendMessage("§cPlayer not found: §7" + args[1]); return; }
        }
        plugin.getProgressionManager().showFullStats(player, target);
    }

    // ─── Dimension Info (Phase 4) ─────────────────────────────────────────────

    private void handleDimensionInfo(Player player) {
        player.sendMessage("§2§l===== DIMENSION INFO =====");

        // World info
        org.bukkit.World w = plugin.getDimensionManager().getVerdantWorld();
        if (w != null) {
            player.sendMessage("§7World: §a" + w.getName() + " §8(loaded ✓)");
            player.sendMessage("§7Players inside: §a" + w.getPlayers().size());
            player.sendMessage("§7Planet Vitality: §a"
                + String.format("%.0f%%", plugin.getEnvironmentEffects().getPlanetVitality() * 100));
        } else {
            player.sendMessage("§cWorld not loaded.");
        }

        // Structures
        player.sendMessage("§2§l— Structures ——————————————");
        for (uk.greenparty.managers.StructureManager.StructureInfo info :
            plugin.getStructureManager().getAllStructures()) {
            String status = info.built ? "§a✓" : "§c✗";
            player.sendMessage("  " + status + " " + info.displayName
                + " " + info.locationString());
        }

        // Active effects
        var cfg = plugin.getConfig();
        player.sendMessage("§2§l— Active Effects ——————————");
        player.sendMessage("  §7Leaf Particles: "
            + (cfg.getBoolean("environment-effects.leaf-particles.enabled", true) ? "§aON" : "§cOFF"));
        player.sendMessage("  §7Composter Sounds: "
            + (cfg.getBoolean("environment-effects.composter-sounds.enabled", true) ? "§aON" : "§cOFF"));
        player.sendMessage("  §7Light Show: "
            + (cfg.getBoolean("environment-effects.light-show.enabled", true) ? "§aON" : "§cOFF"));
        player.sendMessage("  §7Aurora/Weather: "
            + (cfg.getBoolean("environment-effects.weather-effects.enabled", true) ? "§aON" : "§cOFF"));
        player.sendMessage("  §7Planet Vitality Bar: "
            + (cfg.getBoolean("environment-effects.boss-bar.enabled", true) ? "§aON" : "§cOFF"));

        // Seasonal bonus
        if (plugin.getProgressionManager().isSeasonalBonus()) {
            player.sendMessage("§6§l🎉 Seasonal Bonus: §a"
                + plugin.getProgressionManager().getSeasonalBonusDescription());
        }

        player.sendMessage("§2§l==========================");
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList(
                "teleport", "leave", "info", "quest", "manifesto", "council", "help",
                "stats", "dimension-info"));
            if (sender.hasPermission("greenparty.admin")) {
                subs.add("release");
                subs.add("give");
            }
            return subs;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("quest")) {
                return Arrays.asList("status", "assign", "list", "submit");
            }
            if (args[0].equalsIgnoreCase("release") && sender.hasPermission("greenparty.admin")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("greenparty.admin")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("greenparty.admin")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("greenparty.admin")) {
            String partial = args[2].toLowerCase();
            return ITEM_NAMES.stream()
                .filter(n -> n.startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
