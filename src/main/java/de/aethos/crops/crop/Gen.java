package de.aethos.crops.crop;

import de.aethos.crops.AethosCrops;
import org.bukkit.NamespacedKey;

public enum Gen {
    KAEFER("Käfer"),
    PILZ("Pilz"),
    UNKRAUT("Unkraut"),
    WACHSTUM("Wachstum"),
    MENGE("Menge"),
    AUSDAUER("Ausdauer");

    private final NamespacedKey key = new NamespacedKey(AethosCrops.getInstance(), name());

    private final String displayName;

    Gen(String displayName) {
        this.displayName = displayName;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }
}


