package de.aethos.crops.Integration;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.Gen.IGen;
import de.aethos.crops.Utils.SeedGenes;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Optionale Anbindung an AethosDebug: registriert einen F3-Debug-Provider, der
 * den anvisierten Aethos-Crop anzeigt. Ist das Plugin nicht installiert, sind
 * alle Aufrufe No-ops. Die API-Klassen werden nur ueber die innere Bridge
 * beruehrt, die erst bei aktivem Plugin classgeloadet wird (Ladeguard wie
 * AchievementsHook/GuiDesignerHook).
 */
public final class DebugHook {

    static final String NAMESPACE = "aethoscrops";

    private static boolean active;

    private DebugHook() {
    }

    public static void init(JavaPlugin plugin) {
        active = plugin.getServer().getPluginManager().getPlugin("AethosDebug") != null;
        if (active) {
            Bridge.register();
            plugin.getLogger().info("AethosDebug angebunden - F3-Debug verfuegbar.");
        }
    }

    public static void shutdown() {
        if (active) {
            Bridge.unregister();
        }
    }

    /**
     * Ladeguard: beruehrt die AethosDebug-API erst, wenn das Plugin da ist.
     */
    private static final class Bridge {

        static void register() {
            de.tyrael.aethosdebug.api.AethosDebugApi.register(new CropProvider());
        }

        static void unregister() {
            de.tyrael.aethosdebug.api.AethosDebugApi.unregister(NAMESPACE);
        }
    }

    /**
     * F3-Provider: liest ausschliesslich In-Memory-State des anvisierten Blocks
     * (ein Chunk-PDC-Lookup, kein Scan) - guenstig genug fuer die haeufigen
     * block()-Aufrufe bei Fadenkreuz-Bewegung.
     */
    private static final class CropProvider implements de.tyrael.aethosdebug.api.DebugProvider {

        @Override
        public String namespace() {
            return NAMESPACE;
        }

        @Override
        public List<de.tyrael.aethosdebug.api.DebugLine> block(Player player, World world, int x, int y, int z) {
            // Nur geladene Chunks anfassen - kein Nachladen fuer die Anzeige.
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return List.of();
            }

            Block block = world.getBlockAt(x, y, z);
            Crop crop = AethosCrops.getDataManager().loadCrop(block);
            if (crop == null) {
                return List.of();
            }

            List<de.tyrael.aethosdebug.api.DebugLine> lines = new ArrayList<>(5);
            lines.add(new de.tyrael.aethosdebug.api.DebugLine("crop", "Crop", crop.getId()));
            lines.add(new de.tyrael.aethosdebug.api.DebugLine("stage", "Stufe",
                    crop.getStage() + "/" + crop.getMaxStage()));

            SeedGenes genes = crop.getGenes();
            lines.add(new de.tyrael.aethosdebug.api.DebugLine("genes", "Gene",
                    "Wuchs " + genes.getGrowth()
                            + " | Ertrag " + genes.getYield()
                            + " | Res " + genes.getResistance()
                            + " | " + genes.getStars() + "★"));

            lines.add(new de.tyrael.aethosdebug.api.DebugLine("health", "Gesundheit",
                    Math.round(crop.getHealth()) + "%"));

            List<IGen> diseases = crop.getDiseases();
            if (!diseases.isEmpty()) {
                StringJoiner joiner = new StringJoiner(", ");
                for (IGen disease : diseases) {
                    joiner.add(disease.getDisplayName() + " Lv" + crop.getDiseaseLevel(disease));
                }
                lines.add(new de.tyrael.aethosdebug.api.DebugLine("diseases", "Krankheiten", joiner.toString()));
            }

            return lines;
        }
    }
}
