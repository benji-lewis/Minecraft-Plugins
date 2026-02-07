package uk.co.xfour.furniture;

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
import uk.co.xfour.furniture.modules.Module;

/**
 * Handles the /furniture command interactions.
 */
public final class FurnitureCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final List<Module> modules;
    private final FurnitureItemFactory itemFactory;

    /**
     * Creates a new furniture command handler.
     *
     * @param plugin the owning plugin
     * @param modules the configured modules
     */
    public FurnitureCommand(JavaPlugin plugin, List<Module> modules, FurnitureItemFactory itemFactory) {
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
            sender.sendMessage("Furniture configuration reloaded.");
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
        sender.sendMessage("Furniture modules:");
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("modules");
        if (section == null) {
            sender.sendMessage("- No module configuration found.");
            return;
        }
        for (String key : section.getKeys(false)) {
            if ("tuning".equals(key)) {
                continue;
            }
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
        FurnitureItemDefinition definition = getDefinition(args[itemIndex]);
        if (definition == null) {
            sender.sendMessage("Unknown furniture item. Available: " + String.join(", ", getItemKeys()));
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
        ItemStack item = itemFactory.createItem(definition);
        item.setAmount(amount);
        target.getInventory().addItem(item).values()
                .forEach(remaining -> target.getWorld().dropItemNaturally(target.getLocation(), remaining));
        sender.sendMessage("Gave " + amount + " " + definition.displayName() + " to " + target.getName() + ".");
        if (!sender.equals(target)) {
            target.sendMessage("You received " + amount + " " + definition.displayName() + ".");
        }
    }

    private FurnitureItemDefinition getDefinition(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (FurnitureItemDefinition definition : itemFactory.loadDefinitions()) {
            if (definition.key().equalsIgnoreCase(normalized)) {
                return definition;
            }
        }
        return null;
    }

    private List<String> getItemKeys() {
        return itemFactory.loadDefinitions().stream()
                .map(FurnitureItemDefinition::key)
                .sorted()
                .collect(Collectors.toList());
    }
}
