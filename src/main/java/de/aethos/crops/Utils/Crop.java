package de.aethos.crops.Utils;

import de.aethos.crops.Utils.Gen.IGen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Crop {
    private final String id;
    private final String displayName;
    private final List<String> stageModelPaths;
    private final double growSpeed;
    private final DropTable dropTable;
    private final LinkedHashMap<IGen, Double> diseaseChances;
    private final int maxStage;
    private int currentStage = 1;
    private double health = 100.0D;
    // Gene des gepflanzten Samens; 0 = neutraler Default (Alt-Daten, Vanilla-Verhalten).
    private SeedGenes genes = new SeedGenes(0, 0, 0);
    private final List<IGen> gens = new ArrayList<>();
    private final Map<String, Integer> diseaseLevels = new HashMap<>();

    public Crop(String id, String displayName, List<String> stageModelPaths, double growSpeed, DropTable dropTable, Map<IGen, Double> diseaseChances, int maxStage) {
        this.id = id;
        this.displayName = displayName;
        this.stageModelPaths = List.copyOf(stageModelPaths);
        this.growSpeed = growSpeed;
        this.dropTable = dropTable;
        this.diseaseChances = new LinkedHashMap<>(diseaseChances);
        this.maxStage = maxStage;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getStageModelPaths() {
        return stageModelPaths;
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

    public String getModelPathForStage(int stage) {
        int clampedStage = Math.max(1, Math.min(stage, maxStage));
        return stageModelPaths.get(clampedStage - 1);
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
        diseaseLevels.put(disease.getId(), Math.min(level, 5));
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
