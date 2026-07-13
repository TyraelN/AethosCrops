package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.SeedItemManager;
import de.aethos.crops.Utils.SeedGenes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Samen-Analyse-Menue (/aethoscrops analyze), umgesetzt mit GuiLib
 * (GUI-Standard der Aethos-Plugins, siehe AETHOS.md).
 * <p>
 * Obere 18 Slots sind die Eingabeflaeche (nur Aethos-Samen), die untere Reihe
 * besteht aus GuiLib-Buttons (Rahmen + Analysieren). Der Analysieren-Button
 * vereinzelt jeden Samen zu einem Einzel-Item mit exakter Gen-Lore
 * (SeedItemManager.createAnalyzedSeed).
 * <p>
 * GuiLib cancelt Klick und Drag vorab; erlaubte Interaktionen werden in
 * onClick/onDrag wieder freigegeben. Die Samen-Merge-Logik uebernimmt danach
 * der SeedStackListener - dessen HIGH-Handler laufen nach dem GuiLib-Listener,
 * weil GuiLib (Abhaengigkeit) vor AethosCrops aktiviert wird. Beim Schliessen
 * geht der Inhalt der Eingabeflaeche zurueck an den Spieler (Ueberschuss
 * droppt) - nichts geht verloren. Pro open() entsteht eine frische Instanz;
 * die Zuordnung Inventar->Menue verwaltet GuiLib (kein Session-Map noetig).
 */
public class SeedAnalysisMenu extends MenuHolder<AethosCrops> {

    private static final int GUI_SIZE = 27;
    private static final int INPUT_SLOTS = 18;
    private static final int BUTTON_SLOT = 22;

    public SeedAnalysisMenu(AethosCrops plugin) {
        // String-Titel-Konstruktor: GuiLib erstellt das Inventory mit sich
        // selbst als InventoryHolder - nur so ist die Holder-Erkennung
        // zuverlaessig. Der Inventory-Konstruktor (WeakHashMap-Registry fuer
        // Fremd-Inventare, wuerde Component-Titel erlauben) funktioniert mit
        // GuiLib 1.12.4 auf Paper 26.1 NICHT: kein Callback kommt an,
        // getestet 2026-07-13.
        super(plugin, GUI_SIZE, "Samen-Analyse");

        for (int slot = INPUT_SLOTS; slot < GUI_SIZE; slot++) {
            setButton(slot, slot == BUTTON_SLOT ? analyzeButton() : new ItemButton<>(frameItem()));
        }
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    /* ============================ Buttons ============================ */

    private ItemStack frameItem() {
        ItemStack frame = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = frame.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            frame.setItemMeta(meta);
        }
        return frame;
    }

    private ItemButton<SeedAnalysisMenu> analyzeButton() {
        ItemStack icon = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.translatable("gui.aethos.analysis.button")
                    .fallback("Analysieren")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.translatable("gui.aethos.analysis.button.lore")
                    .fallback("Vereinzelt alle Samen und zeigt ihre exakten Werte.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
        }

        return new ItemButton<>(icon) {
            @Override
            public void onClick(SeedAnalysisMenu holder, InventoryClickEvent event) {
                if (event.getWhoClicked() instanceof Player player) {
                    holder.analyze(player);
                }
            }
        };
    }

    /* ============================ Klick-Handling ============================ */

    @Override
    public void onClick(InventoryClickEvent event) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        Inventory clicked = event.getClickedInventory();

        // Klick neben das Fenster (Drop vom Cursor): erlaubt.
        if (clicked == null) {
            event.setCancelled(false);
            return;
        }

        boolean topClicked = clicked.equals(getInventory());

        if (topClicked && event.getSlot() >= INPUT_SLOTS) {
            // Rahmen/Analyse-Button: Button-Dispatch von GuiLib, bleibt gecancelt.
            super.onClick(event);
            return;
        }

        if (topClicked) {
            // Eingabeflaeche: nur Samen duerfen hinein (Cursor, Hotbar-Swap, Offhand).
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !seeds.isSeed(cursor)) {
                return; // bleibt gecancelt
            }

            if (event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked() instanceof Player player) {
                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
                if (hotbar != null && !hotbar.getType().isAir() && !seeds.isSeed(hotbar)) {
                    return;
                }
            }

            if (event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player player) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (!offhand.getType().isAir() && !seeds.isSeed(offhand)) {
                    return;
                }
            }

            event.setCancelled(false);
            return;
        }

        // Spielerinventar: Shift-Klick nur fuer Samen zulassen (sonst wuerden
        // beliebige Items in die Eingabeflaeche wandern), alles andere normal.
        if (event.getClick().isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir() && !seeds.isSeed(current)) {
                return;
            }
        }

        event.setCancelled(false);
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < GUI_SIZE);
        if (!touchesTop) {
            event.setCancelled(false);
            return;
        }

        boolean touchesFrame = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= INPUT_SLOTS && slot < GUI_SIZE);
        if (!touchesFrame && AethosCrops.getSeedItemManager().isSeed(event.getOldCursor())) {
            event.setCancelled(false);
        }
    }

    /* ============================ Analyse ============================ */

    private void analyze(Player player) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        Inventory inventory = getInventory();
        List<ItemStack> results = new ArrayList<>();

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!seeds.isSeed(stack) || seeds.isAnalyzed(stack)) {
                continue; // bereits analysierte Samen bleiben unveraendert liegen
            }

            for (SeedGenes gene : seeds.getGenes(stack)) {
                ItemStack analyzed = seeds.createAnalyzedSeed(stack, gene);
                if (analyzed != null) {
                    results.add(analyzed);
                }
            }
            inventory.setItem(slot, null);
        }

        if (results.isEmpty()) {
            return;
        }

        // Erst freie Eingabe-Slots fuellen, Ueberschuss ins Inventar, Rest droppen.
        int index = 0;
        for (int slot = 0; slot < INPUT_SLOTS && index < results.size(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                inventory.setItem(slot, results.get(index++));
            }
        }

        while (index < results.size()) {
            giveOrDrop(player, results.get(index++));
        }

        player.sendMessage(Component.translatable("gui.aethos.analysis.done")
                .fallback("%s Samen analysiert.")
                .arguments(Component.text(results.size(), NamedTextColor.WHITE))
                .color(NamedTextColor.GREEN));
    }

    /* ============================ Schliessen ============================ */

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = getInventory();
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, null);
            giveOrDrop(player, stack);
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
    }
}
