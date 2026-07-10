package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.SeedItemManager;
import de.aethos.crops.Utils.SeedGenes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Samen-Analyse-Menue (/aethoscrops analyze): Spieler legen Aethos-Samen in
 * die Eingabeflaeche; der Analysieren-Button vereinzelt jeden Samen zu einem
 * Einzel-Item mit exakter Gen-Lore (SeedItemManager.createAnalyzedSeed).
 * <p>
 * Klick-Regeln: Eingabeflaeche akzeptiert nur Samen; Rahmen und Button sind
 * nicht entnehmbar. Beim Schliessen geht der Inhalt der Eingabeflaeche
 * zurueck an den Spieler (Ueberschuss droppt) - nichts geht verloren.
 * Das Samen-Merging in der Eingabeflaeche uebernimmt der SeedStackListener
 * (laeuft auf HIGH nach diesem Listener).
 */
public class SeedAnalysisListener implements Listener {

    private static final int GUI_SIZE = 27;
    private static final int INPUT_SLOTS = 18;
    private static final int BUTTON_SLOT = 22;

    private final Map<UUID, Inventory> sessions = new HashMap<>();

    /* ============================ Oeffnen ============================ */

    public void open(Player player) {
        // Erst schliessen: openInventory() feuert sonst das Close-Event der
        // alten Ansicht NACH dem Ueberschreiben der Session-Map.
        player.closeInventory();

        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, Component
                .translatable("gui.aethos.analysis.title")
                .fallback("Samen-Analyse"));

        ItemStack filler = frameItem();
        for (int slot = INPUT_SLOTS; slot < GUI_SIZE; slot++) {
            inventory.setItem(slot, slot == BUTTON_SLOT ? buttonItem() : filler.clone());
        }

        sessions.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    private ItemStack frameItem() {
        ItemStack frame = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = frame.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            frame.setItemMeta(meta);
        }
        return frame;
    }

    private ItemStack buttonItem() {
        ItemStack button = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.translatable("gui.aethos.analysis.button")
                    .fallback("Analysieren")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.translatable("gui.aethos.analysis.button.lore")
                    .fallback("Vereinzelt alle Samen und zeigt ihre exakten Werte.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            button.setItemMeta(meta);
        }
        return button;
    }

    /* ============================ Klick-Handling ============================ */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory session = sessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session)) {
            return;
        }

        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        Inventory clicked = event.getClickedInventory();
        boolean topClicked = clicked != null && clicked.equals(session);

        if (topClicked && event.getSlot() >= INPUT_SLOTS) {
            // Rahmen/Button: nie entnehmbar; Button loest die Analyse aus.
            event.setCancelled(true);
            if (event.getSlot() == BUTTON_SLOT && event.getWhoClicked() instanceof Player player) {
                analyze(player, session);
            }
            return;
        }

        if (topClicked) {
            // Eingabeflaeche: nur Samen duerfen hinein (Cursor, Hotbar-Swap, Offhand).
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !seeds.isSeed(cursor)) {
                event.setCancelled(true);
                return;
            }

            if (event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked() instanceof Player player) {
                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
                if (hotbar != null && !hotbar.getType().isAir() && !seeds.isSeed(hotbar)) {
                    event.setCancelled(true);
                }
            }

            if (event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player player) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (!offhand.getType().isAir() && !seeds.isSeed(offhand)) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        // Spielerinventar: Shift-Klick nur fuer Samen zulassen (sonst wuerden
        // beliebige Items in die Eingabeflaeche wandern).
        if (event.getClick().isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir() && !seeds.isSeed(current)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory session = sessions.get(event.getWhoClicked().getUniqueId());
        if (session == null || !event.getView().getTopInventory().equals(session)) {
            return;
        }

        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < GUI_SIZE);
        if (!touchesTop) {
            return;
        }

        boolean touchesFrame = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= INPUT_SLOTS && slot < GUI_SIZE);
        if (touchesFrame || !AethosCrops.getSeedItemManager().isSeed(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /* ============================ Analyse ============================ */

    private void analyze(Player player, Inventory session) {
        SeedItemManager seeds = AethosCrops.getSeedItemManager();
        List<ItemStack> results = new ArrayList<>();

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = session.getItem(slot);
            if (!seeds.isSeed(stack) || seeds.isAnalyzed(stack)) {
                continue; // bereits analysierte Samen bleiben unveraendert liegen
            }

            for (SeedGenes gene : seeds.getGenes(stack)) {
                ItemStack analyzed = seeds.createAnalyzedSeed(stack, gene);
                if (analyzed != null) {
                    results.add(analyzed);
                }
            }
            session.setItem(slot, null);
        }

        if (results.isEmpty()) {
            return;
        }

        // Erst freie Eingabe-Slots fuellen, Ueberschuss ins Inventar, Rest droppen.
        int index = 0;
        for (int slot = 0; slot < INPUT_SLOTS && index < results.size(); slot++) {
            ItemStack existing = session.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                session.setItem(slot, results.get(index++));
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || !event.getInventory().equals(session)) {
            return;
        }

        sessions.remove(event.getPlayer().getUniqueId());

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = session.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            session.setItem(slot, null);
            giveOrDrop(player, stack);
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
    }
}
