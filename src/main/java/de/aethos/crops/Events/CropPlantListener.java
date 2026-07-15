package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CropPlantListener implements Listener {

    // HIGH + ignoreCancelled: erst nach Schutz-Plugins reagieren, damit keine
    // Crop-Daten fuer nie platzierte Bloecke entstehen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropPlant(BlockPlaceEvent event) {
        // dismiss if block isnt WHEAT
        if (event.getBlock().getType() != Material.WHEAT) return;

        ItemStack itemInHand = event.getItemInHand();

        CropManager cropManager = AethosCrops.getCropManager();
        if (!cropManager.isValidSeed(itemInHand)) return;

        Crop crop = cropManager.toCrop(itemInHand);
        if (crop == null) return;

        cropManager.setOriginalFirstStage(event.getBlock());
        AethosCrops.getDataManager().saveCrop(event.getBlock(), crop);
        AethosCrops.getDisplayManager().spawnOrUpdateDisplay(event.getBlock(), crop);
        de.aethos.crops.Integration.AchievementsHook.increment(event.getPlayer(), "aethoscrops:planted");

        // Vanilla verbraucht den Samen erst nach dem Event - danach die
        // Gen-Liste des Rest-Stacks auf die neue Stack-Groesse kuerzen
        // (der gepflanzte Samen war der letzte Eintrag).
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        Bukkit.getScheduler().runTask(AethosCrops.getInstance(), () -> {
            PlayerInventory inventory = player.getInventory();
            ItemStack remaining = hand == EquipmentSlot.OFF_HAND
                    ? inventory.getItemInOffHand()
                    : inventory.getItemInMainHand();

            if (AethosCrops.getSeedItemManager().trimToAmount(remaining)) {
                if (hand == EquipmentSlot.OFF_HAND) {
                    inventory.setItemInOffHand(remaining);
                } else {
                    inventory.setItemInMainHand(remaining);
                }
            }
        });
    }
}
