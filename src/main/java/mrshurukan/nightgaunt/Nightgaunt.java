package mrshurukan.nightgaunt;

import mrshurukan.nightgaunt.commands.*;
import mrshurukan.nightgaunt.listeners.TeleportableFireplaceListener;
import mrshurukan.nightgaunt.modules.ArmorStash;
import mrshurukan.nightgaunt.modules.TeleportableFireplace;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Nightgaunt extends JavaPlugin {
    private FileConfiguration config;

    // Plugin modules
    public ArmorStash armorStash;
    public TeleportableFireplace teleportableFireplace;

    @Override
    public void onEnable() {
        // Save the default configuration file if it doesn't exist
        saveDefaultConfig();
        config = getConfig();

        // Events
        getServer().getPluginManager().registerEvents(new TeleportableFireplaceListener(this), this);

        // Commands
        CommandExecutor toggleArmorCommand = new ToggleArmorCommand(this);
        getCommand("togglearmor").setExecutor(toggleArmorCommand);

        CommandExecutor equipArmorCommand = new EquipArmorCommand(this);
        getCommand("equip").setExecutor(equipArmorCommand);

        CommandExecutor stashArmorCommand = new StashArmorCommand(this);
        getCommand("stash").setExecutor(stashArmorCommand);

        armorStash = new ArmorStash(this, config);
        teleportableFireplace = new TeleportableFireplace(this, config);

        getLogger().info("Nightgaunt reporting for duty");
    }

    @Override
    public void onDisable() {
        // Save the player armor data to the config
        armorStash.saveArmorData();

        getLogger().info("Nightgaunt signing off");
    }

    public void createNewTeleportableCampfire(Location location) {
        if (location.getBlock().getType() != Material.CAMPFIRE)
        {
            Bukkit.broadcastMessage("Can't create a teleportable campfire here (Not a campfire!)");
            return;
        }

        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 0.25f);
        location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 3f);

        BlockFace blockFace = ((Directional)location.getBlock().getBlockData()).getFacing();
        location.getBlock().setType(Material.SOUL_CAMPFIRE);

        Directional directionalBlockData = (Directional)location.getBlock().getBlockData();
        directionalBlockData.setFacing(blockFace);

        location.getBlock().setBlockData(directionalBlockData);
    }
}
