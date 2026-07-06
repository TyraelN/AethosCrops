package de.aethos.crops.Utils;

import de.aethos.crops.AethosCrops;
import org.bukkit.NamespacedKey;

public class CropKey {
    public static final NamespacedKey CROP_TYPE = key("crop_type");
    public static final NamespacedKey SEED_GENES = key("seed_genes");
    public static final NamespacedKey DISEASE_TOOL = key("disease_tool");
    public static final NamespacedKey DISEASE_GUI_ITEM = key("disease_gui_item");
    public static final NamespacedKey BUGS_TOOL = key("bugs_tool");
    public static final NamespacedKey FUNGAL_TOOL = key("fungal_tool");
    public static final NamespacedKey WEED_TOOL = key("weed_tool");
    public static final NamespacedKey MODEL_PATH = key("model_path");

    private static NamespacedKey key(String key) {
        return new NamespacedKey(AethosCrops.getInstance(), key);
    }
}
