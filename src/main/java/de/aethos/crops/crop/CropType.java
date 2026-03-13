package de.aethos.crops.crop;

import de.aethos.crops.AethosCrops;
import org.bukkit.NamespacedKey;

public enum CropType {
    WEIZEN("Weizen"),
    HAFER("Hafer"),
    KUERBIS("Kürbis");

    public static final NamespacedKey TYPE_KEY = new NamespacedKey(AethosCrops.getInstance(), "CropType");

    private final String name;

    CropType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
