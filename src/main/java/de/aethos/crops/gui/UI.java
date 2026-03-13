package de.aethos.crops.gui;

import de.aethos.crops.crop.Crop;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class UI extends InventoryView {
    protected final Player player;
    protected final Crop crop;
    protected Inventory inv;
    public UI(Player player, Crop crop){
        this.player = player;
        this.crop = crop;
    }

    @Override
    public @NotNull Inventory getTopInventory() {
        return inv;
    }

    @Override
    public @NotNull Inventory getBottomInventory() {
        return player.getInventory();
    }

    @Override
    public @NotNull HumanEntity getPlayer() {
        return player;
    }

    @Override
    public @NotNull InventoryType getType() {
        return InventoryType.CHEST;
    }

    @Override
    public abstract @NotNull String getTitle();

    public abstract void onClick(InventoryClickEvent event);

    protected ItemStack createGUIItems(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        final List<Component> comp = new ArrayList<>();
        for (String zeile : lore){
            comp.add(Component.text(zeile));
        }
        meta.displayName(Component.text(name));
        meta.lore(comp);
        item.setItemMeta(meta);
        return item;
    }

    public ArrayList<ItemStack> similarItemStackList(ItemStack a, ItemStack b, int anzahl, int mengeB) {
        ArrayList<ItemStack> ergebnis = new ArrayList<>();
        double r = Math.random();
        if(r <= 0.2){
            mengeB += 1;
        }
        while(anzahl > 0) {
            int presentB = 0;
            for (ItemStack n : ergebnis) {
                if (b == n) {
                    presentB += 1;
                }
            }
            int neededB = mengeB - presentB;
            if(anzahl == neededB){
                ergebnis.add(b);
            }
            if(neededB == 0){
                ergebnis.add(a);
            }
            else {
                double random = Math.random();
                if (random < 0.5) {
                    ergebnis.add(b);
                } else {
                    ergebnis.add(a);
                }
            }
            anzahl -= 1;
        }
        return ergebnis;
    }
}
