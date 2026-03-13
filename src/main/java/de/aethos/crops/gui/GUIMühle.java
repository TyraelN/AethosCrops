package de.aethos.crops.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIMühle extends  GUIMaker{
    public Component title = Component.text("Mühle");
    public Inventory GUIMühle = Bukkit.createInventory(null, 54, "Mühle");
    public static ItemStack PLACEHOLDER = createGUIItems(Material.GRAY_STAINED_GLASS_PANE , ChatColor.BLACK + ".");
    public static ItemStack AIR = new ItemStack(Material.AIR);
    public static ItemStack PFEIL = createGUIItems(Material.ARROW, ChatColor.GREEN + "Wird Verarbeitet", "Klicken (Rechts)");
    public static ItemStack ANZEIGE_RED = createGUIItems(Material.RED_STAINED_GLASS_PANE, "");
    public static ItemStack ANZEIGE_GREEN = createGUIItems(Material.GREEN_STAINED_GLASS_PANE, "");
    public static ItemStack ERGEBNIS = createGUIItems(Material.ORANGE_STAINED_GLASS_PANE, "");
    public static final ItemStack[] CONTENT =
            {
            PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER,
            PLACEHOLDER, AIR, AIR, AIR, PLACEHOLDER, ERGEBNIS, ERGEBNIS, ERGEBNIS, PLACEHOLDER,
            PLACEHOLDER, AIR, AIR, AIR, PFEIL, ERGEBNIS, ERGEBNIS, ERGEBNIS, PLACEHOLDER,
            PLACEHOLDER, AIR, AIR, AIR, PLACEHOLDER, ERGEBNIS, ERGEBNIS, ERGEBNIS, PLACEHOLDER,
            PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, PLACEHOLDER,
                    ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED, ANZEIGE_RED
            };
    public GUIMühle() {
        GUIMühle.setContents(CONTENT);
    }
}
