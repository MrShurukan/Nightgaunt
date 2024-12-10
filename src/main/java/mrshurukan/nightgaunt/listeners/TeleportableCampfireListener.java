package mrshurukan.nightgaunt.listeners;

import mrshurukan.nightgaunt.Nightgaunt;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfire;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfireModule;
import org.bukkit.*;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.messaging.MessageTooLargeException;

import java.util.*;
import java.util.stream.Collectors;

public class TeleportableCampfireListener implements Listener {
    private final Nightgaunt plugin;

    public TeleportableCampfireListener(Nightgaunt plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        Player playerOwner = null;
        if (item.getThrower() != null)
            playerOwner = Bukkit.getPlayer(item.getThrower());

        if (playerOwner == null || item.getItemStack().getType() != Material.GHAST_TEAR) return;

        Player finalPlayerOwner = playerOwner;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Do 3 tries to see if ghast tear is above the campfire
            for (int tryNumber = 1; tryNumber <= 3; tryNumber++) {
                // Check if the tear is above the campfire
                Location location = item.getLocation();

                Block block = location.getWorld()
                        .getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                // Jackpot!
                if (block.getType() == Material.CAMPFIRE) {
                    Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                        plugin.teleportableCampfire.createNewTeleportableCampfire(block.getLocation(), finalPlayerOwner);
                        item.remove();
                        return null;
                    });

                    return;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        // We only want the right click on the soul campfire
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.SOUL_CAMPFIRE) return;

        // Check if this soul campfire is registered
        List<TeleportableCampfire> campfires = plugin.teleportableCampfire.getCampfireList();
        Location clickLocation = event.getClickedBlock().getLocation();
        Optional<TeleportableCampfire> selectedCampfire
                = TeleportableCampfireModule.findCampfireByLocation(campfires, clickLocation);

        // If this was just a regular soul campfire - we exit
        if (!selectedCampfire.isPresent()) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        Inventory help = Bukkit.getServer().createInventory(player, 54, "Campfire Selector");

        int slotNumber = 0;
        for (TeleportableCampfire campfire : campfires) {
            ItemStack itemStack = prepareMenuItem(campfire, selectedCampfire.get());
            help.setItem(slotNumber++, itemStack);
        }

