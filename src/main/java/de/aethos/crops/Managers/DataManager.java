package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.Gen.IGen;
import de.aethos.crops.Utils.SeedGenes;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataManager {
    private static final Pattern KEY_REGEX = Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
    private static final String DISPLAY_SUFFIX = "/display";
    private final Plugin plugin;

    public DataManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void saveCrop(Block block, Crop crop) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getKey(block));

        pdc.set(key, PersistentDataType.STRING, serialize(crop));
    }

    // Updates an existing crop entry
    public void updateCrop(Block block, Crop crop) {
        removeCrop(block);
        saveCrop(block, crop);
    }

    @Nullable
    public Crop loadCrop(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getKey(block));

        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        return deserialize(raw);
    }

    public boolean hasCrop(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getKey(block));

        return pdc.has(key, PersistentDataType.STRING);
    }

    public void removeCrop(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getKey(block));

        pdc.remove(key);
    }

    public void saveDisplayId(Block block, UUID entityId) {
        if (block == null || entityId == null) {
            return;
        }

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getDisplayKey(block));

        pdc.set(key, PersistentDataType.STRING, entityId.toString());
    }

    @Nullable
    public UUID loadDisplayId(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getDisplayKey(block));

        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            clearDisplayId(block);
            return null;
        }
    }

    public void clearDisplayId(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, getDisplayKey(block));

        pdc.remove(key);
    }

    public Map<Block, Crop> loadAllCrops(Chunk chunk) {
        Map<Block, Crop> crops = new HashMap<>();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        for (NamespacedKey key : new HashSet<>(pdc.getKeys())) {

            if (!key.getNamespace().equalsIgnoreCase(plugin.getName())) {
                continue;
            }

            if (key.getKey().endsWith(DISPLAY_SUFFIX)) {
                String blockKey = key.getKey().substring(0, key.getKey().length() - DISPLAY_SUFFIX.length());
                NamespacedKey cropKey = new NamespacedKey(plugin, blockKey);
                if (!pdc.has(cropKey, PersistentDataType.STRING)) {
                    pdc.remove(key);
                }
                continue;
            }

            if (!KEY_REGEX.matcher(key.getKey()).matches()) {
                continue;
            }

            String raw = pdc.get(key, PersistentDataType.STRING);
            if (raw == null || raw.isEmpty()) {
                pdc.remove(key);
                pdc.remove(new NamespacedKey(plugin, key.getKey() + DISPLAY_SUFFIX));
                continue;
            }

            Block block = getBlockFromKey(key, chunk);
            if (block == null) {
                pdc.remove(key);
                pdc.remove(new NamespacedKey(plugin, key.getKey() + DISPLAY_SUFFIX));
                continue;
            }

            Crop crop = deserialize(raw);
            if (crop == null) {
                pdc.remove(key);
                pdc.remove(new NamespacedKey(plugin, key.getKey() + DISPLAY_SUFFIX));
                continue;
            }

            crops.put(block, crop);
        }

        return crops;
    }

    public static String getKey(final Block block) {
        final int x = block.getX() & 15;
        final int y = block.getY();
        final int z = block.getZ() & 15;
        return "x" + x + "y" + y + "z" + z;
    }

    private static String getDisplayKey(final Block block) {
        return getKey(block) + DISPLAY_SUFFIX;
    }

    @Nullable
    public static Block getBlockFromKey(final NamespacedKey key, final Chunk chunk) {
        final Matcher matcher = KEY_REGEX.matcher(key.getKey());

        if (matcher.matches()) {

            final int x = Integer.parseInt(matcher.group(1));
            final int y = Integer.parseInt(matcher.group(2));
            final int z = Integer.parseInt(matcher.group(3));

            return x >= 0 && x <= 15 &&
                    z >= 0 && z <= 15 &&
                    y >= chunk.getWorld().getMinHeight() &&
                    y <= chunk.getWorld().getMaxHeight() - 1
                    ? chunk.getBlock(x, y, z)
                    : null;
        }

        return null;
    }

    private String serialize(Crop crop) {
        StringJoiner diseaseData = new StringJoiner(",");
        crop.getDiseaseLevels().forEach((diseaseId, level) -> diseaseData.add(diseaseId + "=" + level));
        return crop.getId() + ";" + crop.getStage() + ";" + crop.getHealth() + ";" + diseaseData
                + ";" + crop.getGenes().serialize();
    }

    @Nullable
    private Crop deserialize(String raw) {

        String[] split = raw.split(";", -1);
        if (split.length < 2) {
            return null;
        }

        String id = split[0];

        int stage;
        try {
            stage = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        Crop crop = AethosCrops.getCropRegistry().get(id);
        if (crop == null) {
            return null;
        }

        Crop instance = new Crop(
                crop.getId(),
                crop.getDisplayName(),
                crop.getStageModelPaths(),
                crop.getGrowSpeed(),
                crop.getDropTable(),
                crop.getDiseaseChances(),
                crop.getMaxStage()
        );

        for (int i = 1; i < stage; i++) {
            instance.incrementStage();
        }

        if (split.length >= 3) {
            try {
                instance.setHealth(Double.parseDouble(split[2]));
            } catch (NumberFormatException ignored) {
                instance.setHealth(100.0D);
            }
        }

        if (split.length >= 5) {
            SeedGenes genes = SeedGenes.deserialize(split[4]);
            if (genes != null) {
                instance.setGenes(genes);
            }
        }

        if (split.length >= 4 && !split[3].isBlank()) {
            String[] diseases = split[3].split(",");
            for (String diseaseData : diseases) {
                String[] diseaseSplit = diseaseData.split("=", 2);
                if (diseaseSplit.length != 2) {
                    continue;
                }

                IGen disease = AethosCrops.getGenRegistry().findById(diseaseSplit[0]);
                if (disease == null) {
                    continue;
                }

                try {
                    int level = Integer.parseInt(diseaseSplit[1]);
                    instance.setDiseaseLevel(disease, level);
                } catch (NumberFormatException ignored) {
                    // ignore malformed disease level
                }
            }
        }

        return instance;
    }
}
