package de.aethos.crops.Utils;

import org.bukkit.inventory.ItemStack;

/**
 * Dropentry class containing Item and Chance

 * Chance is a floating number percent value

 * 100% chance = 1
 * 57% chance = 0.57
 */

public class DropEntry {

    private final ItemStack item;
    private final double chance;

    public DropEntry(ItemStack item, double chance) {
        this.item = item;
        this.chance = chance;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getChance() {
        return chance;
    }
}
