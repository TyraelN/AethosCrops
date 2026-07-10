package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

/**
 * Baut Custom-Crops sauber ab, wenn ihr Block physikalisch zerstoert wird.
 * Zwei Pfade:
 * - BlockPhysicsEvent: der Traeger-Block (Farmland) ist weg -> Pflanze poppt.
 * - BlockFromToEvent: Fluessigkeit fliesst direkt IN den Pflanzen-Block
 *   (Farmland darunter noch intakt) - ohne diesen Handler blieben Crop-Daten
 *   und Display verwaist zurueck.
 * In beiden Faellen: Custom-Loot droppen, Daten + Display entfernen, Block
 * auf AIR setzen (verhindert den Vanilla-Drop).
 */
public class WaterDestroyCropListener implements Listener {

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        // dismiss if block isnt WHEAT
        if (event.getBlock().getType() != Material.WHEAT) return;

        Block below = event.getBlock().getRelative(0, -1, 0);
        if (below.getType() == Material.FARMLAND) return;

        breakCrop(event.getBlock());
    }

    // Wasser/Lava fliesst in den Pflanzen-Block selbst - das feuert KEIN
    // Physics-Event mit fehlendem Farmland, sondern ein FromTo-Event auf den
    // Zielblock. Die Fluessigkeit fliesst danach normal weiter.
    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        if (event.getToBlock().getType() != Material.WHEAT) return;

        breakCrop(event.getToBlock());
    }

    // Gemeinsamer Abbau-Pfad: Loot droppen, Daten + Display entfernen, AIR setzen.
    private void breakCrop(Block block) {
        DataManager dataManager = AethosCrops.getDataManager();
        if (!dataManager.hasCrop(block)) return;

        Crop crop = dataManager.loadCrop(block);
        if (crop == null) {
            dataManager.removeCrop(block);
            AethosCrops.getDisplayManager().removeDisplay(block);
            return;
        }

        CropManager cropManager = AethosCrops.getCropManager();
        cropManager.dropBrokenCropLoot(block, crop);
        dataManager.removeCrop(block);
        AethosCrops.getDisplayManager().removeDisplay(block);
        block.setType(Material.AIR);
    }
}
