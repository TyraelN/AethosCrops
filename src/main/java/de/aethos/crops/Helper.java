package de.aethos.crops;

import de.aethos.crops.crop.Crop;
import de.aethos.crops.crop.CropManager;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public final class Helper {
    private Helper() {

    }

    public static List<String> zusatzItems(String a, String b, int genlevel) {
        ArrayList<String> itemStacks = new ArrayList<>();
        //Level 1, Level 2-11, Level 12-21, level 22-31, level 32-41, level 42-51, level 52-61, level 62-71, level 72-81, level 82-91, level 92-99, level 100
        int umbruch = 2;
        int level = 1;
        while (umbruch <= genlevel) {
            if (level == 5 || level == 10 || level == 1) {
                itemStacks.add(b);
            }
            level += 1;
            umbruch += 10;
        }
        double probability = 1;
        while (level >= 0) {
            double rand = Math.random();
            if (rand <= probability) {
                itemStacks.add(a);
            }
            probability -= 0.08;
            level -= 1;
        }
        return itemStacks;
    }

    public static List<Crop> neighbourCheck(Location loc) {
        double X = loc.getX() + 1;
        double Z = loc.getZ() + 1;
        ArrayList<Crop> cro = new ArrayList<>();
        while (X >= loc.getX() - 1) {
            loc.set(X, loc.getY(), loc.getZ());
            if (CropManager.isCrop(loc)) {
                cro.add(CropManager.getCrop(loc));
            }
            X -= 2;
        }
        while (Z >= loc.getZ() - 1) {
            loc.set(loc.getX(), loc.getY(), Z);
            if (CropManager.isCrop(loc)) {
                cro.add(CropManager.getCrop(loc));
            }
            Z -= 2;
        }
        return cro;
    }
}
