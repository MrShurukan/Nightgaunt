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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.units.qual.N;

import javax.management.InstanceNotFoundException;
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

        Bukkit.broadcastMessage("A new campfire was just created!");
    }

    public static String getBaseConfigPath() {
        return "teleportableCampfires";
    }

    public List<TeleportableCampfire> getCampfireList() {
        List<TeleportableCampfire> list = (List<TeleportableCampfire>) plugin.getConfig().getList(getBaseConfigPath() + ".campfires");

        if (list == null) return new ArrayList<>();
        return list;
    }

    public static Optional<TeleportableCampfire> findCampfireById(List<TeleportableCampfire> list, int id) {
        return list.stream().filter(x -> x.getId() == id).findAny();
    }

    public Optional<TeleportableCampfire> findCampfireById(int id) {
        List<TeleportableCampfire> campfires = getCampfireList();
        return findCampfireById(campfires, id);
    }

    public static Optional<TeleportableCampfire> findCampfireByLocation(List<TeleportableCampfire> list, Location location) {
        return list.stream().filter(x -> x.getLocation().equals(location)).findAny();
    }

    public Optional<TeleportableCampfire> findCampfireByLocation(Location location) {
        List<TeleportableCampfire> campfires = getCampfireList();
        return findCampfireByLocation(campfires, location);
    }

    public void updateCampfireInConfig(TeleportableCampfire campfire) throws InstanceNotFoundException {
        String basePath = getBaseConfigPath();
        List<TeleportableCampfire> campfires = (List<TeleportableCampfire>) config.getList(basePath + ".campfires");

        // Find the campfire to update
        for (int i = 0; i < campfires.size(); i++) {
            TeleportableCampfire c = campfires.get(i);
            if (c.getId() == campfire.getId()) {
                campfires.set(i, campfire);
                config.set(getBaseConfigPath() + ".campfires", campfires);
                plugin.saveConfig();

                return;
            }
        }

        throw new InstanceNotFoundException("Can't find the campfire with id: " + campfire.getId());
    }

    public void teleportPlayer(Player player, int campfireId) {
        Optional<TeleportableCampfire> campfireOptional = findCampfireById(campfireId);

        if (!campfireOptional.isPresent()) {
            player.sendMessage("Can't find campfire with id " + campfireId);
            return;
        }

        TeleportableCampfire campfire = campfireOptional.get();

        // First let's get some effects on the player
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 1000, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 1.5f);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Wait for 3 seconds
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                player.removePotionEffect(PotionEffectType.NAUSEA);
                // Play the sound at the original place
                playTeleportSound(player);
                // Teleport
                // Add 0.5, 0, 0.5 so you get teleported to the center of the block
                player.teleport(campfire.getTeleportPoint().clone().add(0.5, 0, 0.5));
                // Play sound at the new place
                playTeleportSound(player);

                return null;
            });
        });
    }

    private static void playTeleportSound(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.1f, 0.7f);
    }

    public void sitNear(Player player, int campfireId) {
        player.sendMessage("Just imagine you are sitting rn");
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));;
    }
}
