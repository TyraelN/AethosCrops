package de.aethos.crops.gui;


import de.aethos.crops.AethosCrops;
import de.aethos.crops.crop.CropType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class MühlenUI extends UI implements Listener {
    private static final ArrayList<Integer> ERGEBNIS_SLOTS = new ArrayList<>(List.of(14, 15, 16, 23, 24, 25, 32, 33, 34));
    private boolean isProssesing = false;

    public MühlenUI(Player player) {
        super(player, null);
        GUIMühle guiMühle = new GUIMühle();
        this.inv = guiMühle.GUIMühle;
        player.openInventory(this);
    }

    public MühlenUI() {
        super(null, null);
    }

    @Override
    public @NotNull String getTitle() {
        return "Mühle";
    }

    public void onClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) {
            return;
        }
        if (currentItem.equals(GUIMühle.ANZEIGE_RED) || currentItem.equals(GUIMühle.ERGEBNIS) || currentItem.equals(GUIMühle.ANZEIGE_GREEN) || currentItem.equals(GUIMühle.PLACEHOLDER) || currentItem.equals(GUIMühle.PFEIL)) {
            event.setCancelled(true);
            return;
        }
        if (currentItem.getType().equals(Material.SUGAR) && event.getView().getTopInventory().equals(event.getClickedInventory()) && ERGEBNIS_SLOTS.contains(event.getSlot())) {
            int i = event.getSlot();
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getInventory().setItem(i, GUIMühle.ERGEBNIS);
                }
            }.runTask(AethosCrops.getInstance());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void seedProssesing(InventoryClickEvent event) {
        if (!(event.getView() instanceof MühlenUI ui)) {
            return;
        }
        if (ui.isProssesing) {
            return;
        }
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }
        Inventory inventory = event.getInventory();
        ui.isProssesing = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = inventory.first(GUIMühle.ANZEIGE_RED);
                if (i < 0) {
                    while (inventory.first(GUIMühle.ANZEIGE_GREEN) >= 0) {
                        int i1 = inventory.first(GUIMühle.ANZEIGE_GREEN);
                        inventory.setItem(i1, GUIMühle.ANZEIGE_RED);
                    }
                    return;
                }
                inventory.setItem(i, GUIMühle.ANZEIGE_GREEN);
            }
        }.runTaskTimer(AethosCrops.getInstance(), 2, 2);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inventory.getViewers().isEmpty()) {
                    return;
                }
                for (ItemStack item : inventory) {
                    if (item == null || item.getItemMeta() == null) {
                        continue;
                    }
                    if (item.getItemMeta().getPersistentDataContainer().has(CropType.TYPE_KEY)) {
                        item.subtract();
                        for (ItemStack stack : inventory) {
                            if (stack == null) {
                                continue;
                            }
                            if (stack.getType().equals(Material.SUGAR) && stack.getAmount() < 64) {
                                stack.add();
                                break;
                            } else if (stack.equals(GUIMühle.ERGEBNIS)) {
                                int i = inventory.first(GUIMühle.ERGEBNIS);
                                stack.subtract();
                                inventory.setItem(i, new ItemStack(Material.SUGAR));
                                break;

                            }
                        }
                    }
                }
            }
        }.runTaskTimer(AethosCrops.getInstance(), 20, 20);
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        for (ItemStack item : event.getInventory()) {
            if (item == null) {
                continue;
            }
            if (item.equals(GUIMühle.ERGEBNIS) || item.equals(GUIMühle.PLACEHOLDER) || item.equals(GUIMühle.PFEIL) || item.equals(GUIMühle.ANZEIGE_GREEN) || item.equals(GUIMühle.ANZEIGE_RED)) {
                continue;
            }
            if (event.getPlayer().getInventory().firstEmpty() >= 0 || event.getPlayer().getInventory().first(item) >= 0) {
                event.getPlayer().getInventory().addItem(item);
            } else {
                event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), item);
            }

        }

    }
}


