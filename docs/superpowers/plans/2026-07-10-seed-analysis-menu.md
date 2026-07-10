# Samen-Analyse-Menü Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spieler mit Permission `aethoscrops.analyze` öffnen per `/aethoscrops analyze` ein GUI, legen Aethos-Samen hinein und erhalten pro Korn ein einzelnes, nicht mehr stackbares Item mit exakten Genwerten in der Lore.

**Architecture:** Neuer `SeedAnalysisListener` (GUI, nach dem Muster des `DiseaseTreatmentListener`), Erweiterungen in `SeedItemManager` (analysierte Einzel-Items, Stacking-Ausschluss), `CropKey` (neue PDC-Keys) und `AdminCommands`/`CropsCommand` (Subcommand + per-Subcommand-Permissions).

**Tech Stack:** Java 21, Paper-API 26.1 (Adventure-Components, PDC, Brigadier-BasicCommand), Maven.

**Spec:** `docs/superpowers/specs/2026-07-10-seed-analysis-menu-design.md`

## Global Constraints

- Kein Test-Framework im Projekt (kein `src/test`, keine JUnit-Dependency, Bukkit-Klassen bräuchten Server-Mocks). Verifikation pro Task: `mvn -q -f /root/mc-admin/dev/plugins/AethosCrops/pom.xml clean package` muss fehlerfrei durchlaufen. End-to-End-Verifikation in Task 4 in-game auf DEV.
- Lore/Namen immer als `Component.translatable(...)` mit deutschem `fallback(...)` und `.decoration(TextDecoration.ITALIC, false)` — wie bestehende Lore in `SeedItemManager`.
- Kommentare im Code auf Deutsch, ASCII-Umschreibung (ue/oe/ae) wie im Bestand.
- Permission-Namen exakt: `aethoscrops.analyze` (neu), `aethoscrops.admin` (bestehend).
- PROD wird nicht angefasst; Deploy nur auf DEV.

---

### Task 1: SeedItemManager — analysierte Samen & Stacking-Ausschluss

**Files:**
- Modify: `src/main/java/de/aethos/crops/Utils/CropKey.java`
- Modify: `src/main/java/de/aethos/crops/Managers/SeedItemManager.java`

**Interfaces:**
- Consumes: bestehende `SeedGenes`, `CropKey`, `starsComponent(int)`.
- Produces (von Task 2 genutzt):
  - `boolean isAnalyzed(ItemStack stack)`
  - `ItemStack createAnalyzedSeed(ItemStack template, SeedGenes gene)` — liefert Einzel-Item (amount 1) mit exakter Gen-Lore, `analyzed`-Flag und einzigartiger `analysis_id`.
  - `canMerge(a, b)` liefert `false`, sobald einer der Stacks analysiert ist; `consolidate(...)` überspringt analysierte Stacks.

- [ ] **Step 1: Neue PDC-Keys in `CropKey` ergänzen**

In `CropKey.java` nach `MODEL_PATH` einfügen:

```java
    public static final NamespacedKey ANALYZED = key("analyzed");
    public static final NamespacedKey ANALYSIS_ID = key("analysis_id");
```

- [ ] **Step 2: `isAnalyzed` und `createAnalyzedSeed` in `SeedItemManager` ergänzen**

Import ergänzen: `java.util.UUID`.

Im Abschnitt `/* Lesen/Schreiben */` nach `getCropId(...)` einfügen:

