package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.SeedGenes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Verwaltet Samen-ItemStacks inklusive Gen-Buchfuehrung.
 * <p>
 * Ein Stack traegt im PDC eine Liste von Gen-Eintraegen - genau einen pro Samen
 * im Stack ("120.200.80;99.100.101;..."). Name und Lore haengen nur von Crop-ID
 * und Sterne-Guete ab, sodass Stacks gleicher Guete optisch identisch sind.
 * <p>
 * Invarianten:
 * - Alle Eintraege eines Stacks haben dieselbe Sterne-Guete.
 * - Die Listenlaenge entspricht der Stack-Groesse.
 * - Verbraucht (gepflanzt, einzeln gedroppt, abgespalten) wird immer vom ENDE
 *   der Liste. Die Reparatur bei Inkonsistenz kuerzt deshalb ebenfalls am Ende
 *   bzw. fuellt zyklisch auf - so heilt sich auch Vanilla-Stacking von exakt
 *   identischen Stacks korrekt (identische Eintraege werden dupliziert).
 */
public class SeedItemManager {

    private static final String ENTRY_SEPARATOR = ";";

    /* ============================ Erzeugung ============================ */

    // Baut einen Samen-Stack; ein Gen-Eintrag pro Samen. Alle Eintraege muessen
    // dieselbe Guete haben (Aufrufer gruppiert vorher nach Sternen).
    public ItemStack createSeedStack(Crop crop, List<SeedGenes> genes) {
        if (crop == null || genes == null || genes.isEmpty()) {
            return null;
        }

        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS, genes.size());
        ItemMeta meta = seed.getItemMeta();
        if (meta == null) {
            return seed;
        }

