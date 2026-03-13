package de.aethos.crops.gui;

import de.aethos.crops.PlayerItems;
import de.aethos.crops.crop.Crop;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

import static de.aethos.crops.crop.CropManager.heal;

public class KäferUI extends UI implements Listener {
    private int clicks = 30;
    private static final ItemStack PFLANZE = GUIKÄFER.PFLANZE;
    private static final ItemStack KÄFER = GUIKÄFER.KÄFER;

    public KäferUI(Player player, Crop crop) {
        super(player, crop);
        GUIKÄFER guikäfer = new GUIKÄFER();
        this.inv = guikäfer.GUIKäfer;
        player.openInventory(this);
    }

    @Override
    public @NotNull String getTitle() {
        return "Käfer";
    }

    public void  onClick (InventoryClickEvent event){
        Inventory inv = event.getInventory();
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        if (item.equals(PFLANZE) || item.equals(GUIKÄFER.PLACEHOLDER) || item.equals(KÄFER) || item.equals(PlayerItems.getPestizit())) {
            event.setCancelled(true);
        }
        if (item.equals(KÄFER)) {
            int i = event.getSlot();
            inv.setItem(i,PFLANZE);
        }
        if (inv.first(KÄFER) == -1) {
            inv.close();
            heal(crop);
        }
        else {
            clicks -= 1;
            if(clicks <= 0 ){
                inv.close();
            }
            ItemStack[] content = Objects.requireNonNull(event.getClickedInventory()).getContents();
            int käferAnzahl = 0;
            for (ItemStack n : content) {
                if (n.equals(KÄFER)) {
                    käferAnzahl += 1;
                    System.out.println(käferAnzahl);
                }
            }
            while (!(inv.first(PFLANZE) == -1)) {
                inv.setItem(inv.first(PFLANZE), GUIKÄFER.P);
            }
            while (!(inv.first(KÄFER) == -1)) {
                inv.setItem(inv.first(KÄFER), GUIKÄFER.P);
            }
            ArrayList<ItemStack> newKäfer = similarItemStackList(PFLANZE, KÄFER, 28, käferAnzahl);
            System.out.println(käferAnzahl);
            System.out.println(newKäfer);
            for (ItemStack n : newKäfer) {
                int position = inv.first(GUIKÄFER.P);
                if (position <= 0) {
                    continue;
                }
                inv.setItem(position, n);
            }
        }
    }
}