```java
    // Analysierte Samen sind vereinzelte Einzel-Items mit exakter Gen-Lore.
    // Sie stacken nie: Vanilla nicht (einzigartige analysis_id in der Meta),
    // und die manuelle Merge-Logik schliesst sie ueber canMerge() aus.
    public boolean isAnalyzed(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(CropKey.ANALYZED);
    }

    // Baut aus einem Samen-Stack (Vorlage fuer Name/Material/crop_type) ein
    // analysiertes Einzel-Item fuer genau einen Gen-Eintrag.
    public ItemStack createAnalyzedSeed(ItemStack template, SeedGenes gene) {
        if (template == null || gene == null) {
            return null;
        }

        ItemStack single = template.clone();
        single.setAmount(1);

        ItemMeta meta = single.getItemMeta();
        if (meta == null) {
            return single;
        }

        meta.lore(List.of(
                Component.translatable("item.aethos.seed.quality")
                        .fallback("Güte: %s")
                        .arguments(starsComponent(gene.getStars()))
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                statLine("item.aethos.seed.growth", "Wachstum: %s", gene.getGrowth()),
                statLine("item.aethos.seed.yield", "Ertrag: %s", gene.getYield()),
                statLine("item.aethos.seed.resistance", "Resistenz: %s", gene.getResistance()),
                Component.translatable("item.aethos.seed.analyzed")
                        .fallback("✦ Analysiert")
                        .color(COLOR_GOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.translatable("item.aethos.seed.lore")
                        .fallback("Aethos-Pflanzensamen")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(CropKey.SEED_GENES, PersistentDataType.STRING, gene.serialize());
        meta.getPersistentDataContainer().set(CropKey.ANALYZED, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(CropKey.ANALYSIS_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
        single.setItemMeta(meta);
        return single;
    }

    // Exakte Wert-Zeile der Analyse-Lore, z.B. "Wachstum: 120/255".
    private Component statLine(String key, String fallback, int value) {
        return Component.translatable(key)
                .fallback(fallback)
                .arguments(Component.text(value + "/" + SeedGenes.MAX_VALUE, NamedTextColor.WHITE))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }
```

- [ ] **Step 3: Stacking-Ausschluss in `canMerge` und `consolidate`**

In `canMerge(ItemStack a, ItemStack b)` direkt nach dem `isSeed`-Check einfügen:

```java
        // Analysierte Samen sind vereinzelt und stacken grundsaetzlich nicht.
        if (isAnalyzed(a) || isAnalyzed(b)) {
            return false;
        }
```

In `consolidate(Inventory inventory)` die Schleifen-Prüfung erweitern — aus

```java
            if (!isSeed(stack)) {
                continue;
            }
```

wird

```java
            if (!isSeed(stack) || isAnalyzed(stack)) {
                continue;
            }
```

- [ ] **Step 4: Kompilieren**

Run: `mvn -q -f /root/mc-admin/dev/plugins/AethosCrops/pom.xml clean package`
Expected: BUILD SUCCESS, keine Compiler-Fehler.

- [ ] **Step 5: Commit**

```bash
cd /root/mc-admin/dev/plugins/AethosCrops
git add src/main/java/de/aethos/crops/Utils/CropKey.java src/main/java/de/aethos/crops/Managers/SeedItemManager.java
git commit -m "feat: analysierte Samen (Einzel-Items mit exakter Gen-Lore, vom Stacking ausgeschlossen)"
```

---

### Task 2: SeedAnalysisListener (GUI) + Registrierung

**Files:**
- Create: `src/main/java/de/aethos/crops/Events/SeedAnalysisListener.java`
- Modify: `src/main/java/de/aethos/crops/AethosCrops.java` (Feld, Getter, Registrierung)

**Interfaces:**
- Consumes: `SeedItemManager.isSeed/isAnalyzed/getGenes/createAnalyzedSeed` (Task 1).
- Produces: `void open(Player player)` — öffnet das Analyse-Menü; von `AdminCommands` (Task 3) aufgerufen. Statischer Zugriff: `AethosCrops.getSeedAnalysisListener()`.

- [ ] **Step 1: Listener-Klasse anlegen**

Vollständiger Inhalt von `SeedAnalysisListener.java`:

```java
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
```

- [ ] **Step 2: In `AethosCrops` registrieren und Getter anlegen**

