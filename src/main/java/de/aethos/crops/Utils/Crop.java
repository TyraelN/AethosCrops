package de.aethos.crops.Utils;

import de.aethos.crops.Utils.Gen.IGen;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Crop {
    private final String id;
    private final String displayName;
    // Item-Model des Displays (z.B. "aethos:block/crops/barley"); die Stufe
    // waehlt das Stadium per custom_model_data-Float (siehe DisplayManager).
    private final String itemModel;
    // Validierter Model-Key der Samen-Items (Config 'seed-item-model', Default
    // aethos:item/crops/<id>_seeds); null = ungueltig -> Samen behalten Vanilla-Look.
    private final NamespacedKey seedItemModel;
    private final double growSpeed;
    private final DropTable dropTable;
    private final LinkedHashMap<IGen, Double> diseaseChances;
    private final int maxStage;
    private final CropTuning tuning;
    private int currentStage = 1;
    private double health = 100.0D;
    // Gene des gepflanzten Samens; 0 = neutraler Default (Alt-Daten, Vanilla-Verhalten).
    private SeedGenes genes = new SeedGenes(0, 0, 0);
    private final List<IGen> gens = new ArrayList<>();
    private final Map<String, Integer> diseaseLevels = new HashMap<>();

    public Crop(String id, String displayName, String itemModel, NamespacedKey seedItemModel, double growSpeed, DropTable dropTable, Map<IGen, Double> diseaseChances, int maxStage, CropTuning tuning) {
        this.id = id;
        this.displayName = displayName;
        this.itemModel = itemModel;
        this.seedItemModel = seedItemModel;
        this.growSpeed = growSpeed;
        this.dropTable = dropTable;
        this.diseaseChances = new LinkedHashMap<>(diseaseChances);
        this.maxStage = maxStage;
        this.tuning = tuning;
    }

    public CropTuning getTuning() {
        return tuning;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getItemModel() {
        return itemModel;
    }

    public NamespacedKey getSeedItemModel() {
        return seedItemModel;
    }

    public double getGrowSpeed() {
        return growSpeed;
    }

    public DropTable getDropTable() {
        return dropTable;
    }

    public List<IGen> getAllowedDiseases() {
        return Collections.unmodifiableList(new ArrayList<>(diseaseChances.keySet()));
    }

    public Map<IGen, Double> getDiseaseChances() {
        return Collections.unmodifiableMap(diseaseChances);
    }

    public int getMaxStage() {
        return maxStage;
    }

    public List<IGen> getDiseases() {
        return Collections.unmodifiableList(new ArrayList<>(gens));
    }

    // Bildet die eigene Stufe (1..maxStage) linear auf eine externe Skala 0..max
    // ab (Vanilla-Age bzw. Modell-Stadium). Den Endwert gibt es erst auf der
    // Max-Stufe, damit "ausgewachsen" aussen nie zu frueh erreicht wird.
    public int scaleStageTo(int max) {
        if (isAtMaxStage() || maxStage <= 1) {
            return max;
        }

        int scaled = Math.round((currentStage - 1) * (float) max / (maxStage - 1));
        return Math.min(scaled, max - 1);
    }

    public int getStage() {
        return currentStage;
    }

    public void incrementStage() {
        currentStage++;
    }

    public void addDisease(IGen gen) {
        if (!gens.contains(gen)) {
            gens.add(gen);
        }

        if (gen != null) {
            diseaseLevels.putIfAbsent(gen.getId(), 1);
        }
    }

    public void removeDisease(IGen gen) {
        gens.remove(gen);
        if (gen != null) {
            diseaseLevels.remove(gen.getId());
        }
    }

    public boolean isAtMaxStage() {
        return getStage() == getMaxStage();
    }

    public int getDiseaseLevel(IGen disease) {
        if (disease == null) {
            return 0;
        }

        return diseaseLevels.getOrDefault(disease.getId(), 0);
    }

    public void setDiseaseLevel(IGen disease, int level) {
        if (disease == null) {
            return;
        }

        if (level <= 0) {
            removeDisease(disease);
            return;
        }

        addDisease(disease);
        diseaseLevels.put(disease.getId(), Math.min(level, tuning.diseaseMaxLevel()));
    }

    public Map<String, Integer> getDiseaseLevels() {
        return Collections.unmodifiableMap(diseaseLevels);
    }

    public SeedGenes getGenes() {
        return genes;
    }

    public void setGenes(SeedGenes genes) {
        if (genes != null) {
            this.genes = genes;
        }
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0.0D, Math.min(100.0D, health));
    }

    public void reduceHealthPercent(double percent) {
        if (percent <= 0) {
            return;
        }

        setHealth(health - percent);
    }
}
