package uk.co.xfour.kimjongun3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KimJongUnCommand implements BasicCommand {
    private final KimJongUn3Plugin plugin;
    private final KimJongUnItems items;
    private final KimJongUnSpawner spawner;
    private final RadiationSuit radiationSuit;

    public KimJongUnCommand(KimJongUn3Plugin plugin, KimJongUnItems items, KimJongUnSpawner spawner,
                            RadiationSuit radiationSuit) {
        this.plugin = plugin;
        this.items = items;
        this.spawner = spawner;
        this.radiationSuit = radiationSuit;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0) {
            sender.sendMessage("Usage: /kimjongun3 <give|spawn> [player] [item]");
            return;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /kimjongun3 spawn.");
                return;
            }
            Location location = player.getLocation();
            spawner.spawnMob(location);
            sender.sendMessage("Spawned Kim Jong Un 3.");
            return;
        }
        if (sub.equals("give")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /kimjongun3 give <player> <item>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found.");
                return;
            }
            Optional<KimJongUnItems.KimJongUnItem> itemType = KimJongUnItems.KimJongUnItem.fromId(args[2]);
            if (itemType.isPresent()) {
                ItemStack itemStack = items.createItem(itemType.get());
                target.getInventory().addItem(itemStack);
                sender.sendMessage("Gave " + itemType.get().displayName() + " to " + target.getName() + ".");
                return;
            }
            Optional<RadiationSuit.Piece> suitPiece = radiationSuit.pieceFromId(args[2]);
            if (suitPiece.isPresent()) {
                ItemStack itemStack = radiationSuit.createPiece(suitPiece.get());
                target.getInventory().addItem(itemStack);
                sender.sendMessage("Gave " + suitPiece.get().displayName() + " to " + target.getName() + ".");
                return;
            }
            sender.sendMessage("Unknown item. Try: " +
                    KimJongUnItems.KimJongUnItem.partItems().stream()
                            .map(KimJongUnItems.KimJongUnItem::id)
                            .collect(Collectors.joining(", ")) + ", missile, launchpad, radiation_helmet, "
                    + "radiation_chestplate, radiation_leggings, radiation_boots.");
            return;
        }
        sender.sendMessage("Unknown subcommand.");
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!canUse(sender)) {
            return List.of();
        }
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
            for (RadiationSuit.Piece piece : RadiationSuit.Piece.values()) {
                ids.add(piece.id());
            }
            return ids.stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public @NotNull String permission() {
        return "kimjongun3.admin";
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(permission());
    }

    public @NotNull KimJongUn3Plugin plugin() {
        return plugin;
    }
}
