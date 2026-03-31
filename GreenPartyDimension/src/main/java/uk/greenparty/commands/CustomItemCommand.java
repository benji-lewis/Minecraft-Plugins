package uk.greenparty.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.CustomItemManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CustomItemCommand — /customitem give <player> <item_name>
 *
 * Admin-only command (greenparty.admin) to bestow the sacred artefacts
 * of the Green Party directly into a player's inventory.
 *
 * Because sometimes democracy needs a helping hand. Specifically, an admin's hand.
 *
 * Usage: /customitem give <player> <manifesto|badge|spoon>
 */
public class CustomItemCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;
    private static final List<String> ITEM_NAMES = Arrays.asList("manifesto", "badge", "spoon");

    public CustomItemCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Permission check — admin only
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage(ChatColor.RED + "✗ You lack the administrative authority to distribute Green Party artefacts.");
            sender.sendMessage(ChatColor.DARK_RED + "  (The council has noted your insubordination.)");
            return true;
        }

        // Validate subcommand
        if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.GREEN + "╔═══════════════════════════════════════╗");
            sender.sendMessage(ChatColor.GREEN + "║     " + ChatColor.BOLD + "Green Party Custom Items" + ChatColor.GREEN + "          ║");
            sender.sendMessage(ChatColor.GREEN + "╚═══════════════════════════════════════╝");
            sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/customitem give <player> <item_name>");
            sender.sendMessage(ChatColor.YELLOW + "Items: " + ChatColor.GREEN + "manifesto" + ChatColor.GRAY + ", " +
                    ChatColor.GREEN + "badge" + ChatColor.GRAY + ", " + ChatColor.GREEN + "spoon");
            sender.sendMessage(ChatColor.GRAY + "Example: /customitem give yupBenji manifesto");
            return true;
        }

        // Need player and item name
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "✗ Usage: /customitem give <player> <manifesto|badge|spoon>");
            return true;
        }

        String targetName = args[1];
        String itemName   = args[2].toLowerCase();

        // Validate item name
        if (!ITEM_NAMES.contains(itemName)) {
            sender.sendMessage(ChatColor.RED + "✗ Invalid item name: " + ChatColor.YELLOW + args[2]);
            sender.sendMessage(ChatColor.GRAY + "  Valid items: " + ChatColor.GREEN + "manifesto" +
                    ChatColor.GRAY + ", " + ChatColor.GREEN + "badge" +
                    ChatColor.GRAY + ", " + ChatColor.GREEN + "spoon");
            return true;
        }

        // Find target player (online only)
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            // Try case-insensitive search
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(targetName)) {
                    target = p;
                    break;
                }
            }
        }

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "✗ Player not found: " + ChatColor.YELLOW + targetName);
            sender.sendMessage(ChatColor.GRAY + "  (They must be online to receive Green Party artefacts.)");
            return true;
        }

        // Build the item
        CustomItemManager cim = plugin.getCustomItemManager();
        ItemStack item;
        String prettyName;

        switch (itemName) {
            case "manifesto":
                item = cim.createManifesto();
                prettyName = "Green Party Manifesto";
                break;
            case "badge":
                item = cim.createGreenPartyBadge();
                prettyName = "Green Party Badge";
                break;
            case "spoon":
                item = cim.createCompostCeremonialSpoon();
                prettyName = "Compost Ceremonial Spoon";
                break;
            default:
                // Should never reach here after validation above
                sender.sendMessage(ChatColor.RED + "✗ Unexpected error: item lookup failed.");
                return true;
        }

        // Check inventory space
        if (target.getInventory().firstEmpty() == -1) {
            sender.sendMessage(ChatColor.RED + "✗ " + target.getName() + "'s inventory is full!");
            sender.sendMessage(ChatColor.GRAY + "  The artefact cannot be delivered. Ask them to make space.");
            target.sendMessage(ChatColor.YELLOW + "⚠ An admin tried to give you a " + ChatColor.GREEN + prettyName +
                    ChatColor.YELLOW + " but your inventory is full!");
            return true;
        }

        // Give the item
        target.getInventory().addItem(item);

        // Confirm to sender
        String senderName = (sender instanceof Player) ? sender.getName() : "Console";
        sender.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.WHITE + prettyName +
                ChatColor.GREEN + " given to " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + ".");

        // Notify the recipient
        target.sendMessage(ChatColor.GREEN + "╔═════════════════════════════════════╗");
        target.sendMessage(ChatColor.GREEN + "║  " + ChatColor.BOLD + "Green Party Artefact Received" + ChatColor.GREEN + "       ║");
        target.sendMessage(ChatColor.GREEN + "╚═════════════════════════════════════╝");
        target.sendMessage(ChatColor.WHITE + "You have received: " + ChatColor.GREEN + ChatColor.BOLD + prettyName);
        target.sendMessage(ChatColor.GRAY + "Bestowed by " + ChatColor.YELLOW + senderName +
                ChatColor.GRAY + " on behalf of the Green Council.");
        target.sendMessage(ChatColor.DARK_GREEN + "Handle with ideological responsibility.");

        // Log it
        plugin.getLogger().info("[CustomItems] Admin " + senderName + " gave " + prettyName +
                " to " + target.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("greenparty.admin")) return List.of();

        if (args.length == 1) {
            return List.of("give");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String partial = args[2].toLowerCase();
            return ITEM_NAMES.stream()
                    .filter(name -> name.startsWith(partial))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
