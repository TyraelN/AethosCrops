package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.CropRegistry;
import de.aethos.crops.Utils.CropTuning;
import de.aethos.crops.Utils.DropTable;
import de.aethos.crops.Utils.Gen.IGen;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    // Maximale Abweichung je Gen-Wert (0-255) bei der Vererbung an geerntete Samen.
    public int getMutationSpread() {
        int spread = plugin.getConfig().getInt("genetics.mutation-spread", 12);
        return Math.max(0, Math.min(127, spread));
    }

    // Ernte wirft immer mindestens einen vererbten Samen ab.
    public boolean isGuaranteedSeedDrop() {
        return plugin.getConfig().getBoolean("harvest.guaranteed-seed", true);
    }

    // true: Trampeln prallt an Aethos-Pflanzen ab; false: Pflanze wird zerstoert (mit Drop).
    public boolean isTrampleProtected() {
        return plugin.getConfig().getBoolean("protection.cancel-trample", true);
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

        String itemModel = parseItemModel(cropId, cropSection);
        if (itemModel == null) {
            return null;
        }

        Map<IGen, Double> diseaseChances = parseDiseaseChances(cropId, cropSection);
        DropTable dropTable = parseDropTable(cropId, cropSection.getConfigurationSection("drops"));
        CropTuning tuning = parseTuning(cropSection);

        return new Crop(cropId, displayName, itemModel, parseSeedItemModel(cropId, cropSection),
                growSpeed, dropTable, diseaseChances, maxStage, tuning);
    }

    // Balancing pro Crop; fehlende Werte fallen auf die globalen Abschnitte
    // 'genetics' bzw. 'diseases' zurueck (crops.<id>.genetics/diseases ueberschreibt).
    private CropTuning parseTuning(ConfigurationSection cropSection) {
        FileConfiguration config = plugin.getConfig();

        double growthMaxBonus = Math.max(0.0D, cropSection.getDouble("genetics.growth-max-bonus",
                config.getDouble("genetics.growth-max-bonus", 1.0D)));
        double yieldMaxBonus = Math.max(0.0D, cropSection.getDouble("genetics.yield-max-bonus",
                config.getDouble("genetics.yield-max-bonus", 1.0D)));
        double resistanceMaxReduction = Math.min(1.0D, Math.max(0.0D, cropSection.getDouble("genetics.resistance-max-reduction",
                config.getDouble("genetics.resistance-max-reduction", 0.5D))));
        double diseaseDamagePerLevel = Math.max(0.0D, cropSection.getDouble("diseases.damage-per-level",
                config.getDouble("diseases.damage-per-level", 5.0D)));
        int diseaseMaxLevel = Math.max(1, cropSection.getInt("diseases.max-level",
                config.getInt("diseases.max-level", 5)));

        return new CropTuning(growthMaxBonus, yieldMaxBonus, resistanceMaxReduction, diseaseDamagePerLevel, diseaseMaxLevel);
    }

    // Item-Model der Samen-Items; Default folgt der Pack-Konvention
    // aethos:item/crops/<id>_seeds, per 'seed-item-model' uebersteuerbar. Wird hier
    // EINMAL validiert und als NamespacedKey gecacht - SeedItemManager haengt sonst
    // still ein setItemModel(null) an (Samen saehe kommentarlos vanilla aus).
    private NamespacedKey parseSeedItemModel(String cropId, ConfigurationSection cropSection) {
        String model = cropSection.getString("seed-item-model",
                "aethos:item/crops/" + cropId.toLowerCase(java.util.Locale.ROOT) + "_seeds");
        NamespacedKey key = NamespacedKey.fromString(model);
        if (key == null) {
            plugin.getLogger().warning("Crop '" + cropId + "': 'seed-item-model' ist kein gueltiger "
                    + "namespaced key: " + model + " - Samen behalten das Vanilla-Aussehen.");
        }
        return key;
    }

    // Item-Model der Pflanzen-Displays; das Wachstums-Stadium waehlt der Client
    // per custom_model_data-Float aus (range_dispatch im Resource Pack).
    private String parseItemModel(String cropId, ConfigurationSection cropSection) {
        String itemModel = cropSection.getString("item-model", "aethos:block/crops/" + cropId);
        if (NamespacedKey.fromString(itemModel) == null) {
            plugin.getLogger().warning("Skipping crop '" + cropId + "': 'item-model' is not a valid namespaced key: " + itemModel);
            return null;
        }

        return itemModel;
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
