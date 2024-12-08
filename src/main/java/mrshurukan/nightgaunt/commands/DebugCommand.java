package mrshurukan.nightgaunt.commands;

import mrshurukan.nightgaunt.Nightgaunt;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfire;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfireModule;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class DebugCommand implements CommandExecutor {
    private final Nightgaunt plugin;

    // Constructor to link this command with the main plugin instance
    public DebugCommand(Nightgaunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        switch (args[0]) {
            case "campfire":
                if (args.length < 2) return false;
                return processCampfireCommand(commandSender, command, label, args);

            case "block":
                if (args.length < 2) return false;
                return processBlockCommand(commandSender, command, label, args);

            default:
                commandSender.sendMessage("Unknown argument: " + args[0]);
                return false;
        }
    }

    private boolean processCampfireCommand(CommandSender commandSender, Command command, String label, String[] args) {
        switch (args[1]) {
            case "list":
                List<TeleportableCampfire> campfires = TeleportableCampfireModule.getCampfireList(plugin);

                if (campfires != null)
                    commandSender.sendMessage(campfires.toString());
                else
                    commandSender.sendMessage("There are no saved campfires");

                return true;

            default:
                commandSender.sendMessage("Unknown argument: " + args[1]);
                return false;
        }
    }

    private boolean processBlockCommand(CommandSender commandSender, Command command, String label, String[] args) {
        switch (args[1]) {
            case "passable":
                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage("Only players can use this command");
                    return false;
                }

                Player player = (Player) commandSender;
                Block targetBlock = player.getTargetBlockExact(20);
                if (targetBlock == null) {
                    commandSender.sendMessage("You must target a block");
                    return false;
                }

                commandSender.sendMessage(String.format("This block is %s",
                        targetBlock.isPassable() ? "passable" : "NOT passable"));
                return true;

            default:
                commandSender.sendMessage("Unknown argument: " + args[0]);
                return false;
        }
    }
}
