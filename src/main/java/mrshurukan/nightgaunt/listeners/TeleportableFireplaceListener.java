package mrshurukan.nightgaunt.listeners;

import mrshurukan.nightgaunt.Nightgaunt;
import org.bukkit.Bukkit;

import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.ArrayList;

public class TeleportableFireplaceListener implements Listener {
    private final Nightgaunt plugin;

    public TeleportableFireplaceListener(Nightgaunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        // We only want the right click on the soul campfire
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.SOUL_CAMPFIRE) return;

        Player player = event.getPlayer();
        Inventory help = Bukkit.getServer().createInventory(player, 54, "Campfire Selector");

        //Here you define our item
        ItemStack ref1 = new ItemStack(Material.BOOK);
        ItemMeta metaref1 = ref1.getItemMeta();
        ArrayList<String> lore = new ArrayList<String>();

        lore.add(" ");
        lore.add("§for visit our site");
        lore.add(" ");
        lore.add("§atest.net/help");

        metaref1.setLore(lore);
        metaref1.setDisplayName("§6§lClick to get help");


        ref1.setItemMeta(metaref1);
        help.setItem(5, ref1);


        //Here opens the inventory
        player.openInventory(help);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {

        if (event.getClickedInventory() != null
                && event.getInventory().getHolder() != null
                && event.getView().getTitle().equalsIgnoreCase("Campfire Selector")) {

            event.setCancelled(true);
            Bukkit.broadcastMessage("You clicked custom inventory");
        }
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
                        // Bukkit.broadcastMessage("NUH-UH, NOT YET");
                        plugin.createNewTeleportableCampfire(block.getLocation());
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
