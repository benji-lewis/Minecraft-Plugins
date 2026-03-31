package uk.greenparty.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.managers.CosmeticsManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /cosmetics <list|equip <id>|unequip <id>>
 */
public class CosmeticsCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public CosmeticsCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command must be used by a player.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getCosmeticsManager().listCosmetics(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "equip" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: §a/cosmetics equip <id>"); return true; }
                plugin.getCosmeticsManager().equipCosmetic(player, args[1]);
            }
            case "unequip" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: §a/cosmetics unequip <id>"); return true; }
                plugin.getCosmeticsManager().unequipCosmetic(player, args[1]);
            }
            case "check" -> plugin.getCosmeticsManager().checkUnlocks(player);
            default -> {
                player.sendMessage("§cUsage: §a/cosmetics <list|equip <id>|unequip <id>|check>");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "equip", "unequip", "check");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("equip") || args[0].equalsIgnoreCase("unequip"))) {
            String partial = args[1].toLowerCase();
            return plugin.getCosmeticsManager().getAllCosmetics().stream()
                .map(def -> def.id)
                .filter(id -> id.startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
