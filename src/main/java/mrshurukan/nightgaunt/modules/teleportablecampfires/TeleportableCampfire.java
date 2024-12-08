package mrshurukan.nightgaunt.modules.teleportablecampfires;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TeleportableCampfire implements ConfigurationSerializable {
    private final int id;
    private final UUID ownerUUID;
    private String name;
    private final Location location;
    private Location teleportPoint;

    public TeleportableCampfire(int id, String name, Block campfireBlock, Player owner) throws Exception {

        if (campfireBlock.getBlockData().getMaterial() != Material.SOUL_CAMPFIRE) {
            throw new Exception("Can't create teleportable fireplace not on a soul campfire");
        }

        this.id = id;
        this.name = name;
        this.ownerUUID = owner.getUniqueId();
        location = campfireBlock.getLocation();

        locateTeleportPoint(owner);
        Bukkit.broadcastMessage(String.format("Hi! I'm a campfire, I was created.\n%s", this));
    }

    private TeleportableCampfire(int id, String name, UUID ownerUUID, Location location, Location teleportPoint) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.location = location;
        this.teleportPoint = teleportPoint;
    }

    private void locateTeleportPoint(Player owner) throws Exception {
        for (int yOffset = -2; yOffset <= 2; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    if (xOffset == 0 && yOffset == 0 && zOffset == 0) continue;

                    // We are interested in blocks player can stand on
                    Location seekLocation = location.clone().add(xOffset, yOffset, zOffset);
                    if (seekLocation.getBlock().isPassable()) continue;

                    Bukkit.broadcastMessage(
                            String.format("Analyzing block: %s %d %d %d",
                                    seekLocation.getBlock().getType(),
                                    (int)seekLocation.getX(), (int)seekLocation.getY(), (int)seekLocation.getZ())
                    );

                    // If the block is not passable we need to see if you can stand on top
                    // (i.e. there are at least two passable blocks above)
                    if (seekLocation.clone().add(0, 1, 0).getBlock().isPassable() &&
                            seekLocation.clone().add(0, 2, 0).getBlock().isPassable()) {
                        teleportPoint = seekLocation;
                        teleportPoint.add(0, 1, 0);
                        return;
                    }
                }
            }
        }

        String errorMessage = "Can't create a fireplace at this location! There are no standable blocks in a 2 block radius";
        owner.sendMessage(errorMessage);
        throw new Exception(errorMessage);
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Location getLocation() {
        return location;
    }
    public Location getTeleportPoint() {
        return teleportPoint;
    }
    public int getId() {
        return id;
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "id", id,
                "name", name,
                "ownerUUID", ownerUUID.toString(),
                "location", location,
                "teleportPoint", teleportPoint
        );
    }

    public static TeleportableCampfire deserialize(Map<String, Object> configObject) {
        return new TeleportableCampfire(
                (Integer) configObject.get("id"),
                (String) configObject.get("name"),
                UUID.fromString((String) configObject.get("ownerUUID")),
                (Location) configObject.get("location"),
                (Location) configObject.get("teleportPoint"));
    }

    public String toString() {
        return String.format("TeleportableCampfire { id: %d, name: %s, location: { x: %f y: %f z: %f }, teleportPoint: { x: %f y: %f z: %f } }",
                id, name,
                location.getX(), location.getY(), location.getZ(),
                teleportPoint.getX(), teleportPoint.getY(), teleportPoint.getZ());
    }
}
