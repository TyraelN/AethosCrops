package de.aethos.crops.Utils.Gen;

import org.bukkit.entity.Player;

public abstract class BaseDiseaseGen implements IGen {
    private final String id;
    private final String displayName;

    protected BaseDiseaseGen(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void openGui(Player player) {
        // handled by dedicated disease treatment listener
    }
}
