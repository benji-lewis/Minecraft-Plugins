package uk.co.xfour.spacetravel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.spacetravel.modules.Module;

/**
 * Handles the /spacetravel command interactions.
 */
public final class SpaceTravelCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final List<Module> modules;
    private final SpaceTravelItemFactory itemFactory;

    /**
     * Creates a new space travel command handler.
     *
     * @param plugin the owning plugin
     * @param modules the configured modules
     */
    public SpaceTravelCommand(JavaPlugin plugin, List<Module> modules, SpaceTravelItemFactory itemFactory) {
        this.plugin = plugin;
        this.modules = new ArrayList<>(modules);
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            sendStatus(sender);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadConfig();
            sender.sendMessage("SpaceTravel configuration reloaded.");
            return true;
        }
        if ("give".equalsIgnoreCase(args[0])) {
            handleGive(sender, label, args);
            return true;
        }
        sender.sendMessage("Usage: /" + label + " <status|reload|give>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "reload", "give").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> options = new ArrayList<>();
            if (sender instanceof Player) {
                options.addAll(getItemKeys());
            }
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return options.stream()
                    .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            if (Bukkit.getOnlinePlayers().stream()
                    .anyMatch(player -> player.getName().equalsIgnoreCase(args[1]))) {
                return getItemKeys().stream()
                        .filter(option -> option.startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("SpaceTravel modules:");
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("modules");
        if (section == null) {
            sender.sendMessage("- No module configuration found.");
            return;
        }
        for (String key : section.getKeys(false)) {
            boolean enabled = plugin.getConfig().getBoolean("modules." + key + ".enabled", true);
            sender.sendMessage("- " + key + ": " + (enabled ? "enabled" : "disabled"));
        }
        sender.sendMessage("Loaded module count: " + modules.size());
    }

    private void handleGive(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " give <player> <item> [amount]");
            return;
        }
        Player target = null;
        int itemIndex = 1;
        if (args.length >= 3) {
            Player resolved = Bukkit.getPlayer(args[1]);
            if (resolved != null) {
                target = resolved;
                itemIndex = 2;
            }
        }
        if (target == null) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage("You must specify an online player.");
                return;
            }
        }
        SpaceTravelItemType type = getItemType(args[itemIndex]);
        if (type == null) {
            sender.sendMessage("Unknown space travel item. Available: " + String.join(", ", getItemKeys()));
            return;
        }
        int amount = 1;
        int amountIndex = itemIndex + 1;
        if (args.length > amountIndex) {
            try {
                amount = Integer.parseInt(args[amountIndex]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("Amount must be a whole number.");
                return;
            }
        }
        if (amount <= 0) {
            sender.sendMessage("Amount must be at least 1.");
            return;
        }
        ItemStack item = itemFactory.createItem(type);
        item.setAmount(amount);
        target.getInventory().addItem(item).values()
                .forEach(remaining -> target.getWorld().dropItemNaturally(target.getLocation(), remaining));
        sender.sendMessage("Gave " + amount + " " + type.getDisplayName() + " to " + target.getName() + ".");
        if (!sender.equals(target)) {
            target.sendMessage("You received " + amount + " " + type.getDisplayName() + ".");
        }
    }

    private SpaceTravelItemType getItemType(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (SpaceTravelItemType type : SpaceTravelItemType.values()) {
            if (type.getKey().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    private List<String> getItemKeys() {
        List<String> keys = new ArrayList<>();
        for (SpaceTravelItemType type : SpaceTravelItemType.values()) {
            keys.add(type.getKey());
        }
        return keys;
    }
}
