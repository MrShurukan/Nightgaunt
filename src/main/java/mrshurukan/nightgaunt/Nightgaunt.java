package mrshurukan.nightgaunt;

import mrshurukan.nightgaunt.commands.*;
import mrshurukan.nightgaunt.listeners.TeleportableCampfireListener;
import mrshurukan.nightgaunt.modules.ArmorStashModule;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfire;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfireModule;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public class Nightgaunt extends JavaPlugin {
    private FileConfiguration config;

    // Plugin modules
    public ArmorStashModule armorStash;
    public TeleportableCampfireModule teleportableCampfire;

    @Override
    public void onEnable() {
        // Register serializable stuff
        ConfigurationSerialization.registerClass(TeleportableCampfire.class);

        // Save the default configuration file if it doesn't exist
        saveDefaultConfig();
        config = getConfig();

        // Events
        getServer().getPluginManager().registerEvents(new TeleportableCampfireListener(this), this);

        // Commands
        CommandExecutor toggleArmorCommand = new ToggleArmorCommand(this);
        getCommand("togglearmor").setExecutor(toggleArmorCommand);

        CommandExecutor equipArmorCommand = new EquipArmorCommand(this);
        getCommand("equip").setExecutor(equipArmorCommand);

        CommandExecutor stashArmorCommand = new StashArmorCommand(this);
        getCommand("stash").setExecutor(stashArmorCommand);

        CommandExecutor debugCommand = new DebugCommand(this);
        getCommand("debugnightgaunt").setExecutor(debugCommand);

        armorStash = new ArmorStashModule(this, config);
        teleportableCampfire = new TeleportableCampfireModule(this, config);

        getLogger().info("Nightgaunt reporting for duty");
    }

    @Override
    public void onDisable() {
        // Save the player armor data to the config
        armorStash.saveArmorData();

        getLogger().info("Nightgaunt signing off");
    }
}
