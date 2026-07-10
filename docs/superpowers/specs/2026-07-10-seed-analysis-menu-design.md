# Design: Samen-Analyse-Menü (AethosCrops)

Datum: 2026-07-10
Status: Vom Nutzer freigegeben

## Ziel

Spieler mit entsprechender Permission können ein Analyse-Menü öffnen, dort
Aethos-Samen hineinlegen und analysieren lassen. Analysierte Samen werden
**vereinzelt** (ein Item pro Korn), zeigen in der Lore ihre **exakten
Genwerte** (Wachstum/Ertrag/Resistenz, 0–255) und **stacken nicht mehr** —
weder per Vanilla noch über die manuelle Merge-Logik des SeedStackListeners.

## Öffnen & Permission

- Einstieg: `/aethoscrops analyze` (Alias `/acrops analyze`).
- Neue Permission: `aethoscrops.analyze` (default: op; Vergabe an Spieler
  über das Permissions-Plugin).
- Umbau der Befehls-Registrierung: `CropsCommand.permission()` liefert aktuell
  `aethoscrops.admin` und sperrt damit den gesamten Befehl. Neu: die
  Command-Level-Permission entfällt (`permission()` → `null`); stattdessen
  prüft `AdminCommands.execute()` pro Subcommand:
  - `analyze` → `aethoscrops.analyze`
  - alle bisherigen Subcommands (`reload`, `chunkinfo`, `blockinfo`,
    `giveseed`, `givediseasetool`) → `aethoscrops.admin`
  - ohne passende Permission: rote Fehlermeldung, keine Usage-Ausgabe von
    Admin-Subcommands an Nicht-Admins.
  - `suggest()` schlägt nur Subcommands vor, die der Sender nutzen darf.

## Menü

27er-Chest-GUI, Titel „Samen-Analyse" (Translatable-Key
`gui.aethos.analysis.title`, deutscher Fallback, analog zu bestehender
Item-Lore).

Layout:

- **Slots 0–17 (obere zwei Reihen):** Eingabefläche. Spieler legt Samen-Stacks
  per Klick hinein und kann sie frei wieder herausnehmen. Es lassen sich nur
  Items einlegen, für die `SeedItemManager.isSeed()` true liefert; andere
  Items werden abgewiesen (Klick gecancelt).
