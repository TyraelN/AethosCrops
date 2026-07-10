package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class DisplayManager {

    // Die Crop-Modelle im Resource Pack haben die Stadien 0..7 (range_dispatch).
    private static final int MODEL_STAGE_MAX = 7;

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

        applyPresentation(display, block);
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
        Location location = block.getLocation().toCenterLocation();
        Entity entity = block.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        if (!(entity instanceof ItemDisplay display)) {
            entity.remove();
            return null;
        }

        AethosCrops.getDataManager().saveDisplayId(block, display.getUniqueId());
        return display;
    }

    // Block-Modelle sitzen fest wie ein Block: kein Billboard, volle Groesse,
    // Blockmitte. Wird auch bei Updates angewendet, damit Alt-Displays
    // (Billboard/0.85er-Skalierung/Bodenposition der Platzhalter-Zeit) migrieren.
    private void applyPresentation(ItemDisplay display, Block block) {
        display.setBillboard(Display.Billboard.FIXED);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        ));

        Location center = block.getLocation().toCenterLocation();
        if (display.getLocation().distanceSquared(center) > 0.01D) {
            display.teleport(center);
        }
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

    // Display-Item nach dem Resource-Pack-Schema: Stick mit item_model des Crops
    // (z.B. "aethos:block/crops/barley"); das Wachstums-Stadium 0..7 waehlt der
    // range_dispatch im Pack ueber den custom_model_data-Float.
    private ItemStack createDisplayItem(Crop crop) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setItemModel(NamespacedKey.fromString(crop.getItemModel()));
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        modelData.setFloats(List.of((float) crop.scaleStageTo(MODEL_STAGE_MAX)));
        meta.setCustomModelDataComponent(modelData);
        item.setItemMeta(meta);
        return item;
    }
}
