package de.aethos.crops.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class UIListner implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void  inventoryClose (InventoryCloseEvent event){
        if(event.getView() instanceof UI){
            if(event.getView() instanceof MühlenUI){
                return;
            }
            event.getInventory().clear();
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void inventoryShift(InventoryClickEvent event) {
        if (event.getView() instanceof UI) {
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            if(event.getView() instanceof MühlenUI){
                event.setCancelled(false);

            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void inventoryDrop(InventoryDragEvent event) {
        if (!(event.getView() instanceof UI)) {
            return;
        }
        event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event){
        if(event.getView() instanceof UI ui){
            ui.onClick(event);
        }
    }
}
