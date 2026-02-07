package uk.co.xfour.furniture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.xfour.furniture.modules.Module;

/**
 * Handles the /furniture command interactions.
 */
public final class FurnitureCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final List<Module> modules;

    /**
     * Creates a new furniture command handler.
     *
     * @param plugin the owning plugin
     * @param modules the configured modules
     */
    public FurnitureCommand(JavaPlugin plugin, List<Module> modules) {
        this.plugin = plugin;
        this.modules = new ArrayList<>(modules);
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
        sender.sendMessage("Usage: /" + label + " <status|reload>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "reload").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
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
}
