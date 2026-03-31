package uk.greenparty.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.greenparty.GreenPartyPlugin;

/**
 * /achievements command — lists the player's achievements in the Verdant Utopia.
 *
 * The council has 14 achievement criteria pending approval.
 * Four have been ratified. Progress.
 */
public class AchievementsCommand implements CommandExecutor {

    private final GreenPartyPlugin plugin;

    public AchievementsCommand(GreenPartyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Achievements] §7This command is for players only.");
            sender.sendMessage("§8(Console has no achievements. Console only observes.)");
            return true;
        }

        plugin.getAchievementManager().listAchievements(player);
        return true;
    }
}