        int stars = genes.get(0).getStars();
        meta.displayName(seedName(crop));
        meta.lore(List.of(
                Component.translatable("item.aethos.seed.quality")
                        .fallback("Güte: %s")
                        .arguments(starsComponent(stars))
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.translatable("item.aethos.seed.lore")
                        .fallback("Aethos-Pflanzensamen")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(CropKey.CROP_TYPE, PersistentDataType.STRING, crop.getId());
        meta.getPersistentDataContainer().set(CropKey.SEED_GENES, PersistentDataType.STRING, serialize(genes));
        // Saatgut-Optik per item_model (beim Config-Laden validiert + gecacht; Default
        // aethos:item/crops/<id>_seeds, per 'seed-item-model' uebersteuerbar). null =
        // ungueltiger Key, wurde beim Laden gewarnt -> Samen behaelt den Vanilla-Look.
        NamespacedKey seedModel = crop.getSeedItemModel();
        if (seedModel != null) meta.setItemModel(seedModel);
        seed.setItemMeta(meta);
        return seed;
    }

    // Tier-Farben der Guete: 1-2 Kupfer, 3-4 Silber, 5 Gold.
    private static final TextColor COLOR_COPPER = TextColor.color(0xB87333);
    private static final TextColor COLOR_SILVER = TextColor.color(0xC0C0C0);
    private static final TextColor COLOR_GOLD = TextColor.color(0xFFD700);

    // Name als TranslatableComponent: Uebersetzung liefert das Resource Pack
    // (Key "item.aethos.seed" mit dem Crop-Namen "crop.aethos.<id>" als Argument);
    // der Fallback ist die deutsche Version aus der Config.
    private Component seedName(Crop crop) {
        Component cropName = Component.translatable("crop.aethos." + crop.getId())
                .fallback(crop.getDisplayName())
                .color(NamedTextColor.GREEN);

        return Component.translatable("item.aethos.seed")
                .fallback("%s-Samen")
                .arguments(cropName)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component starsComponent(int stars) {
        int clamped = Math.max(1, Math.min(5, stars));
        TextColor tierColor = clamped >= 5 ? COLOR_GOLD : clamped >= 3 ? COLOR_SILVER : COLOR_COPPER;
        return Component.text("★".repeat(clamped)).color(tierColor)
                .append(Component.text("☆".repeat(5 - clamped)).color(NamedTextColor.DARK_GRAY));
    }

    // Legacy-Variante fuer Chat-Ausgaben (AdminCommands); behaelt die exakten
    // Hex-Farben als §x-Sequenz statt auf Named-Colors runterzurechnen.
    private static final LegacyComponentSerializer LEGACY_HEX = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static String formatStars(int stars) {
        return LEGACY_HEX.serialize(starsComponent(stars));
    }

    /* ============================ Lesen/Schreiben ============================ */

    // Samen-Erkennung fuer die Stack-Verwaltung: nur der crop_type-Tag zaehlt,
    // damit auch Samen (noch) unbekannter Crops konsistent verwaltet werden.
    public boolean isSeed(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        String cropId = meta.getPersistentDataContainer().get(CropKey.CROP_TYPE, PersistentDataType.STRING);
        return cropId != null && !cropId.isBlank();
    }

    public String getCropId(ItemStack stack) {
        if (!isSeed(stack)) {
            return null;
        }

        return stack.getItemMeta().getPersistentDataContainer().get(CropKey.CROP_TYPE, PersistentDataType.STRING);
    }

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

    // Liefert die Gen-Liste des Stacks, repariert auf Stack-Groesse:
    // zu lang -> am Ende kuerzen, zu kurz -> zyklisch auffuellen,
    // leer (Alt-Samen ohne Gene) -> ein Zufallsgen fuer alle Samen.
    public List<SeedGenes> getGenes(ItemStack stack) {
        if (!isSeed(stack)) {
            return new ArrayList<>();
        }

        List<SeedGenes> genes = readRawGenes(stack);
        int amount = stack.getAmount();

        if (genes.isEmpty()) {
            // Alt-Samen ohne Gen-Daten: neutraler Default 0 (Vanilla-Verhalten),
            // sofort persistieren, damit alle Lesezugriffe dasselbe sehen.
            SeedGenes legacy = new SeedGenes(0, 0, 0);
            for (int i = 0; i < amount; i++) {
                genes.add(legacy);
            }
            setGenes(stack, genes);
            return genes;
        }

        if (genes.size() == amount) {
            return genes;
        }

        while (genes.size() > amount) {
            genes.remove(genes.size() - 1);
        }

        int existing = genes.size();
        for (int i = 0; genes.size() < amount; i++) {
            genes.add(genes.get(i % existing));
        }

        setGenes(stack, genes);
        return genes;
    }

    public void setGenes(ItemStack stack, List<SeedGenes> genes) {
        if (stack == null || genes == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(CropKey.SEED_GENES, PersistentDataType.STRING, serialize(genes));
        stack.setItemMeta(meta);
    }

    // Guete des Stacks = Guete des ersten Eintrags (alle Eintraege sind gleichwertig).
    public int getStars(ItemStack stack) {
        List<SeedGenes> genes = getGenes(stack);
        return genes.isEmpty() ? 0 : genes.get(0).getStars();
    }

    /* ============================ Stack-Operationen ============================ */

    public boolean canMerge(ItemStack a, ItemStack b) {
        if (!isSeed(a) || !isSeed(b)) {
            return false;
        }

        // Analysierte Samen sind vereinzelt und stacken grundsaetzlich nicht.
        if (isAnalyzed(a) || isAnalyzed(b)) {
            return false;
        }

        String cropA = getCropId(a);
        String cropB = getCropId(b);
        if (cropA == null || !cropA.equalsIgnoreCase(cropB)) {
            return false;
        }

        return getStars(a) == getStars(b);
    }

    // Verschiebt so viele Samen wie moeglich von 'from' nach 'into' (inkl. ihrer
    // Gen-Eintraege, vom Ende von 'from' ans Ende von 'into'). Gibt die Anzahl
    // der verschobenen Samen zurueck; beide Stacks werden in-place angepasst.
    public int merge(ItemStack from, ItemStack into) {
        if (!canMerge(from, into)) {
            return 0;
        }

        int space = into.getMaxStackSize() - into.getAmount();
        int move = Math.min(space, from.getAmount());
        if (move <= 0) {
            return 0;
        }

        List<SeedGenes> fromGenes = getGenes(from);
        List<SeedGenes> intoGenes = getGenes(into);

        List<SeedGenes> moved = new ArrayList<>(fromGenes.subList(fromGenes.size() - move, fromGenes.size()));
        List<SeedGenes> remaining = new ArrayList<>(fromGenes.subList(0, fromGenes.size() - move));

        intoGenes.addAll(moved);
        into.setAmount(into.getAmount() + move);
        setGenes(into, intoGenes);

        from.setAmount(from.getAmount() - move);
        if (from.getAmount() > 0) {
            setGenes(from, remaining);
        }

        return move;
    }

    // Spaltet 'take' Samen (vom Ende der Liste) als neuen Stack ab.
    // Der Ursprungsstack wird in-place verkleinert (Amount kann 0 werden -
    // der Aufrufer ersetzt ihn dann durch null/AIR).
    public ItemStack takeFromStack(ItemStack stack, int take) {
        if (!isSeed(stack) || take <= 0) {
            return null;
        }

        int actualTake = Math.min(take, stack.getAmount());
        List<SeedGenes> genes = getGenes(stack);

        List<SeedGenes> taken = new ArrayList<>(genes.subList(genes.size() - actualTake, genes.size()));
        List<SeedGenes> remaining = new ArrayList<>(genes.subList(0, genes.size() - actualTake));

        ItemStack result = stack.clone();
        result.setAmount(actualTake);
        setGenes(result, taken);

        stack.setAmount(stack.getAmount() - actualTake);
        if (stack.getAmount() > 0) {
            setGenes(stack, remaining);
        }

        return result;
    }

    // Kuerzt die Gen-Liste am Ende auf die Stack-Groesse (nach Verbrauch, z.B.
    // Pflanzen). Gibt true zurueck, wenn etwas korrigiert wurde.
    public boolean trimToAmount(ItemStack stack) {
        if (!isSeed(stack)) {
            return false;
        }

        List<SeedGenes> raw = readRawGenes(stack);
        if (raw.size() <= stack.getAmount()) {
            return false;
        }

        while (raw.size() > stack.getAmount()) {
            raw.remove(raw.size() - 1);
        }

        setGenes(stack, raw);
        return true;
    }

    // Fuehrt alle Samen-Stacks gleicher Crop-ID und Guete in einem Inventar
    // zusammen (fruehe Slots werden aus spaeteren aufgefuellt).
    public void consolidate(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        Map<String, List<Integer>> openSlots = new HashMap<>();
        ItemStack[] contents = inventory.getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!isSeed(stack) || isAnalyzed(stack)) {
                continue;
            }

            String groupKey = getCropId(stack).toLowerCase() + "#" + getStars(stack);
            List<Integer> candidates = openSlots.computeIfAbsent(groupKey, key -> new ArrayList<>());

            for (Integer targetSlot : candidates) {
                ItemStack target = contents[targetSlot];
                if (target == null || target.getAmount() >= target.getMaxStackSize()) {
                    continue;
                }

                merge(stack, target);
                if (stack.getAmount() <= 0) {
                    break;
                }
            }

            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            } else {
                candidates.add(slot);
            }
        }

        inventory.setStorageContents(contents);
    }

    /* ============================ intern ============================ */

    // Rohliste ohne Reparatur - fuer Operationen, die die Diskrepanz zwischen
    // Listenlaenge und Stack-Groesse selbst aufloesen (Drop-Fix, trimToAmount).
    public List<SeedGenes> readRawGenes(ItemStack stack) {
        List<SeedGenes> genes = new ArrayList<>();
        if (stack == null) {
            return genes;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return genes;
        }

        String raw = meta.getPersistentDataContainer().get(CropKey.SEED_GENES, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return genes;
        }

        for (String entry : raw.split(ENTRY_SEPARATOR)) {
            SeedGenes parsed = SeedGenes.deserialize(entry);
            if (parsed != null) {
                genes.add(parsed);
            }
        }

        return genes;
    }

    private String serialize(List<SeedGenes> genes) {
        StringJoiner joiner = new StringJoiner(ENTRY_SEPARATOR);
        for (SeedGenes gene : genes) {
            joiner.add(gene.serialize());
        }
        return joiner.toString();
    }
}
