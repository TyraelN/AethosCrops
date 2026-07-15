package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

public class CropBreakListener implements Listener {

    @EventHandler
    public void onCropBreak(BlockBreakEvent event) {

        // dismiss if block isnt WHEAT
        if (event.getBlock().getType() != Material.WHEAT) return;

        DataManager dataManager = AethosCrops.getDataManager();
        if (!dataManager.hasCrop(event.getBlock())) return;

        Crop crop = dataManager.loadCrop(event.getBlock());
        if (crop == null) {
            dataManager.removeCrop(event.getBlock());
            AethosCrops.getDisplayManager().removeDisplay(event.getBlock());
            return;
        }

        event.setDropItems(false);

        CropManager cropManager = AethosCrops.getCropManager();
        cropManager.dropBrokenCropLoot(event.getBlock(), crop);
        de.aethos.crops.Integration.AchievementsHook.increment(event.getPlayer(), "aethoscrops:harvested");

        dataManager.removeCrop(event.getBlock());
        AethosCrops.getDisplayManager().removeDisplay(event.getBlock());
    }
}
