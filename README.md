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

## Befehl
`/aethoscrops` (Alias `/acrops`), Permission `aethoscrops.admin` (Default op):
`chunkinfo` · `blockinfo` · `giveseed <id>` · `givediseasetool <disease-id>` · `reload`.
`reload` ist auch von der Server-Konsole/dem Aethos-Panel ausführbar.

## Aethos-Integration
Schreibt `aethos.desc` + `status.txt` in den Datenordner → erscheint im Aethos-Manager-Panel
(Status + Aktion „Config neu laden"). Plugin-Name trägt das Pflicht-Präfix `Aethos`.

## Herkunft / Lizenz
Ursprünglich entwickelt von **Luis-GameDev** (https://github.com/Luis-GameDev/Aethos-Crops).
Für Aethos sicherheitsgeprüft, auf das `de.aethos`-Paket umgezogen, auf die Aethos-Konventionen
(paper-plugin.yml, Brigadier-Command, Descriptor, Reposilite) umgestellt und in `AethosCrops`
umbenannt.
