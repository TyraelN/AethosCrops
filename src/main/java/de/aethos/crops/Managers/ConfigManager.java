package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.CropRegistry;
import de.aethos.crops.Utils.DropTable;
import de.aethos.crops.Utils.Gen.IGen;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final AethosCrops plugin;

    public ConfigManager(AethosCrops plugin) {
        this.plugin = plugin;
    }

    public double getWrongLeafHealthPenalty() {
        return Math.max(0.0D, plugin.getConfig().getDouble("disease-treatment.wrong-leaf-health-penalty", 2.0D));
    }

    public void loadCrops(CropRegistry cropRegistry) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection cropsSection = config.getConfigurationSection("crops");

        cropRegistry.clear();

        if (cropsSection == null) {
            plugin.getLogger().warning("No 'crops' section found in config.yml. No crops were loaded.");
            return;
        }

        int loaded = 0;

        for (String cropId : cropsSection.getKeys(false)) {
            ConfigurationSection cropSection = cropsSection.getConfigurationSection(cropId);
            if (cropSection == null) {
                continue;
            }

            Crop crop = parseCrop(cropId, cropSection);
            if (crop == null) {
                continue;
            }

            cropRegistry.register(crop);
            loaded++;
        }

        plugin.getLogger().info("Loaded " + loaded + " crops into registry.");
    }

    private Crop parseCrop(String cropId, ConfigurationSection cropSection) {
        String displayName = cropSection.getString("display-name", cropId);
        int maxStage = cropSection.getInt("max-stage", 7);
        double growSpeed = cropSection.getDouble("grow-speed", 1.0D);

        if (maxStage < 1) {
            plugin.getLogger().warning("Skipping crop '" + cropId + "': 'max-stage' must be at least 1.");
            return null;
        }

        if (growSpeed <= 0) {
            plugin.getLogger().warning("Skipping crop '" + cropId + "': 'grow-speed' must be greater than 0.");
            return null;
        }

        List<String> stageModelPaths = parseStageModelPaths(cropId, cropSection, maxStage);
        if (stageModelPaths == null) {
            return null;
        }

        Map<IGen, Double> diseaseChances = parseDiseaseChances(cropId, cropSection);
        DropTable dropTable = parseDropTable(cropId, cropSection.getConfigurationSection("drops"));

        return new Crop(cropId, displayName, stageModelPaths, growSpeed, dropTable, diseaseChances, maxStage);
    }

    private List<String> parseStageModelPaths(String cropId, ConfigurationSection cropSection, int maxStage) {
        List<String> configuredPaths = cropSection.getStringList("stage-model-paths");
        if (!configuredPaths.isEmpty()) {
            if (configuredPaths.size() < maxStage) {
                plugin.getLogger().warning("Skipping crop '" + cropId + "': 'stage-model-paths' must contain at least 'max-stage' entries.");
                return null;
            }

            List<String> stageModelPaths = new ArrayList<>();
            for (int i = 0; i < maxStage; i++) {
                String path = configuredPaths.get(i);
                if (path == null || path.isBlank()) {
                    plugin.getLogger().warning("Skipping crop '" + cropId + "': 'stage-model-paths' contains blank value at stage " + (i + 1) + ".");
                    return null;
                }

                stageModelPaths.add(path);
            }

            return stageModelPaths;
        }

        String modelBasePath = cropSection.getString("model-base-path");
        if (modelBasePath == null || modelBasePath.isBlank()) {
            plugin.getLogger().warning("Skipping crop '" + cropId + "': missing 'stage-model-paths' (or legacy 'model-base-path').");
            return null;
        }

        List<String> fallbackPaths = new ArrayList<>();
        for (int i = 1; i <= maxStage; i++) {
            fallbackPaths.add(modelBasePath + "/stage" + i);
        }

        return fallbackPaths;
    }


    private Map<IGen, Double> parseDiseaseChances(String cropId, ConfigurationSection cropSection) {
        Map<IGen, Double> diseases = new LinkedHashMap<>();

        ConfigurationSection diseaseSection = cropSection.getConfigurationSection("allowed-diseases");
        if (diseaseSection != null) {
            for (String diseaseId : diseaseSection.getKeys(false)) {
                IGen disease = AethosCrops.getGenRegistry().findById(diseaseId);
                if (disease == null) {
                    plugin.getLogger().warning("Skipping unknown disease id '" + diseaseId + "' for crop '" + cropId + "'.");
                    continue;
                }

                double chance = diseaseSection.getDouble(diseaseId, 1.0D);
                if (chance < 0 || chance > 1) {
                    plugin.getLogger().warning("Skipping disease id '" + diseaseId + "' for crop '" + cropId + "': chance must be between 0 and 1.");
                    continue;
                }

                diseases.put(disease, chance);
            }
        }

        if (!diseases.isEmpty()) {
            return diseases;
        }

        List<String> legacyDiseaseIds = cropSection.getStringList("allowed-disease-ids");
        for (String diseaseId : legacyDiseaseIds) {
            IGen disease = AethosCrops.getGenRegistry().findById(diseaseId);
            if (disease == null) {
                plugin.getLogger().warning("Skipping unknown disease id '" + diseaseId + "' for crop '" + cropId + "'.");
                continue;
            }

            diseases.put(disease, 1.0D);
        }

        return diseases;
    }

    private DropTable parseDropTable(String cropId, ConfigurationSection dropsSection) {
        DropTable dropTable = new DropTable();

        if (dropsSection == null) {
            return dropTable;
        }

        ConfigurationSection entriesSection = dropsSection.getConfigurationSection("entries");
        if (entriesSection == null) {
            return dropTable;
        }

        for (String entryId : entriesSection.getKeys(false)) {
            ConfigurationSection entrySection = entriesSection.getConfigurationSection(entryId);
            if (entrySection == null) {
                continue;
            }

            String materialRaw = entrySection.getString("material");
            Material material = materialRaw == null ? null : Material.matchMaterial(materialRaw);
            int amount = entrySection.getInt("amount", 1);
            double chance = entrySection.getDouble("chance", 1.0D);

            if (material == null) {
                plugin.getLogger().warning("Skipping drop entry '" + entryId + "' for crop '" + cropId + "': invalid material '" + materialRaw + "'.");
                continue;
            }

            if (amount <= 0) {
                plugin.getLogger().warning("Skipping drop entry '" + entryId + "' for crop '" + cropId + "': 'amount' must be at least 1.");
                continue;
            }

            if (chance < 0 || chance > 1) {
                plugin.getLogger().warning("Skipping drop entry '" + entryId + "' for crop '" + cropId + "': 'chance' must be between 0 and 1.");
                continue;
            }

            dropTable.add(new ItemStack(material, amount), chance);
        }

        return dropTable;
    }

    public ItemStack createDiseaseToolItem(String diseaseId) {
        if (diseaseId == null || diseaseId.isBlank()) {
            return null;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("disease-treatment.items." + diseaseId.toLowerCase());
        return createDiseaseItemFromSection(diseaseId, section, Material.STICK, CropKey.DISEASE_TOOL, null, null);
    }

    public ItemStack createDiseaseGuiItem(String diseaseId) {
        if (diseaseId == null || diseaseId.isBlank()) {
            return null;
        }

        String normalizedDiseaseId = diseaseId.toLowerCase();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("disease-treatment.gui-items." + normalizedDiseaseId);

        Material defaultMaterial = defaultGuiMaterialForDisease(normalizedDiseaseId);
        String defaultDisplayName = defaultGuiDisplayNameForDisease(normalizedDiseaseId);
        List<String> defaultLore = Collections.singletonList(ChatColor.GRAY + "Find and remove this symptom.");

        return createDiseaseItemFromSection(
                normalizedDiseaseId,
                section,
                defaultMaterial,
                CropKey.DISEASE_GUI_ITEM,
                defaultDisplayName,
                defaultLore
        );
    }

    private ItemStack createDiseaseItemFromSection(
            String diseaseId,
            ConfigurationSection section,
            Material fallbackMaterial,
            org.bukkit.NamespacedKey markerKey,
            String fallbackDisplayName,
            List<String> fallbackLore
    ) {
        Material material = fallbackMaterial;
        int amount = 1;
        String displayName = fallbackDisplayName;
        List<String> lore = fallbackLore;
        Integer customModelData = null;
        String itemModelPath = null;

        if (section == null) {
            return buildDiseaseItem(diseaseId, material, amount, displayName, lore, customModelData, itemModelPath, markerKey);
        }

        Material sectionMaterial = Material.matchMaterial(section.getString("material", fallbackMaterial.name()));
        material = sectionMaterial;
        if (material == null || material.isAir()) {
            material = fallbackMaterial;
        }

        amount = Math.max(1, section.getInt("amount", amount));
        String sectionDisplayName = section.getString("display-name");
        if (sectionDisplayName != null && !sectionDisplayName.isBlank()) {
            displayName = sectionDisplayName;
        }

        List<String> configuredLore = section.getStringList("lore");
        if (!configuredLore.isEmpty()) {
            lore = configuredLore;
        }

        if (section.contains("custom-model-data")) {
            customModelData = section.getInt("custom-model-data");
        }

        String configuredModelPath = section.getString("item-model-path");
        if (configuredModelPath != null && !configuredModelPath.isBlank()) {
            itemModelPath = configuredModelPath;
        }

        return buildDiseaseItem(diseaseId, material, amount, displayName, lore, customModelData, itemModelPath, markerKey);
    }

    private ItemStack buildDiseaseItem(
            String diseaseId,
            Material material,
            int amount,
            String displayName,
            List<String> lore,
            Integer customModelData,
            String itemModelPath,
            org.bukkit.NamespacedKey markerKey
    ) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (displayName != null && !displayName.isBlank()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }

        if (lore != null && !lore.isEmpty()) {
            List<String> translatedLore = lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList();
            meta.setLore(translatedLore);
        }

        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        if (itemModelPath != null && !itemModelPath.isBlank()) {
            meta.getPersistentDataContainer().set(CropKey.MODEL_PATH, PersistentDataType.STRING, itemModelPath);
        }

        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, diseaseId.toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    private Material defaultGuiMaterialForDisease(String diseaseId) {
        Map<String, Material> materials = new HashMap<>();
        materials.put("weed", Material.SHORT_GRASS);
        materials.put("fungalinfection", Material.RED_MUSHROOM);
        materials.put("bugs", Material.COBWEB);
        return materials.getOrDefault(diseaseId, Material.DEAD_BUSH);
    }

    private String defaultGuiDisplayNameForDisease(String diseaseId) {
        return switch (diseaseId) {
            case "weed" -> "&aWeed";
            case "fungalinfection" -> "&6Fungus";
            case "bugs" -> "&cInfesting Bugs";
            default -> "&7Disease Symptom";
        };
    }
}
