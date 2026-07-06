package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.SeedItemManager;
import de.aethos.crops.Utils.SeedGenes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Haelt die Gen-Buchfuehrung der Samen-Stacks bei allen Inventar-Aktionen
 * konsistent. Vanilla stackt nur Items mit identischer Meta - Stacks gleicher
 * Guete, aber unterschiedlicher Gen-Listen werden deshalb hier manuell
 * zusammengefuehrt (Klick, Shift-Klick, Kisten-Oeffnen, Pickup); Splits
 * (Rechtsklick, Q-Drop) werden manuell ausgefuehrt, damit jeder Teil-Stack
 * exakt seine eigenen Samen behaelt.
 * <p>
 * Hopper, Dispenser und Dropper sind fuer Samen gesperrt: nicht hineinlegbar,
 * nicht per Hopper aus Kisten ziehbar, nicht auswerfbar.
 */
public class SeedStackListener implements Listener {

    // HIGH + ignoreCancelled: laeuft nach GUI-Listenern (z.B. Disease-Treatment),
    // die Klicks bereits canceln.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        boolean cursorSeed = seeds.isSeed(cursor);
        boolean currentSeed = seeds.isSeed(current);

        Inventory top = event.getView().getTopInventory();
        if (isSeedBlockedContainer(top.getType()) && blocksSeedInsert(event, seeds, cursorSeed, currentSeed, top)) {
            event.setCancelled(true);
            return;
        }

        if (!cursorSeed && !currentSeed) {
            return;
        }

