package de.aethos.crops.gui;

import de.aethos.crops.PlayerItems;
import de.aethos.crops.crop.Crop;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static de.aethos.crops.crop.CropManager.heal;

public class UnkrautUI extends UI {
    public UnkrautUI(Player player, Crop crop) {
        super(player, crop);
        GUIUNKRAUT guiunkraut = new GUIUNKRAUT();
        this.inv = guiunkraut.GUIUnkrauft;
        player.openInventory(this);
    }

    @Override
    public @NotNull String getTitle() {
        return "Unkraut";
    }

    public void onClick(InventoryClickEvent event){
        if(!(event.getView() instanceof UnkrautUI)){
            System.out.println("1");
            return;
        }
        if(!Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())){
            System.out.println("2");
            event.setCancelled(false);
            return;
        }
        System.out.println("3");
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            System.out.println("4");
            return;
        }
        if (event.getCursor() == null) {
            System.out.println("5");
            return;
        }
        if (PlayerItems.itemIsUnkrautHacke(event.getCursor())) {
            if(item.equals((GUIUNKRAUT.NORMALE_PFLANZE))){
                System.out.println("6");
                event.getInventory().close();
                return;
            }
            if (item.equals(GUIUNKRAUT.UNKRAUT)) {
                System.out.println("8");
                ItemStack courser = event.getCursor();
                ItemMeta meta = courser.getItemMeta();
                if(meta instanceof Damageable damageable){
                    damageable.setDamage(damageable.getDamage() + 1);
                    event.getCursor().setItemMeta(meta);
                    if(damageable.getDamage() > 59){
                        event.getCursor().subtract();
                    }
                }
                int i1 = event.getSlot();
                event.getInventory().setItem(i1, GUIUNKRAUT.NORMALE_PFLANZE);
            }
        }
        if (event.getInventory().first(GUIUNKRAUT.UNKRAUT) == -1) {
            System.out.println("9");
            event.getInventory().close();
            heal(crop);
        }
    }

}