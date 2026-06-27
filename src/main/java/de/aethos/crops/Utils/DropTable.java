package de.aethos.crops.Utils;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Droptable Class containing drop entries
 * A droptable may contain the same DropEntry multiple
 * times to ensure multiple items of the same type to be dropped

 * If a specific drop should drop at least 2 times with 100% chance,
 * add it twice with a chance of 1, if the maximum amount of drops is 4,
 * add 2 more entries with a custom chance
 */

public class DropTable {

    private final List<DropEntry> entries = new ArrayList<>();

    public void add(ItemStack item, double chance) {
        entries.add(new DropEntry(item, chance));
    }

    public List<DropEntry> getEntries() {
        return entries;
    }
}