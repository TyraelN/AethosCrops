package de.aethos.crops.Utils.Gen;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GenRegistry {
    private final Map<String, IGen> gens = new LinkedHashMap<>();

    public GenRegistry() {
        register(new WeedDiseaseGen());
        register(new BugsDiseaseGen());
        register(new FungalInfectionDiseaseGen());
    }

    public void register(IGen gen) {
        if (gen == null || gen.getId() == null || gen.getId().isBlank()) {
            return;
        }

        gens.put(gen.getId().toLowerCase(), gen);
    }

    public IGen findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return gens.get(id.toLowerCase());
    }

    public Collection<IGen> getAll() {
        return Collections.unmodifiableCollection(gens.values());
    }
}
