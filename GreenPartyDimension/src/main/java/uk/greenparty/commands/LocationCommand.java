package uk.greenparty.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import uk.greenparty.GreenPartyPlugin;
import uk.greenparty.routines.LocationRegistry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LocationCommand — Admin command to inspect the routine LocationRegistry.
 *
 * /location list              — List all registered location names
 * /location info <name>       — Show coordinates for a specific location
 *
 * Requires greenparty.admin permission.
 * Useful for verifying that structure locations registered correctly after startup.
 */
public class LocationCommand implements CommandExecutor, TabCompleter {

    private final GreenPartyPlugin plugin;

    public LocationCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LocationRegistry registry = plugin.getLocationRegistry();

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendList(sender, registry);
            return true;
        }

        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            String name = args[1].toLowerCase();
            sendInfo(sender, registry, name);
            return true;
        }

        sender.sendMessage("§7Usage: §a/location list §7| §a/location info <name>");
        return true;
    }

    private void sendList(CommandSender sender, LocationRegistry registry) {
        Collection<String> names = registry.listAll();
        if (names.isEmpty()) {
            sender.sendMessage("§7[LocationRegistry] §cNo locations registered yet.");
            sender.sendMessage("§7(Structures may still be generating — try again in a few seconds.)");
            return;
        }

        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);

        sender.sendMessage("§2§l=== LocationRegistry (" + sorted.size() + " locations) ===");
        // Chunk into lines of 4 to keep chat readable
        StringBuilder sb = new StringBuilder("§7");
        int col = 0;
        for (String name : sorted) {
            sb.append("§a").append(name).append("§7, ");
            col++;
            if (col >= 4) {
                sender.sendMessage(sb.toString());
                sb = new StringBuilder("§7");
                col = 0;
            }
        }
        if (col > 0) {
            // Remove trailing ", "
            String line = sb.toString();
            if (line.endsWith(", ")) line = line.substring(0, line.length() - 2);
            sender.sendMessage(line);
        }
        sender.sendMessage("§7Use §a/location info <name>§7 for coordinates.");
    }

    private void sendInfo(CommandSender sender, LocationRegistry registry, String name) {
        Location loc = registry.getLocation(name);
        if (loc == null) {
            sender.sendMessage("§c[LocationRegistry] Unknown location: §7" + name);
            sender.sendMessage("§7Use §a/location list§7 to see all registered names.");
            return;
        }

        String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        sender.sendMessage("§2§l=== Location: " + name + " ===");
        sender.sendMessage(String.format("§7World: §a%s", world));
        sender.sendMessage(String.format("§7X: §a%.2f  §7Y: §a%.2f  §7Z: §a%.2f",
            loc.getX(), loc.getY(), loc.getZ()));
        sender.sendMessage(String.format("§7Block: §a%d, %d, %d",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "info").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            LocationRegistry registry = plugin.getLocationRegistry();
            return registry.listAll().stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
