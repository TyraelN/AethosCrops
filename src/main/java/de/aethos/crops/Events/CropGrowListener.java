package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.concurrent.ThreadLocalRandom;

public class CropGrowListener implements Listener {

    @EventHandler
    public void onCropGrow(BlockGrowEvent event) {
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

        int growthSteps = resolveGrowthSteps(crop.getGrowSpeed());
        if (growthSteps <= 0) {
            event.setCancelled(true);
            return;
        }

        CropManager cropManager = AethosCrops.getCropManager();
        for (int i = 0; i < growthSteps; i++) {
            crop = cropManager.incrementStage(crop);
        }
        crop = cropManager.updateGen(crop);

        if (crop.isAtMaxStage()) {
            cropManager.setOriginalLastStage(event.getNewState());
        } else {
            cropManager.setOriginalFirstStage(event.getNewState());
        }

        dataManager.updateCrop(event.getBlock(), crop);
        AethosCrops.getDisplayManager().spawnOrUpdateDisplay(event.getBlock(), crop);
    }

    private int resolveGrowthSteps(double growSpeed) {
        if (growSpeed <= 0) {
            return 0;
        }

        int guaranteedGrowthSteps = (int) Math.floor(growSpeed);
        double fractionalGrowthChance = growSpeed - guaranteedGrowthSteps;

        if (ThreadLocalRandom.current().nextDouble() < fractionalGrowthChance) {
            guaranteedGrowthSteps++;
        }

        return guaranteedGrowthSteps;
    }
}
