package de.aethos.crops;

import de.aethos.crops.Events.CropBreakListener;
import de.aethos.crops.Events.CropGrowListener;
import de.aethos.crops.Events.CropPlantListener;
import de.aethos.crops.Events.DiseaseTreatmentListener;
import de.aethos.crops.Events.FarmlandTrampleListener;
import de.aethos.crops.Events.SeedStackListener;
import de.aethos.crops.Events.WaterDestroyCropListener;
import de.aethos.crops.Managers.ConfigManager;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Managers.DisplayManager;
import de.aethos.crops.Managers.SeedItemManager;
import de.aethos.crops.Testing.AdminCommands;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropRegistry;
import de.aethos.crops.Utils.Gen.GenRegistry;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class AethosCrops extends JavaPlugin {
    private static AethosCrops instance;
    public static DataManager dataManager;
    public static CropManager cropManager;
    public static DisplayManager displayManager;
    public static CropRegistry cropRegistry;
    public static ConfigManager configManager;
    public static GenRegistry genRegistry;
    public static SeedItemManager seedItemManager;

    @Override
    public void onEnable() {
        instance = this;

        dataManager = new DataManager(this);
        cropManager = new CropManager();
        seedItemManager = new SeedItemManager();
        displayManager = new DisplayManager();
        cropRegistry = new CropRegistry();
        genRegistry = new GenRegistry();
        configManager = new ConfigManager(this);
        configManager.loadCrops(cropRegistry);

        registerListeners();
        registerCommands();

        // Optionale GuiDesigner-Anbindung: nur registrieren, wenn das Plugin
        // da ist - die Hook-Klasse darf sonst nie geladen werden (Classpath).
        if (getServer().getPluginManager().getPlugin("AethosGuiDesigner") != null) {
            de.aethos.crops.Integration.GuiDesignerHook.register(this);
        }

        // Optionale AethosAchievements-Anbindung: die Hook-Klasse macht ihren
        // eigenen Praesenz-Check, daher unbedingt aufrufen.
        de.aethos.crops.Integration.AchievementsHook.init(this);

        // Aethos-Integration: Descriptor + Status fuer das Manager-Panel.
        getDataFolder().mkdirs();
        writeAethosDescriptor();
        writeStatus();
        getServer().getScheduler().runTaskTimer(this, this::writeStatus, 100L, 200L);
    }

    @Override
    public void onDisable() {

    }

    public static CropManager getCropManager() {
        return cropManager;
    }

    public static DisplayManager getDisplayManager() {
        return displayManager;
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static CropRegistry getCropRegistry() {
        return cropRegistry;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static GenRegistry getGenRegistry() {
        return genRegistry;
    }

    public static SeedItemManager getSeedItemManager() {
        return seedItemManager;
    }

    public static AethosCrops getInstance() {
        return instance;
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new CropBreakListener(), this);
        pluginManager.registerEvents(new CropGrowListener(), this);
        pluginManager.registerEvents(new CropPlantListener(), this);
        pluginManager.registerEvents(new DiseaseTreatmentListener(), this);
        pluginManager.registerEvents(new FarmlandTrampleListener(), this);
        pluginManager.registerEvents(new WaterDestroyCropListener(), this);
        pluginManager.registerEvents(new SeedStackListener(), this);
    }

    private void registerCommands() {
        // Paper-Plugins registrieren Befehle ueber die Brigadier-Lifecycle-API (nicht getCommand()).
        AdminCommands adminCommands = new AdminCommands();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "aethoscrops",
                        "Verwaltung/Test von AethosCrops",
                        List.of("acrops"),
                        new CropsCommand(adminCommands)));
    }

    private static final class CropsCommand implements BasicCommand {
        private final AdminCommands delegate;

        private CropsCommand(AdminCommands delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(CommandSourceStack source, String[] args) {
            delegate.execute(source.getSender(), args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return delegate.suggest(source.getSender(), args);
        }

        @Override
        public String permission() {
            // Permissions werden pro Subcommand in AdminCommands geprueft
            // (analyze -> aethoscrops.analyze, Rest -> aethoscrops.admin).
            return null;
        }
    }

    /* ============================ Aethos-Integration ============================ */

    private void writeAethosDescriptor() {
        write("aethos.desc", String.join("\n",
                "schema=1",
                "id=AethosCrops",
                "display=Aethos Crops",
                "version=" + getPluginMeta().getVersion(),
                "status=status.txt",
                "action=r|Config neu laden|aethoscrops reload",
                ""));
    }

    private void writeStatus() {
        write("status.txt", String.join("\n",
                "Status=aktiv",
                "Geladene Crops=" + (cropRegistry != null ? cropRegistry.size() : 0),
                "Krankheiten=" + (genRegistry != null ? genRegistry.getAll().size() : 0),
                ""));
    }

    private void write(String name, String content) {
        try { Files.writeString(getDataFolder().toPath().resolve(name), content, StandardCharsets.UTF_8); }
        catch (IOException e) { getLogger().warning("Konnte " + name + " nicht schreiben: " + e.getMessage()); }
    }
}
