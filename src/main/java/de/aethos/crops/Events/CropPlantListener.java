package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class CropPlantListener implements Listener {

    @EventHandler
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
    }
}