Import ergänzen: `de.aethos.crops.Events.SeedAnalysisListener`.

Statisches Feld bei den anderen Managern:

```java
    public static SeedAnalysisListener seedAnalysisListener;
```

In `onEnable()` nach `configManager.loadCrops(cropRegistry);`:

```java
        seedAnalysisListener = new SeedAnalysisListener();
```

In `registerListeners()`:

```java
        pluginManager.registerEvents(seedAnalysisListener, this);
```

Getter bei den anderen Gettern:

```java
    public static SeedAnalysisListener getSeedAnalysisListener() {
        return seedAnalysisListener;
    }
```

- [ ] **Step 3: Kompilieren**

Run: `mvn -q -f /root/mc-admin/dev/plugins/AethosCrops/pom.xml clean package`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /root/mc-admin/dev/plugins/AethosCrops
git add src/main/java/de/aethos/crops/Events/SeedAnalysisListener.java src/main/java/de/aethos/crops/AethosCrops.java
git commit -m "feat: Samen-Analyse-Menue (GUI mit Eingabeflaeche und Analysieren-Button)"
```

---

### Task 3: Subcommand `analyze` + per-Subcommand-Permissions

**Files:**
- Modify: `src/main/java/de/aethos/crops/AethosCrops.java`
- Modify: `src/main/java/de/aethos/crops/Testing/AdminCommands.java`
- Modify: `src/main/resources/paper-plugin.yml`

**Interfaces:**
- Consumes: `AethosCrops.getSeedAnalysisListener().open(Player)` (Task 2).
- Produces: `/aethoscrops analyze` (Permission `aethoscrops.analyze`), bisherige Subcommands hinter `aethoscrops.admin`; `suggest()` filtert nach Permission.

- [ ] **Step 1: `CropsCommand.permission()` öffnen**

In `AethosCrops.java`, innere Klasse `CropsCommand`: die Methode

```java
        @Override
        public String permission() {
            return "aethoscrops.admin";
        }
```

ersetzen durch

```java
        @Override
        public String permission() {
            // Permissions werden pro Subcommand in AdminCommands geprueft
            // (analyze -> aethoscrops.analyze, Rest -> aethoscrops.admin).
            return null;
        }
```

- [ ] **Step 2: Permission-Prüfung pro Subcommand in `AdminCommands.execute`**

Die bisherige `execute(...)`-Methode komplett ersetzen durch:

```java
    public void execute(CommandSender sender, String[] args) {
        // reload darf auch von der Konsole (Aethos-Panel) kommen.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aethoscrops.admin")) {
                sender.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
                return;
            }
            handleReload(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("analyze")) {
            if (!player.hasPermission("aethoscrops.analyze")) {
                player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
                return;
            }
            AethosCrops.getSeedAnalysisListener().open(player);
            return;
        }

        if (!player.hasPermission("aethoscrops.admin")) {
            player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
            return;
        }

        switch (subCommand) {
            case "chunkinfo" -> handleChunkInfo(player);
            case "blockinfo" -> handleBlockInfo(player);
            case "giveseed" -> handleGiveSeed(player, args);
            case "givediseasetool" -> handleGiveDiseaseTool(player, args);
            default -> sendUsage(player);
        }
    }
```

- [ ] **Step 3: `sendUsage` und `suggest` nach Permission filtern**

`sendUsage(Player player)` ersetzen durch:

```java
    private void sendUsage(Player player) {
        List<String> allowed = allowedSubCommands(player);
        if (allowed.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
            return;
        }
        player.sendMessage(ChatColor.RED + "Usage: /aethoscrops <" + String.join("|", allowed) + ">");
    }

    // Subcommands, die der Sender laut Permission nutzen darf.
    private List<String> allowedSubCommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        if (sender.hasPermission("aethoscrops.analyze")) {
            allowed.add("analyze");
        }
        if (sender.hasPermission("aethoscrops.admin")) {
            allowed.addAll(List.of("chunkinfo", "blockinfo", "giveseed", "givediseasetool", "reload"));
        }
        return allowed;
    }