        //Here opens the inventory
        player.openInventory(help);
    }

    @EventHandler
    private void onSignChangeEvent(SignChangeEvent signChangeEvent) {
        int x = signChangeEvent.getBlock().getX();
        int y = signChangeEvent.getBlock().getY();
        int z = signChangeEvent.getBlock().getZ();

        int campfireId = plugin.getConfig().getInt(TeleportableCampfireModule.getBaseConfigPath()
                + getRenameSignsCampfireIdKey(x, y, z), -1);

        if (campfireId == -1)
            return;

        Optional<TeleportableCampfire> campfireOptional = plugin.teleportableCampfire.findCampfireById(campfireId);

        if (!campfireOptional.isPresent()) {
            signChangeEvent.getPlayer().sendMessage("Can't find a campfire with id: " + campfireId);
            return;
        }

        String[] lines = signChangeEvent.getLines();
        String newName = String.join(" ", lines).trim();

        TeleportableCampfire campfire = campfireOptional.get();
        try {
            campfire.setName(newName);
            plugin.teleportableCampfire.updateCampfireInConfig(campfire);
        } catch (Exception e) {
            signChangeEvent.getPlayer().sendMessage("Error: " + e.getMessage());
        }

        signChangeEvent.getPlayer().getWorld().playSound(
                signChangeEvent.getPlayer().getLocation(),
                Sound.BLOCK_AMETHYST_CLUSTER_HIT,
                0.5f,
                2f
        );
        signChangeEvent.getBlock().setType(Material.AIR);
        signChangeEvent.getPlayer().sendMessage(
                String.format("Renamed! New name is: §6§l'%s'", newName)
        );
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {

        if (event.getClickedInventory() != null
                && event.getInventory().getHolder() != null
                && event.getView().getTitle().equalsIgnoreCase("Campfire Selector")) {

            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                sendErrorAndCloseInventory(event, "There was an error processing clicked campfire " +
                        "(clickedItem == null)");
                return;
            }

            int campfireId = clickedItem.getItemMeta()
                    .getPersistentDataContainer()
                    .getOrDefault(createNamespacedKeyCampfireId(), PersistentDataType.INTEGER, -1);

            if (campfireId == -1) {
                sendErrorAndCloseInventory(event, "Error: custom inventory item doesn't contain a campfireId");
                return;
            }

            int clickedCampfireId = clickedItem.getItemMeta()
                    .getPersistentDataContainer()
                    .getOrDefault(createNamespacedKeyCampfireClickedId(), PersistentDataType.INTEGER, -1);

            if (clickedCampfireId == -1) {
                sendErrorAndCloseInventory(event, "Error: custom inventory item doesn't contain a clickedCampfireId");
                return;
            }

            NamespacedKey keyThisCampfire = createNamespacedKeyIsThisCampfire();
            boolean isThisCampfire = clickedItem.getItemMeta()
                    .getPersistentDataContainer()
                    .getOrDefault(keyThisCampfire, PersistentDataType.BOOLEAN, false);

            Player player = (Player) event.getWhoClicked();
            if (event.getClick().isRightClick()) {
                // Rename this campfire
                Optional<TeleportableCampfire> campfireToRenameOptional = plugin.teleportableCampfire.findCampfireById(campfireId);

                // We need this to spawn a sign above the clicked campfire
                Optional<TeleportableCampfire> campfireClickedOnOptional = plugin.teleportableCampfire.findCampfireById(clickedCampfireId);

                if (!campfireToRenameOptional.isPresent()) {
                    player.closeInventory();
                    player.sendMessage("Can't find campfire with id " + campfireId);
                    return;
                }

                if (!campfireClickedOnOptional.isPresent()) {
                    player.closeInventory();
                    player.sendMessage("Can't find campfire with id " + campfireId);
                    return;
                }

                TeleportableCampfire campfireToRename = campfireToRenameOptional.get();
                TeleportableCampfire campfireClickedOn = campfireClickedOnOptional.get();
                Location signLocation = campfireClickedOn.getLocation().clone().add(0, 1, 0);

                signLocation.getBlock().setType(Material.OAK_SIGN);
                Sign sign = (Sign) signLocation.getBlock().getState();

                SignSide[] signSides = { sign.getSide(Side.FRONT), sign.getSide(Side.BACK) };

                // Max length for a single line is 15
                ArrayList<String> splitCampfireName = splitStringBySize(campfireToRename.getName(), 15);

                for (SignSide signSide : signSides) {
                    for (int i = 0; i < splitCampfireName.size(); i++) {
                        signSide.setLine(i, splitCampfireName.get(i));
                    }
                }
                sign.update();

                int x = sign.getX();
                int y = sign.getY();
                int z = sign.getZ();
                plugin.getConfig().set(TeleportableCampfireModule.getBaseConfigPath()
                        + getRenameSignsCampfireIdKey(x, y, z), campfireToRename.getId());
                plugin.saveConfig();

                player.sendMessage("Please edit the sign above the campfire");
            }
            else {
                // Other ways of interacting will be the same as left click
                if (isThisCampfire) {
                    plugin.teleportableCampfire.sitNear(player, campfireId);
                } else {
                    plugin.teleportableCampfire.teleportPlayer(player, campfireId);
                }
            }

            player.closeInventory();
        }
    }

    private ItemStack prepareMenuItem(TeleportableCampfire campfire, TeleportableCampfire selectedCampfire) {
        ItemStack itemStack = new ItemStack(Material.SOUL_CAMPFIRE);
        ItemMeta itemStackMeta = itemStack.getItemMeta();

        ArrayList<String> lore = new ArrayList<>();

        lore.add(String.format("Created by %s", campfire.getOwnerName()));
        boolean isThisCampfire = campfire.getId() == selectedCampfire.getId();
        if (isThisCampfire) {
            lore.add(" ");
            lore.add("§CYou are looking at this campfire!");
        }

        itemStackMeta.setLore(lore);
        itemStackMeta.setDisplayName(String.format("§6§l%s", campfire.getName()));

        NamespacedKey keyId = createNamespacedKeyCampfireId();
        itemStackMeta.getPersistentDataContainer().set(keyId, PersistentDataType.INTEGER, campfire.getId());

        NamespacedKey keyIsThisCampfire = createNamespacedKeyIsThisCampfire();
        itemStackMeta.getPersistentDataContainer().set(keyIsThisCampfire, PersistentDataType.BOOLEAN, isThisCampfire);

        NamespacedKey keyCampfireClickedId = createNamespacedKeyCampfireClickedId();
        itemStackMeta.getPersistentDataContainer()
                .set(keyCampfireClickedId, PersistentDataType.INTEGER, selectedCampfire.getId());

        itemStack.setItemMeta(itemStackMeta);
        return itemStack;
    }

    private NamespacedKey createNamespacedKeyCampfireId() {
        return new NamespacedKey(plugin, "campfireId");
    }
    private NamespacedKey createNamespacedKeyIsThisCampfire() {
        return new NamespacedKey(plugin, "isThisCampfire");
    }
    private NamespacedKey createNamespacedKeyCampfireClickedId() {
        return new NamespacedKey(plugin, "campfireClickedId");
    }

    private static String getRenameSignsCampfireIdKey(int x, int y, int z) {
        return String.format(".renameSigns.%d:%d:%d.campfireId", x, y, z);
    }

    private static ArrayList<String> splitStringBySize(String str, int size) {
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / size; i++) {
            split.add(str.substring(i * size, Math.min((i + 1) * size, str.length())));
        }
        return split;
    }

    private void sendErrorAndCloseInventory(InventoryClickEvent event, String message) {
        event.getWhoClicked().sendMessage(message);
        event.getWhoClicked().closeInventory();
    }
}
