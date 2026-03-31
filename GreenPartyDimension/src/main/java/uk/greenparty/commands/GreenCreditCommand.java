package uk.greenparty.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

/**
 * /greencredit command — manages the Green Credits economy.
 *
 * Subcommands:
 *   balance — check your balance (or another player's if admin)
 *   give <player> <amount> — admin only: give credits to a player
 *
 * The council has debated a "spend" command. It remains in committee.
 */
public class GreenCreditCommand implements CommandExecutor {

    private final GreenPartyPlugin plugin;

    public GreenCreditCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "balance", "bal", "b" -> handleBalance(sender, args);
            case "give", "g"           -> handleGive(sender, args);
            default                    -> showHelp(sender);
        }

        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        // /greencredit balance [player]
        if (args.length >= 2) {
            // Check another player (admin only)
            if (!sender.hasPermission("greenparty.admin")) {
                sender.sendMessage("§c[Green Credits] §7You don't have permission to check other players' balances.");
                sender.sendMessage("§8(The council finds this request presumptuous.)");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c[Green Credits] §7Player '" + args[1] + "' is not online.");
                return;
            }

            sender.sendMessage("§2[Green Credits] §7" + target.getName() + " has " +
                plugin.getGreenCreditManager().formatBalance(target) + ".");
        } else {
            // Check own balance
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c[Green Credits] §7Console doesn't have a balance. Console doesn't need Green Credits.");
                return;
            }

            int bal = plugin.getGreenCreditManager().getBalance(player);
            sender.sendMessage("");
            sender.sendMessage("§2§l[Green Credits] Balance§r");
            sender.sendMessage("§7Your balance: §a" + bal + " §2Green Credits");
            sender.sendMessage("§8The council tracks your financial virtue carefully.");
            sender.sendMessage("");
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        // /greencredit give <player> <amount>
        if (!sender.hasPermission("greenparty.admin")) {
            sender.sendMessage("§c[Green Credits] §7Only council administrators may distribute Green Credits.");
            sender.sendMessage("§8(This prevents capitalism. The council is very serious about this.)");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c[Green Credits] §7Usage: /greencredit give <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Green Credits] §7Player '" + args[1] + "' is not online.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[Green Credits] §7'" + args[2] + "' is not a valid number.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage("§c[Green Credits] §7Amount must be positive. The council does not support negative giving.");
            return;
        }

        plugin.getGreenCreditManager().addCredits(target, amount, "admin grant by " + sender.getName());
        sender.sendMessage("§2[Green Credits] §7Granted §a" + amount + " GC §7to §a" + target.getName() + "§7.");
        sender.sendMessage("§8The council has filed this transaction in triplicate.");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§2§l[Green Credits] Commands§r");
        sender.sendMessage("§a/greencredit balance §7— check your balance");
        sender.sendMessage("§a/greencredit balance <player> §7— check another player's balance (admin)");
        sender.sendMessage("§a/greencredit give <player> <amount> §7— give credits (admin only)");
        sender.sendMessage("§8Green Credits are the official currency of the Verdant Utopia.");
        sender.sendMessage("§8No, you can't convert them to real money. The council has tried.");
    }
}