        // Klick neben das Fenster: Rechtsklick droppt einen einzelnen Samen vom Cursor.
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            if (cursorSeed && event.getClick() == ClickType.RIGHT && cursor.getAmount() > 1) {
                event.setCancelled(true);
                ItemStack single = seeds.takeFromStack(cursor, 1);
                event.getView().setCursor(cursor.getAmount() > 0 ? cursor : null);
                dropAsPlayer(event.getWhoClicked() instanceof Player player ? player : null, single);
            }
            // Linksklick droppt den ganzen Stack - Liste bleibt konsistent.
            return;
        }

        switch (event.getClick()) {
            case LEFT -> {
                // Stacks gleicher Guete manuell zusammenfuehren (Vanilla wuerde
                // wegen unterschiedlicher PDC-Listen tauschen statt stacken).
                if (cursorSeed && currentSeed && seeds.canMerge(cursor, current)) {
                    if (current.getAmount() >= current.getMaxStackSize()) {
                        return; // Ziel voll -> Vanilla-Swap ganzer Staecke ist konsistent.
                    }

                    event.setCancelled(true);
                    seeds.merge(cursor, current);
                    event.setCurrentItem(current);
                    event.getView().setCursor(cursor.getAmount() > 0 ? cursor : null);
                }
            }
            case RIGHT -> {
                if (cursorSeed && currentSeed && seeds.canMerge(cursor, current)) {
                    // Einen einzelnen Samen vom Cursor in den Slot legen.
                    if (current.getAmount() >= current.getMaxStackSize()) {
                        return;
                    }

                    event.setCancelled(true);
                    ItemStack single = seeds.takeFromStack(cursor, 1);
                    seeds.merge(single, current);
                    event.setCurrentItem(current);
                    event.getView().setCursor(cursor.getAmount() > 0 ? cursor : null);
                } else if (cursorSeed && (current == null || current.getType().isAir())) {
                    // Einen einzelnen Samen in einen leeren Slot legen.
                    event.setCancelled(true);
                    ItemStack single = seeds.takeFromStack(cursor, 1);
                    event.setCurrentItem(single);
                    event.getView().setCursor(cursor.getAmount() > 0 ? cursor : null);
                } else if ((cursor == null || cursor.getType().isAir()) && currentSeed && current.getAmount() > 1) {
                    // Halben Stack aufnehmen: Cursor bekommt die obere Haelfte
                    // (vom Ende der Liste), der Slot behaelt den Rest.
                    event.setCancelled(true);
                    int half = (current.getAmount() + 1) / 2;
                    ItemStack taken = seeds.takeFromStack(current, half);
                    event.setCurrentItem(current.getAmount() > 0 ? current : null);
                    event.getView().setCursor(taken);
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                // Shift-Klick manuell ausfuehren, damit Samen auch in Stacks mit
                // anderer Gen-Liste (aber gleicher Guete) einsortiert werden -
                // sonst landet in Kisten jeder Stack in einem eigenen Slot.
                if (!currentSeed) {
                    return;
                }

                // Eigene Inventar-Ansicht (kein Container offen): Vanilla
                // verschiebt nur ganze Staecke - konsistent.
                if (top.getType() == InventoryType.CRAFTING || top.getType() == InventoryType.PLAYER) {
                    return;
                }

                boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
                Inventory target = clickedTop ? event.getView().getBottomInventory() : top;

                event.setCancelled(true);
                moveWithMerge(seeds, current, target);
                event.setCurrentItem(current.getAmount() > 0 ? current : null);
            }
            case DROP -> {
                // Q auf einen Slot: droppt genau einen Samen.
                if (currentSeed && current.getAmount() > 1) {
                    event.setCancelled(true);
                    ItemStack single = seeds.takeFromStack(current, 1);
                    event.setCurrentItem(current.getAmount() > 0 ? current : null);
                    dropAsPlayer(event.getWhoClicked() instanceof Player player ? player : null, single);
                }
                // Amount 1 oder CONTROL_DROP: ganzer (Teil-)Stack, Liste konsistent.
            }
            default -> {
                // Nummerntasten, F, DOUBLE_CLICK: verschieben ganze Staecke oder
                // stacken nur bei identischer Meta - dort repariert die zyklische
                // Auffuellung in getGenes() korrekt.
            }
        }
    }

    // Drag verteilt den Cursor-Stack auf mehrere Slots und wuerde die Gen-Liste
    // unkontrolliert klonen - nur die Faelle zulassen, die einem normalen
    // Ablegen entsprechen. In gesperrte Container ist Drag generell verboten.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        ItemStack cursor = event.getOldCursor();
        if (!seeds.isSeed(cursor)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (isSeedBlockedContainer(top.getType())) {
            boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
            if (touchesTop) {
                event.setCancelled(true);
                return;
            }
        }

        boolean singleSlot = event.getRawSlots().size() <= 1;
        boolean wholeStack = event.getType() == DragType.EVEN || cursor.getAmount() == 1;
        if (singleSlot && wholeStack) {
            return;
        }

        event.setCancelled(true);
    }

    // Q ausserhalb eines Inventars: Vanilla spaltet einen Samen ab, beide Teile
    // tragen danach die volle Gen-Liste - hier korrekt aufteilen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        Item drop = event.getItemDrop();
        ItemStack dropped = drop.getItemStack();
        if (!seeds.isSeed(dropped)) {
            return;
        }

        List<SeedGenes> droppedGenes = seeds.readRawGenes(dropped);
        if (droppedGenes.size() <= dropped.getAmount()) {
            return;
        }

        // Der gedroppte Samen ist der letzte Eintrag; der Rest gehoert zum
        // verbleibenden Stack in der Hand.
        List<SeedGenes> taken = new ArrayList<>(
                droppedGenes.subList(droppedGenes.size() - dropped.getAmount(), droppedGenes.size()));
        seeds.setGenes(dropped, taken);
        drop.setItemStack(dropped);

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (seeds.isSeed(hand)) {
            seeds.trimToAmount(hand);
            event.getPlayer().getInventory().setItemInMainHand(hand);
        }
    }

    // Nach dem Aufsammeln Stacks gleicher Guete im Spielerinventar vereinen
    // (Vanilla legt sie wegen unterschiedlicher PDC-Listen in separate Slots).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!seeds.isSeed(event.getItem().getItemStack())) {
            return;
        }

        Bukkit.getScheduler().runTask(AethosCrops.getInstance(),
                () -> seeds.consolidate(player.getInventory()));
    }

    // Beim Oeffnen von Lager-Containern vorhandene Samen-Stacks gleicher Guete
    // zusammenfuehren (z.B. frueher einzeln eingelagerte Ernte-Samen).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory top = event.getInventory();
        switch (top.getType()) {
            case CHEST, BARREL, ENDER_CHEST, SHULKER_BOX -> AethosCrops.getSeedItemManager().consolidate(top);
            default -> { }
        }
    }

    /* ================= Hopper/Dispenser/Dropper-Sperre ================= */

    // Hopper-Transfers (ziehen aus Kisten, weiterreichen) fuer Samen unterbinden.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (AethosCrops.getSeedItemManager().isSeed(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // Hopper duerfen keine am Boden liegenden Samen aufsaugen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (AethosCrops.getSeedItemManager().isSeed(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Dispenser/Dropper werfen keine Samen aus (Sicherheitsnetz, falls doch
    // welche hineingelangt sind).
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (AethosCrops.getSeedItemManager().isSeed(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /* ============================ Hilfsfunktionen ============================ */

    private boolean isSeedBlockedContainer(InventoryType type) {
        return type == InventoryType.HOPPER || type == InventoryType.DISPENSER || type == InventoryType.DROPPER;
    }

    // Prueft, ob der Klick Samen in einen gesperrten Container legen wuerde.
    private boolean blocksSeedInsert(InventoryClickEvent event, SeedItemManager seeds,
                                     boolean cursorSeed, boolean currentSeed, Inventory top) {
        boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);

        if (clickedTop) {
            if (cursorSeed) {
                return true;
            }

            if (event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked() instanceof Player player) {
                return seeds.isSeed(player.getInventory().getItem(event.getHotbarButton()));
            }

            if (event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player player) {
                return seeds.isSeed(player.getInventory().getItemInOffHand());
            }

            return false;
        }

        // Shift-Klick aus dem Spielerinventar in den gesperrten Container.
        return currentSeed && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT);
    }

    // Verschiebt einen Samen-Stack in ein Ziel-Inventar: erst in kompatible
    // Stacks gleicher Guete mergen, Rest in den ersten freien Slot.
    private void moveWithMerge(SeedItemManager seeds, ItemStack source, Inventory target) {
        int size = target.getStorageContents().length;

        for (int slot = 0; slot < size && source.getAmount() > 0; slot++) {
            ItemStack existing = target.getItem(slot);
            if (existing == null || !seeds.canMerge(source, existing)) {
                continue;
            }

            if (seeds.merge(source, existing) > 0) {
                target.setItem(slot, existing);
            }
        }

        if (source.getAmount() <= 0) {
            return;
        }

        for (int slot = 0; slot < size; slot++) {
            ItemStack existing = target.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                target.setItem(slot, source.clone());
                source.setAmount(0);
                return;
            }
        }
        // Kein Platz: Stack bleibt (ggf. teilweise gemerged) an der Quelle.
    }

    private void dropAsPlayer(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.getAmount() <= 0) {
            return;
        }

        Item item = player.getWorld().dropItem(player.getEyeLocation(), stack);
        item.setVelocity(player.getEyeLocation().getDirection().multiply(0.3));
        item.setPickupDelay(40);
        item.setThrower(player.getUniqueId());
    }
}
