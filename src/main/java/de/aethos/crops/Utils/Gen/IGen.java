package de.aethos.crops.Utils.Gen;

import org.bukkit.entity.Player;

public interface IGen {
    String getId();

    String getDisplayName();

    void openGui(Player player);

}
