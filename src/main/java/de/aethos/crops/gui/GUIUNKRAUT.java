package de.aethos.crops.gui;

import de.aethos.crops.PlayerItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class GUIUNKRAUT extends GUIMaker{
    public Inventory GUIUnkrauft = Bukkit.createInventory(null, 54, "Vernichte das Unkraut");
    public static ItemStack PLACEHOLDER = createGUIItems(Material.GRAY_STAINED_GLASS_PANE, " ");
    public static ItemStack NORMALE_PFLANZE = createGUIItems(Material.GRASS, " ");
    public static ItemStack UNKRAUT = createGUIItems(Material.GRASS, ChatColor.RED + "Unkraut");
    public static ItemStack P = createGUIItems(Material.DIRT, "Wenn du das Liest ist ein Fehler aufgetreten");
    private final ItemStack[] CONTENT = {
            PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PlayerItems.getUnkrautHacke(), PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER,
            PLACEHOLDER, P, P, UNKRAUT, P, P, P, P, PLACEHOLDER,
            PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
            PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
            PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
            PLACEHOLDER, PLACEHOLDER,PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER,
    };

    public GUIUNKRAUT() {
        GUIUnkrauft.setContents(CONTENT);
        ArrayList<ItemStack> stacks = randomItemStackList(NORMALE_PFLANZE, UNKRAUT, 27);
        stacks.forEach((n) -> {
            int position = GUIUnkrauft.first(P);
            if(position <= 0){
                return;
            }
            GUIUnkrauft.setItem(position, n);
        });
    }
}
