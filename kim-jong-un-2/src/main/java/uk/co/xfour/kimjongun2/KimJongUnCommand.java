package uk.co.xfour.kimjongun2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KimJongUnCommand extends Command implements PluginIdentifiableCommand {
    private final KimJongUn2Plugin plugin;
    private final KimJongUnItems items;
    private final KimJongUnSpawner spawner;

    public KimJongUnCommand(KimJongUn2Plugin plugin, KimJongUnItems items, KimJongUnSpawner spawner) {
        super("kimjongun2");
        this.plugin = plugin;
        this.items = items;
        this.spawner = spawner;
        setDescription("Kim Jong Un 2 admin command.");
        setUsage("/kimjongun2 <give|spawn> [player] [item]");
        setPermission("kimjongun2.admin");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /kimjongun2 <give|spawn> [player] [item]");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /kimjongun2 spawn.");
                return true;
            }
            Location location = player.getLocation();
            spawner.spawnMob(location);
            sender.sendMessage("Spawned Kim Jong Un 2.");
            return true;
        }
        if (sub.equals("give")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /kimjongun2 give <player> <item>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found.");
                return true;
            }
            Optional<KimJongUnItems.KimJongUnItem> itemType = KimJongUnItems.KimJongUnItem.fromId(args[2]);
            if (itemType.isEmpty()) {
                sender.sendMessage("Unknown item. Try: " +
                        KimJongUnItems.KimJongUnItem.partItems().stream()
                                .map(KimJongUnItems.KimJongUnItem::id)
                                .collect(Collectors.joining(", ")) + ", missile, launchpad");
                return true;
            }
            ItemStack stack = items.createItem(itemType.get());
            target.getInventory().addItem(stack);
            sender.sendMessage("Gave " + itemType.get().displayName() + " to " + target.getName() + ".");
            return true;
        }
        sender.sendMessage("Unknown subcommand.");
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                             @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "spawn").stream()
                    .filter(entry -> entry.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>();
            ids.addAll(KimJongUnItems.KimJongUnItem.partItems().stream()
                    .map(KimJongUnItems.KimJongUnItem::id)
                    .collect(Collectors.toList()));
            ids.add("missile");
            ids.add("launchpad");
            return ids.stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public @NotNull KimJongUn2Plugin getPlugin() {
        return plugin;
    }
}
