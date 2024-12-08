package mrshurukan.nightgaunt.modules.teleportablecampfires;

import mrshurukan.nightgaunt.Nightgaunt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TeleportableCampfireModule {
    private final Nightgaunt plugin;
    private final FileConfiguration config;

    public TeleportableCampfireModule(Nightgaunt plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void createNewTeleportableCampfire(Location location, Player owner) throws Exception {
        if (location.getBlock().getType() != Material.CAMPFIRE)
        {
            Bukkit.broadcastMessage("Can't create a teleportable campfire here (Not a campfire!)");
            return;
        }

        // Sound
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 0.25f);
        location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 3f);

        // Remember the original rotation
        BlockFace blockFace = ((Directional)location.getBlock().getBlockData()).getFacing();
        // Set to a soul campfire
        location.getBlock().setType(Material.SOUL_CAMPFIRE);

        // Restore the rotation
        Directional directionalBlockData = (Directional)location.getBlock().getBlockData();
        directionalBlockData.setFacing(blockFace);

        location.getBlock().setBlockData(directionalBlockData);

        // Register the fireplace
        String basePath = getBaseConfigPath();
        List<TeleportableCampfire> campfires = (List<TeleportableCampfire>) config.getList(basePath + ".campfires");
        int nextId = 0;
        if (campfires != null && !campfires.isEmpty())
            nextId = campfires.stream()
                .max(Comparator.comparingInt(TeleportableCampfire::getId))
                .get()
                .getId() + 1;

        TeleportableCampfire fireplace =
                new TeleportableCampfire(nextId, String.format("Campfire %d", nextId), location.getBlock(), owner);

        if (campfires == null)
            campfires = new ArrayList<>();
        campfires.add(fireplace);

        config.set(getBaseConfigPath() + ".campfires", campfires);
        plugin.saveConfig();
    }

    public static String getBaseConfigPath() {
        return "teleportableCampfires";
    }
}