```

In `suggest(...)` den `args.length == 1`-Block ersetzen durch:

```java
        if (args.length == 1) {
            return allowedSubCommands(sender).stream()
                    .filter(entry -> entry.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
```

Und die drei folgenden `giveseed`/`givediseasetool`-Blöcke jeweils zusätzlich mit `sender.hasPermission("aethoscrops.admin") &&` absichern, z.B.:

```java
        if (args.length == 2 && args[0].equalsIgnoreCase("giveseed") && sender.hasPermission("aethoscrops.admin")) {
```

- [ ] **Step 4: Permission in `paper-plugin.yml` deklarieren**

Unter `permissions:` ergänzen:

```yaml
  aethoscrops.analyze:
    description: Erlaubt das Samen-Analyse-Menü (/aethoscrops analyze).
    default: op
```

- [ ] **Step 5: Kompilieren**

Run: `mvn -q -f /root/mc-admin/dev/plugins/AethosCrops/pom.xml clean package`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
cd /root/mc-admin/dev/plugins/AethosCrops
git add src/main/java/de/aethos/crops/AethosCrops.java src/main/java/de/aethos/crops/Testing/AdminCommands.java src/main/resources/paper-plugin.yml
git commit -m "feat: /aethoscrops analyze mit eigener Permission, Admin-Subcommands abgesichert"
```

---

### Task 4: Build, Deploy auf DEV, In-Game-Verifikation

**Files:** keine Quelländerungen (nur Build/Deploy/Test).

- [ ] **Step 1: Bauen und auf DEV deployen**

```bash
bash /root/mc-admin/scripts/build-plugin.sh AethosCrops
bash /root/mc-admin/scripts/deploy-plugins.sh --only AethosCrops-1.0.0-SNAPSHOT.jar
```

Expected: JAR landet in `/root/mc-admin/servers/development/plugins/`.

- [ ] **Step 2: DEV-Server neu starten**

WICHTIG (Memory „Restart-Ansage"): vorher Spieler auf DEV prüfen und per `say` Grund + Dauer ankündigen, auch auf DEV. Danach Neustart über das übliche Verfahren (`/root/mc-admin/scripts/manage.sh` bzw. `mc.sh`).

- [ ] **Step 3: In-Game-Verifikation (Testbot aus experimental/ oder manuell)**

Checkliste (aus dem Spec):
1. Ohne Permission: `/acrops analyze` → rote Fehlermeldung; Tab-Complete zeigt `analyze` nicht.
2. Permission `aethoscrops.analyze` vergeben → Menü öffnet; Rahmen/Button nicht entnehmbar.
3. `/acrops giveseed <id> 5` (als Admin), Stack einlegen, Button klicken → 5 Einzel-Items, Lore zeigt Güte + `Wachstum/Ertrag/Resistenz: x/255` + `✦ Analysiert`.
4. Zwei analysierte Samen stacken nicht: Klick aufeinander, Shift-Klick in Kiste, Drop + Aufsammeln, Kiste öffnen (consolidate) — alles bleibt einzeln.
5. Analysierter Samen lässt sich pflanzen; `/acrops blockinfo` zeigt exakt die Gene aus der Lore.
6. Menü mit Samen schließen → alles zurück im Inventar; volles Inventar → Drop am Spieler.
7. Nicht-Samen-Items (z.B. Weizen) lassen sich weder per Klick noch Shift-Klick noch Drag einlegen.
8. Erneut analysieren: bereits analysierte Samen im Menü bleiben beim Button-Klick unverändert.

- [ ] **Step 4: Abschluss-Commit (falls Fixes nötig waren) und Plan-Checkboxen aktualisieren**

```bash
cd /root/mc-admin/dev/plugins/AethosCrops
git status
```