- **Slots 18–26 (untere Reihe):** Rahmen aus grauem Glasscheiben-Filler
  (nicht entnehmbar). Slot 22 (Mitte): **Analysieren-Button** (Spyglass,
  Name „Analysieren", Translatable mit Fallback).

Verhalten:

- **Button-Klick:** Alle nicht analysierten Samen in der Eingabefläche werden
  vereinzelt und analysiert. Aus einem Stack von N Samen entstehen N
  Einzel-Items; jedes erhält genau seinen Gen-Eintrag aus der bestehenden
  Gen-Liste des Stacks (Reihenfolge der Liste; die Daten pro Korn existieren
  bereits im PDC).
- **Platzlogik:** Ergebnis-Items füllen zuerst freie Eingabe-Slots, Überschuss
  geht ins Spielerinventar, verbleibender Rest wird am Spieler gedroppt.
- **Bereits analysierte Samen** in der Eingabefläche werden beim Button-Klick
  unverändert übersprungen.
- **Menü schließen:** Sämtliche Items der Eingabefläche gehen zurück ins
  Spielerinventar, Überschuss droppt am Spieler. Nichts geht verloren.
- Session-Verwaltung analog `DiseaseTreatmentListener`
  (Map<UUID, Session>, Abgleich über `event.getView().getTopInventory()`);
  Listener-Priorität so, dass der `SeedStackListener` (HIGH,
  ignoreCancelled=true) gecancelte Klicks überspringt.
- Shift-Klicks, Drag-Events und Zahlen-Tasten (Hotbar-Swap) in das GUI werden
  kontrolliert behandelt: erlaubt für Samen in die Eingabefläche, sonst
  gecancelt.

## Analysierter Samen (Item-Format)

- `amount = 1`, PDC enthält weiterhin `crop_type` und `seed_genes` mit genau
  einem Eintrag → Pflanzen, Vererbung und alle bestehenden Pfade funktionieren
  unverändert.
- Neue PDC-Keys (in `CropKey`):
  - `analyzed` (BYTE, 1) — Kennzeichnung.
  - `analysis_id` (STRING, zufällige UUID) — macht jede Item-Meta einzigartig,
    damit Vanilla-Stacking ausgeschlossen ist.
- Lore (Translatable + deutscher Fallback, nicht kursiv, wie bestehende Lore):
  1. Güte-Zeile mit Sternen (wie bisher)
  2. `Wachstum: <g>/255`
  3. `Ertrag: <y>/255`
  4. `Resistenz: <r>/255`
  5. `✦ Analysiert`-Zeile
  6. bestehende „Aethos-Pflanzensamen"-Zeile
- Name bleibt unverändert.

## Stacking-Ausschluss

- `SeedItemManager.isAnalyzed(ItemStack)` — neue Hilfsmethode.
- `canMerge(a, b)` liefert `false`, sobald einer der Stacks analysiert ist
  → deckt Klick-Merge, Shift-Klick, Kisten-Konsolidierung und Pickup im
  `SeedStackListener` ab.
- `consolidate(Inventory)` überspringt analysierte Stacks.
- Vanilla-Stacking ist durch die einzigartige `analysis_id` ausgeschlossen.
- `getGenes()`-Reparaturlogik bleibt konsistent: amount=1, ein Eintrag.

## Fehlerbehandlung / Edge Cases

- Analyse-Button-Klick mit leerer Eingabefläche: nichts passiert.
- Spieler-Disconnect mit offenem Menü: `InventoryCloseEvent` feuert beim
  Disconnect → Rückgabe-Logik greift.
- Samen ohne Gen-Daten (Alt-Samen): `getGenes()` repariert wie bisher auf
  Default-Gene (0/0/0), Analyse zeigt dann exakte Nullwerte.
- Nicht-Samen-Items können nicht eingelegt werden; Filler/Button sind nicht
  entnehmbar.

## Komponenten

- **Neu** `Events/SeedAnalysisListener.java` (nach dem Muster des
  `DiseaseTreatmentListener`): GUI-Aufbau, Klick-/Close-Handling,
  Analyse-Logik, Session-Map.
- **Erweitert** `SeedItemManager`: `isAnalyzed()`, `createAnalyzedSeed(Crop, SeedGenes)`,
  Ausschluss in `canMerge()`/`consolidate()`.
- **Erweitert** `CropKey`: `ANALYZED`, `ANALYSIS_ID`.
- **Erweitert** `AdminCommands` + `CropsCommand`: Subcommand `analyze`,
  per-Subcommand-Permissions, Tab-Complete.
- **Registrierung** des neuen Listeners in `AethosCrops.registerListeners()`.

## Tests

- Unit-testbar (ohne Server): Gen-Serialisierung/Vereinzelungslogik, sofern
  vom Bukkit-API entkoppelt.
- In-Game-Verifikation auf DEV über den Testbot bzw. manuell:
  1. Permission vergeben, `/acrops analyze` öffnet Menü; ohne Permission: Fehlermeldung.
  2. Stack (z.B. 5 Samen) einlegen, analysieren → 5 Einzel-Items mit exakten Werten.
  3. Zwei analysierte Samen gleicher Güte lassen sich nicht stacken (Klick,
     Shift-Klick, Kisten-Öffnen, Pickup, Konsolidierung).
  4. Analysierter Samen lässt sich pflanzen; Crop übernimmt exakt die Gene.
  5. Menü schließen mit vollen Slots → Rückgabe/Drop ohne Verlust.
