package de.aethos.crops;

import de.aethos.crops.crop.CropManager;
import de.aethos.crops.database.DataController;
import de.aethos.crops.gui.MühlenUI;
import de.aethos.crops.gui.UIListner;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AethosCrops extends JavaPlugin {
    private static final CropManager MANAGER = new CropManager();

    private static AethosCrops instance;

    private DataController dataController;

    public static AethosCrops getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        if (dataController != null) {
            dataController.stop();
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        dataController = new DataController(this);
        dataController.start();
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(MANAGER, this);
        manager.registerEvents(new UIListner(), this);
        manager.registerEvents(new MühlenUI(), this);

        getServer().getCommandMap().register(getName(), new CropsCommand(this));
    }

}

