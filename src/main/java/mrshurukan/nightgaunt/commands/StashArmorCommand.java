package mrshurukan.nightgaunt.commands;

import mrshurukan.nightgaunt.Nightgaunt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StashArmorCommand implements CommandExecutor {
    private final Nightgaunt plugin;

    // Constructor to link this command with the main plugin instance
    public StashArmorCommand(Nightgaunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle armor visibility.");
            return false;
        }

        Player player = (Player) sender;
        plugin.armorStash.hideArmor(player);  // Delegate the logic to the main plugin class
        return true;
    }
}