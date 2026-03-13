package de.aethos.crops.gui;

import de.aethos.crops.PlayerItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class GUIKÄFER extends GUIMaker{
    public static Component title = Component.text("Töte die Käfer!");
    public Inventory GUIKäfer = Bukkit.createInventory(null, 54, "Töte die Käfer!");
    public static ItemStack PLACEHOLDER = createGUIItems(Material.GRAY_STAINED_GLASS_PANE, " ");
    public static ItemStack PFLANZE = createGUIItems(Material.FERN, " ");
    public static ItemStack KÄFER = createGUIItems(Material.STONE, ChatColor.RED + "Käfer");
    public static ItemStack P = createGUIItems(Material.DIRT, "Wenn du das Liest ist ein Fehler aufgetreten");

    public GUIKÄFER() {
        ItemStack[] CONTENT = {
                PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PlayerItems.getPestizit(), PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER,
                PLACEHOLDER, P, P, P, P, KÄFER, P, P, PLACEHOLDER,
                PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
                PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
                PLACEHOLDER, P, P, P, P, P, P, P, PLACEHOLDER,
                PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER
        };
        GUIKäfer.setContents(CONTENT);
        ArrayList<ItemStack> stacks = randomItemStackList(PFLANZE, KÄFER, 27);
        stacks.forEach((n) -> {
            int position = GUIKäfer.first(P);
            if(position <= 0){
                return;
            }
            GUIKäfer.setItem(position, n);
        });
    }
}
