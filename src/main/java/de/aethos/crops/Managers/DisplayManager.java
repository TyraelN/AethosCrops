package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class DisplayManager {

    public void spawnOrUpdateDisplay(Block block, Crop crop) {
        if (block == null || crop == null) {
            return;
        }

        ItemDisplay display = getDisplay(block);
        if (display == null || !display.isValid()) {
            display = spawnDisplay(block);
            if (display == null) {
                return;
            }
        }

        display.setItemStack(createDisplayItem(crop));
    }

    public void removeDisplay(Block block) {
        if (block == null) {
            return;
        }

        ItemDisplay display = getDisplay(block);
        if (display == null) {
            return;
        }

        display.remove();
        AethosCrops.getDataManager().clearDisplayId(block);
    }

    private ItemDisplay spawnDisplay(Block block) {
        Location location = block.getLocation().toCenterLocation().add(0, -0.5, 0);
        Entity entity = block.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        if (!(entity instanceof ItemDisplay display)) {
            entity.remove();
            return null;
        }

        display.setBillboard(Display.Billboard.VERTICAL);
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(0.85f, 0.85f, 0.85f),
                new Quaternionf()
        ));

        AethosCrops.getDataManager().saveDisplayId(block, display.getUniqueId());
        return display;
    }

    private ItemDisplay getDisplay(Block block) {
        UUID displayId = AethosCrops.getDataManager().loadDisplayId(block);
        if (displayId == null) {
            return null;
        }

        Entity entity = block.getWorld().getEntity(displayId);
        if (entity instanceof ItemDisplay display) {
            return display;
        }

        AethosCrops.getDataManager().clearDisplayId(block);
        return null;
    }

    private ItemStack createDisplayItem(Crop crop) {
        ItemStack item = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.getPersistentDataContainer().set(
                CropKey.MODEL_PATH,
                PersistentDataType.STRING,
                crop.getModelPathForStage(crop.getStage())
        );
        item.setItemMeta(meta);
        return item;
    }
}
