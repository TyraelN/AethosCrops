package de.aethos.crops.crop;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CropFactory {
    private CropFactory() {

    }

    public static @NotNull Crop getCrop(@NotNull Location loc, @NotNull ItemStack item) {
        return new Crop(loc, item);
    }

    public static @NotNull Crop getCrop(@NotNull Location loc, @NotNull CropType cropType, @Nullable Krankheit krankheit, @NotNull Map<Gen, Integer> genMap, long zeitpunkt) {
        Crop crop = new Crop(loc, CropItemFactory.getItem(cropType));
        if (krankheit != null) {
            crop.setKrankheit(krankheit);
        }
        crop.setZeitpunkt(zeitpunkt);
        crop.setGenMap(genMap);
        return crop;
    }


}
