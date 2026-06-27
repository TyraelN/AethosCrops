package de.aethos.crops.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CropRegistry {

    private final Map<String, Crop> crops = new HashMap<>();

    public void register(Crop crop) {
        crops.put(crop.getId().toLowerCase(), crop);
    }

    public Crop get(String id) {
        if (id == null) return null;
        return crops.get(id.toLowerCase());
    }

    public boolean contains(String id) {
        return crops.containsKey(id.toLowerCase());
    }

    public Collection<Crop> getAll() {
        return crops.values();
    }

    public void clear() {
        crops.clear();
    }

    public int size() {
        return crops.size();
    }
}