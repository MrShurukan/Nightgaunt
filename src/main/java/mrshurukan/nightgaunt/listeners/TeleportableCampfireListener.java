package mrshurukan.nightgaunt.listeners;

import mrshurukan.nightgaunt.Nightgaunt;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfire;
import mrshurukan.nightgaunt.modules.teleportablecampfires.TeleportableCampfireModule;
import org.bukkit.Bukkit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeleportableCampfireListener implements Listener {
    private final Nightgaunt plugin;

    public TeleportableCampfireListener(Nightgaunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        // We only want the right click on the soul campfire
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.SOUL_CAMPFIRE) return;

        // Check if this soul campfire is registered
        List<TeleportableCampfire> campfires = TeleportableCampfireModule.getCampfireList(plugin);
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

        NamespacedKey keyThisCampfire = createNamespacedKeyIsThisCampfire();
        itemStackMeta.getPersistentDataContainer().set(keyThisCampfire, PersistentDataType.BOOLEAN, isThisCampfire);

        itemStack.setItemMeta(itemStackMeta);
        return itemStack;
    }

    private NamespacedKey createNamespacedKeyCampfireId() {
        return new NamespacedKey(plugin, "campfireId");
    }
    private NamespacedKey createNamespacedKeyIsThisCampfire() {
        return new NamespacedKey(plugin, "isThisCampfire");
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

            NamespacedKey keyId = createNamespacedKeyCampfireId();
            int campfireId = clickedItem.getItemMeta()
                    .getPersistentDataContainer()
                    .getOrDefault(keyId, PersistentDataType.INTEGER, -1);

            if (campfireId == -1) {
                sendErrorAndCloseInventory(event, "Error: custom inventory item doesn't contain a campfireId");
                return;
            }

            NamespacedKey keyThisCampfire = createNamespacedKeyIsThisCampfire();
            boolean isThisCampfire = clickedItem.getItemMeta()
                    .getPersistentDataContainer()
                    .getOrDefault(keyThisCampfire, PersistentDataType.BOOLEAN, false);

            Player player = (Player) event.getWhoClicked();

            if (isThisCampfire) {
                plugin.teleportableCampfire.sitNear(player, campfireId);
            }
            else {
                plugin.teleportableCampfire.teleportPlayer(player, campfireId);
            }

            player.closeInventory();
        }
    }

    private void sendErrorAndCloseInventory(InventoryClickEvent event, String message) {
        event.getWhoClicked().sendMessage(message);
        event.getWhoClicked().closeInventory();
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
}
