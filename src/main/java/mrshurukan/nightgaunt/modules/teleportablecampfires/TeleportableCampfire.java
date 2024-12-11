package mrshurukan.nightgaunt.modules.teleportablecampfires;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.MessageTooLargeException;

import java.util.Map;
import java.util.UUID;

public class TeleportableCampfire implements ConfigurationSerializable {
    private final int id;
    private final UUID ownerUUID;
    private final String ownerName;
    private String name;
    private final Location location;
    private Location teleportPoint;

    public TeleportableCampfire(int id, String name, Block campfireBlock, Player owner) throws Exception {

        if (campfireBlock.getBlockData().getMaterial() != Material.CAMPFIRE) {
            throw new Exception("Can't create teleportable fireplace not on a campfire");
        }

        if (campfireBlock.getWorld().getEnvironment() != World.Environment.NORMAL) {

            int x = campfireBlock.getX();
            int y = campfireBlock.getY();
            int z = campfireBlock.getZ();
            // Create a TNT explosion (power = 4f)
            // And light blocks on fire (true)
            campfireBlock.getWorld().createExplosion(x, y + 0.5f, z, 4f, true);

            throw new Exception("Campfire was overpowered!\n[don't create campfires in the Nether or the End dummy]");
        }

        this.id = id;
        this.name = name;
        this.ownerUUID = owner.getUniqueId();
        this.ownerName = owner.getName();
        location = campfireBlock.getLocation();

        locateNewTeleportPoint();
    }

    private TeleportableCampfire(int id, String name, UUID ownerUUID, String ownerName, Location location, Location teleportPoint) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.location = location;
        this.teleportPoint = teleportPoint;
    }

    public boolean hasSolidBlockUnderTeleportPoint() {
        Block b = this.teleportPoint.clone().add(0, -1, 0).getBlock();
        if (b.isPassable())
            return false;

        return true;
    }

    public void locateNewTeleportPoint() throws Exception {
        for (int yOffset = -2; yOffset <= 2; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    if (xOffset == 0 && yOffset == 0 && zOffset == 0) continue;

                    // We are interested in blocks player can stand on
                    Location seekLocation = location.clone().add(xOffset, yOffset, zOffset);
                    if (seekLocation.getBlock().isPassable()) continue;

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

        throw new Exception("Can't create a fireplace at this location! " +
                "There are no standable blocks in a 2 block radius");
    }

    public String getName() {
        return name;
    }
    public void setName(String name) throws MessageTooLargeException {
        if (name.length() > 60) throw new MessageTooLargeException("Name can't be longer than 60 characters!");
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
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "id", id,
                "name", name,
                "ownerUUID", ownerUUID.toString(),
                "ownerName", ownerName,
                "location", location,
                "teleportPoint", teleportPoint
        );
    }

    public static TeleportableCampfire deserialize(Map<String, Object> configObject) {
        return new TeleportableCampfire(
                (Integer) configObject.get("id"),
                (String) configObject.get("name"),
                UUID.fromString((String) configObject.get("ownerUUID")),
                (String) configObject.get("ownerName"),
                (Location) configObject.get("location"), (Location) configObject.get("teleportPoint"));
    }

    public String toString() {
        return String.format("TeleportableCampfire { id: %d, name: %s, location: { x: %f y: %f z: %f }, teleportPoint: { x: %f y: %f z: %f } }",
                id, name,
                location.getX(), location.getY(), location.getZ(),
                teleportPoint.getX(), teleportPoint.getY(), teleportPoint.getZ());
    }
}
