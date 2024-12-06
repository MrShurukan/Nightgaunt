package mrshurukan.nightgaunt.modules;

import jdk.jfr.internal.consumer.StringParser;
import mrshurukan.nightgaunt.Nightgaunt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TeleportableFireplace {
    private final Nightgaunt plugin;
    private final FileConfiguration config;

    public TeleportableFireplace(Nightgaunt plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }



    private String getBaseConfigBath() {
        return "teleportableFireplace.";
    }
}
