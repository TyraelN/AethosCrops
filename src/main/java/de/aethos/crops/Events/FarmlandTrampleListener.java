package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FarmlandTrampleListener implements Listener {

    @EventHandler
    public void onFarmlandTrample(PlayerInteractEvent event) {
        // dismiss if action isnt trample
        if (event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // dismiss if block isnt FARMLAND
        if (block.getType() != Material.FARMLAND) return;

        Block cropBlock = block.getRelative(0, 1, 0);
        if (cropBlock.getType() != Material.WHEAT) return;

        DataManager dataManager = AethosCrops.getDataManager();
        if (!dataManager.hasCrop(cropBlock)) return;

        // Trampelschutz: Event abbrechen, Pflanze (und Farmland) bleiben stehen.
        if (AethosCrops.getConfigManager().isTrampleProtected()) {
            event.setCancelled(true);
            return;
        }

        Crop crop = dataManager.loadCrop(cropBlock);
        if (crop == null) {
            dataManager.removeCrop(cropBlock);
            AethosCrops.getDisplayManager().removeDisplay(cropBlock);
            return;
        }

        CropManager cropManager = AethosCrops.getCropManager();
        cropManager.dropBrokenCropLoot(cropBlock, crop);
        dataManager.removeCrop(cropBlock);
        AethosCrops.getDisplayManager().removeDisplay(cropBlock);
        cropBlock.setType(Material.AIR);
    }
}
