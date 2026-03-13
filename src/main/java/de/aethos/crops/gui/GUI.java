package de.aethos.crops.gui;


import de.aethos.crops.crop.Crop;
import de.aethos.crops.crop.Gen;
import de.aethos.crops.crop.Krankheit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class GUI extends UI {
    public GUI(@NotNull Crop crop, Player player) {
        super(player, null);
        this.inv = getInventory(crop);
        player.openInventory(this);
    }

    public Inventory getInventory(Crop crop) {
        Inventory guilupe = Bukkit.createInventory(null, 54, "Plantstats");
        guilupe.addItem(createGUIItems(Material.DIAMOND, (crop.getType().getName()), "Dieser Crop hat folgende Gene"));
        for (Gen gen : Gen.values()) {
            guilupe.addItem(createGUIItems(Material.WHEAT_SEEDS, gen.getDisplayName(), crop.getGen(gen) + "/100"));
        }
        if (crop.hatKrankheit(Krankheit.KAEFER)) {
            guilupe.addItem(createGUIItems(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Die Pflanze leidet an Käferbefall"));
        }
        if (crop.hatKrankheit(Krankheit.PILZE)) {
            guilupe.addItem(createGUIItems(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Die Pflanze leidet an Pilzbefall"));
        }
        if (crop.hatKrankheit(Krankheit.UNKRAUT)) {
            guilupe.addItem(createGUIItems(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Die Pflanze leidet an Unkrautbefall"));
        }
        if (crop.hatKrankheit(Krankheit.GESUND)) {
            guilupe.addItem(createGUIItems(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Die Pflanze ist gesund"));
        }
        if (!crop.canGrow()) {
            guilupe.addItem(createGUIItems(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Pflanze wächst nicht!"));
        } else if (crop.isKrank()) {
            guilupe.addItem(createGUIItems(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Pflanze wächst nicht!"));
        } else
            guilupe.addItem(createGUIItems(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Pflanze wächst, Glückwunsch"));
        DateFormat dateFormat = new SimpleDateFormat();
        guilupe.addItem(createGUIItems(Material.FARMLAND, "Dieser Crop wurde gepflanzt am: ", dateFormat.format(new Date(crop.getZeitpunkt()))));
        return guilupe;
    }

    @Override
    public @NotNull String getTitle() {
        return "Lupe";
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getView() instanceof GUI)) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }
        event.setCancelled(true);

    }
}


