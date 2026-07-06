# AethosCrops ✦

Custom-Crops für Paper mit **Wachstumsstufen**, **Krankheiten** und konfigurierbaren
**Drops** – dargestellt über `ItemDisplay`-Modelle (Modellpfade kommen aus dem
Ressourcepack, siehe AethosResourcePack). Vollständig über die `config.yml` definierbar.

## Funktionen
- Beliebig viele Crops in der `config.yml` (`crops.<id>`): Anzeigename, Stufen-Modellpfade,
  Wachstumsgeschwindigkeit, Max-Stufe, erlaubte Krankheiten (mit Wahrscheinlichkeit), Drop-Tabelle.
- Wachstum stufenweise über Vanilla-Weizen; Crop-Daten liegen im Chunk-`PersistentDataContainer`.
- **Krankheiten** (Weed, Bugs, Fungal Infection) mit Leveln; Behandlung über ein Such-GUI
  (richtige Symptome anklicken, Blätter senken die Pflanzengesundheit).
- Drops skalieren mit der Pflanzengesundheit; Trampeln/Wasser zerstören Crops korrekt.
- **Genetik**: Jeder Samen trägt drei Gen-Werte (Wachstum, Ertrag, Resistenz; je 0–255,
  Default 0 = Vanilla-Verhalten). Sichtbar ist nur die Sterne-Güte (1–5, Durchschnitt der
  Werte). Samen gleicher Güte stacken; das Plugin führt pro Stack Buch über die Gene der
  Einzelsamen (im Item-PDC, Verbrauch/Splits/Drops bleiben konsistent). Geerntete Samen
  erben die Gene der Elternpflanze (Mutation ± `genetics.mutation-spread`). Effekte:
  Wachstumsgeschwindigkeit ×1.0–2.0, Drop-Menge ×1.0–2.0, Krankheits-Chance ×1.0–0.5.
- Samen-Stacks werden auch in Kisten/Fässern zusammengeführt (Shift-Klick & beim Öffnen).
  Hopper, Dispenser und Dropper sind für Samen gesperrt (nicht hineinlegbar, nicht aus
  Kisten ziehbar, nicht auswerfbar) — die Gen-Buchführung bleibt so immer konsistent.

## Befehl
`/aethoscrops` (Alias `/acrops`), Permission `aethoscrops.admin` (Default op):
`chunkinfo` · `blockinfo` · `giveseed <id> [anzahl]` · `givediseasetool <disease-id>` · `reload`.
`reload` ist auch von der Server-Konsole/dem Aethos-Panel ausführbar.

## Aethos-Integration
Schreibt `aethos.desc` + `status.txt` in den Datenordner → erscheint im Aethos-Manager-Panel
(Status + Aktion „Config neu laden"). Plugin-Name trägt das Pflicht-Präfix `Aethos`.

## Repository
https://github.com/TyraelN/AethosCrops
