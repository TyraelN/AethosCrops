package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

public class WaterDestroyCropListener implements Listener {

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        // dismiss if block isnt WHEAT
        if (event.getBlock().getType() != Material.WHEAT) return;

        Block below = event.getBlock().getRelative(0, -1, 0);
        if (below.getType() == Material.FARMLAND) return;

        DataManager dataManager = AethosCrops.getDataManager();
        if (!dataManager.hasCrop(event.getBlock())) return;

        Crop crop = dataManager.loadCrop(event.getBlock());
        if (crop == null) {
            dataManager.removeCrop(event.getBlock());
            AethosCrops.getDisplayManager().removeDisplay(event.getBlock());
            return;
        }

        CropManager cropManager = AethosCrops.getCropManager();
        cropManager.dropBrokenCropLoot(event.getBlock(), crop);
        dataManager.removeCrop(event.getBlock());
        AethosCrops.getDisplayManager().removeDisplay(event.getBlock());
        event.getBlock().setType(Material.AIR);
    }
}
