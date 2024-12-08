package mrshurukan.nightgaunt.modules;

import mrshurukan.nightgaunt.Nightgaunt;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class ArmorStashModule {
    private final Nightgaunt plugin;
    private final FileConfiguration config;

    public ArmorStashModule(Nightgaunt plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void saveArmorData() {
        // This method will be called during plugin disable to save player armor data
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String basePath = getConfigBasePath(player);
            if (!config.getBoolean(basePath + ".hidden", false))
                storeArmorInConfig(player);
        }
    }

    public void toggleArmorVisibility(Player player) {
        String basePath = getConfigBasePath(player);
        boolean isHidden = config.getBoolean(basePath + ".hidden", false);

        if (isHidden) {
            // Show armor
            restoreArmor(player);
        } else {
            // Hide armor
            hideArmor(player);
        }
    }

    private void storeArmorInConfig(Player player) {
        // Store the player's current armor before hiding it
        ItemStack[] originalArmor = player.getInventory().getArmorContents();

        // getLogger().info(String.format("OriginalArmor length: %d", originalArmor.length));

        // Save the armor data to the config
        String basePath = getConfigBasePath(player);
        config.set(basePath + ".helmet", originalArmor[3]);
        config.set(basePath + ".chestplate", originalArmor[2]);
        config.set(basePath + ".leggings", originalArmor[1]);
        config.set(basePath + ".boots", originalArmor[0]);

        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        config.set(basePath + ".offhand", offhandItem);

        if (!config.contains(basePath + ".hidden"))
            config.set(basePath + ".hidden", false);

        plugin.saveConfig();
    }

    private String getConfigBasePath(Player player) {
        return "armor." + player.getUniqueId().toString();
    }

    public void hideArmor(Player player) {
        String basePath = getConfigBasePath(player);
        if (config.getBoolean(basePath + ".hidden", false)) {
            player.sendMessage("You already have your armor stashed, restore it first");
            return;
        }

        if (Arrays.stream(player.getInventory().getArmorContents()).allMatch(Objects::isNull)) {
            player.sendMessage("You don't have any armor to hide");
            return;
        }

        storeArmorInConfig(player);

        // Hide the armor by setting it to air (no item)
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        player.getInventory().setChestplate(new ItemStack(Material.AIR));
        player.getInventory().setLeggings(new ItemStack(Material.AIR));
        player.getInventory().setBoots(new ItemStack(Material.AIR));

        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        config.set(getConfigBasePath(player) + ".hidden", true);
        plugin.saveConfig();

        player.sendMessage("Your armor is now stashed.");
    }

    public void restoreArmor(Player player) {
        String basePath = getConfigBasePath(player);

        if (!config.getBoolean(basePath + ".hidden", false)) {
            player.sendMessage("You don't have anything stashed");
            return;
        }

        if (Arrays.stream(player.getInventory().getArmorContents()).anyMatch(Objects::nonNull)) {
            player.sendMessage("You have armor on you right now, take it off first");
            return;
        }

        if (config.contains(basePath)) {
            // Restore the original armor from the config
            ItemStack helmet = config.getItemStack(basePath + ".helmet");
            ItemStack chestplate = config.getItemStack(basePath + ".chestplate");
            ItemStack leggings = config.getItemStack(basePath + ".leggings");
            ItemStack boots = config.getItemStack(basePath + ".boots");

            ItemStack offhandItem = config.getItemStack(basePath + ".offhand");

            if (helmet != null)
                player.getInventory().setHelmet(helmet);
            if (chestplate != null)
                player.getInventory().setChestplate(chestplate);
            if (leggings != null)
                player.getInventory().setLeggings(leggings);
            if (boots != null)
                player.getInventory().setBoots(boots);

            if (offhandItem != null)
                player.getInventory().setItemInOffHand(offhandItem);

            config.set(basePath + ".hidden", false);
            plugin.saveConfig();

            player.sendMessage("Your armor is now equipped.");

            if (player.getName().contains("GenkiSuzune"))
                player.sendMessage("Good luck on your hunt, Red Hood!");

            return;
        }

        player.sendMessage("Can't restore armor - nothing is saved!");
    }
}
