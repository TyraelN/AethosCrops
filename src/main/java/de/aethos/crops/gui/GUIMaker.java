package de.aethos.crops.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class GUIMaker {
    public GUIMaker(){}

    public ArrayList<ItemStack> randomItemStackList(ItemStack a, ItemStack b, int anzahl) {
        ArrayList<ItemStack> ergebnis = new ArrayList<>();
        while(anzahl > 0) {

            double random = Math.random();
            if (random < 0.5) {
                ergebnis.add(b);
            }
            else {
                ergebnis.add(a);
            }
            anzahl -= 1;
        }
        return ergebnis;
    }

    protected static ItemStack createGUIItems(final Material material, final String name, final String... Lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(Lore));
        item.setItemMeta(meta);
        return item;
    }
}
