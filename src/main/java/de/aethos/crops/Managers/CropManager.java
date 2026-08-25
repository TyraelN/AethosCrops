package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.Gen.IGen;
import de.aethos.crops.Utils.DropEntry;
import de.aethos.crops.Utils.SeedGenes;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CropManager {

    // Rolls all diseases of a crop with its provided chance
    public Crop updateGen(Crop crop) {
        if (crop == null) {
            return null;
        }

        IGen activeDisease = crop.getDiseases().isEmpty() ? null : crop.getDiseases().get(0);

        for (Map.Entry<IGen, Double> entry : crop.getDiseaseChances().entrySet()) {
            IGen disease = entry.getKey();
            Double chance = entry.getValue();

            if (disease == null || chance == null || chance <= 0) {
                continue;
            }

            if (activeDisease != null && !disease.equals(activeDisease)) {
                continue;
            }

            boolean wasActive = crop.getDiseases().contains(disease);
            int previousLevel = Math.max(1, crop.getDiseaseLevel(disease));
            // Resistenz-Gen senkt die Krankheits-Wahrscheinlichkeit (max. Wirkung aus dem Crop-Tuning).
            double effectiveChance = Math.min(1.0D, chance * crop.getGenes().diseaseChanceMultiplier(crop.getTuning().resistanceMaxReduction()));
            boolean shouldRemainActive = ThreadLocalRandom.current().nextDouble() <= effectiveChance;

            if (shouldRemainActive) {
                if (wasActive) {
                    crop.reduceHealthPercent(previousLevel * crop.getTuning().diseaseDamagePerLevel());
                    crop.setDiseaseLevel(disease, Math.min(previousLevel + 1, crop.getTuning().diseaseMaxLevel()));
                } else {
                    crop.setDiseaseLevel(disease, 1);
                    activeDisease = disease;
                }
            } else {
                crop.removeDisease(disease);
                if (disease.equals(activeDisease)) {
                    activeDisease = null;
                }
            }
        }

        return crop;
    }

    // Increments a crop stage and ensures it does not pass its max stage
    public Crop incrementStage(Crop crop) {
        if (crop == null || crop.isAtMaxStage()) {
            return crop;
        }

        crop.incrementStage();
        return crop;
    }

    // Selects and drops all valid harvest items from a crop drop table
    public void dropHarvestDrops(Block block, Crop crop) {
        if (block == null || crop == null) {
            return;
        }

        // Gesundheit und Ertrags-Gen skalieren die Drop-Menge gemeinsam.
        double amountMultiplier = (crop.getHealth() / 100.0D) * crop.getGenes().yieldMultiplier(crop.getTuning().yieldMaxBonus());
        int mutationSpread = AethosCrops.getConfigManager().getMutationSpread();
        int seedsDropped = 0;

        List<DropEntry> entries = crop.getDropTable() != null ? crop.getDropTable().getEntries() : List.of();
        for (DropEntry entry : entries) {
            if (entry == null || entry.getItem() == null || entry.getChance() <= 0) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() > entry.getChance()) {
                continue;
            }

            int scaledAmount = probabilisticRound(entry.getItem().getAmount() * amountMultiplier);
            if (scaledAmount <= 0) {
                continue;
            }

            if (entry.getItem().getType() == Material.WHEAT_SEEDS) {
                // Samen-Drops erben die Gene der Elternpflanze (leicht mutiert);
                // jeder Samen einzeln, damit jede Mutation ihren eigenen Stack bildet.
                for (int i = 0; i < scaledAmount; i++) {
                    dropInheritedSeed(block, crop, mutationSpread);
                    seedsDropped++;
                }
                continue;
            }

            ItemStack scaledDrop = entry.getItem().clone();
            scaledDrop.setAmount(scaledAmount);
            block.getWorld().dropItem(block.getLocation(), scaledDrop);
        }

        // Eine Ernte wirft immer mindestens einen Samen ab, damit die Linie
        // auch bei verpatzten Wuerfen oder kranker Pflanze nie ausstirbt
        // (abschaltbar ueber harvest.guaranteed-seed).
        if (seedsDropped == 0 && AethosCrops.getConfigManager().isGuaranteedSeedDrop()) {
            dropInheritedSeed(block, crop, mutationSpread);
        }
    }

    private void dropInheritedSeed(Block block, Crop crop, int mutationSpread) {
        SeedGenes inherited = crop.getGenes().mutate(mutationSpread);
        ItemStack seed = AethosCrops.getSeedItemManager().createSeedStack(crop, List.of(inherited));
        if (seed != null) {
            block.getWorld().dropItem(block.getLocation(), seed);
        }
    }

    // Rundet den Nachkommaanteil als Wahrscheinlichkeit (2.3 -> 30% Chance auf 3,
    // sonst 2), damit kleine Mengen nicht systematisch auf 0 abgeschnitten werden.
    private int probabilisticRound(double value) {
        int base = (int) Math.floor(value);
        double fraction = value - base;
        if (fraction > 0 && ThreadLocalRandom.current().nextDouble() < fraction) {
            base++;
        }
        return base;
    }

    // Drops seed when not fully grown, otherwise drops harvest items
    public void dropBrokenCropLoot(Block block, Crop crop) {
        if (block == null || crop == null) {
            return;
        }

        if (crop.isAtMaxStage()) {
            dropHarvestDrops(block, crop);
            return;
        }

        block.getWorld().dropItem(block.getLocation(), toSeed(crop));
    }

    // Checks whether or not the provided Item is a valid seed for a custom crop
    public boolean isValidSeed(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        String cropId = meta.getPersistentDataContainer().get(CropKey.CROP_TYPE, PersistentDataType.STRING);
        return cropId != null && !cropId.isBlank() && AethosCrops.getCropRegistry().contains(cropId);
    }

    // Turns a custom seed into its respective Crop object
    public Crop toCrop(ItemStack itemStack) {
        if (!isValidSeed(itemStack)) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        String cropId = meta.getPersistentDataContainer().get(CropKey.CROP_TYPE, PersistentDataType.STRING);
        if (cropId == null) {
            return null;
        }

        Crop template = AethosCrops.getCropRegistry().get(cropId);
        if (template == null) {
            return null;
        }

        Crop crop = new Crop(
                template.getId(),
                template.getDisplayName(),
                template.getItemModel(),
                template.getSeedItemModel(),
                template.getGrowSpeed(),
                template.getDropTable(),
                template.getDiseaseChances(),
                template.getMaxStage(),
                template.getTuning()
        );

        // Verbraucht wird immer der letzte Samen des Stacks - seine Gene
        // bestimmen die gepflanzte Pflanze (siehe SeedItemManager-Invarianten).
        List<SeedGenes> genes = AethosCrops.getSeedItemManager().getGenes(itemStack);
        if (!genes.isEmpty()) {
            crop.setGenes(genes.get(genes.size() - 1));
        }

        return crop;
    }

    // Turns a crop object into its respective seed item (keeps the crop's genes)
    public ItemStack toSeed(Crop crop) {
        if (crop == null) {
            return new ItemStack(Material.WHEAT_SEEDS);
        }

        ItemStack seed = AethosCrops.getSeedItemManager().createSeedStack(crop, List.of(crop.getGenes()));
        return seed != null ? seed : new ItemStack(Material.WHEAT_SEEDS);
    }

    public boolean isDiseaseTool(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        String diseaseId = meta.getPersistentDataContainer().get(CropKey.DISEASE_TOOL, PersistentDataType.STRING);
        return diseaseId != null && !diseaseId.isBlank();
    }

    public String getDiseaseToolId(ItemStack itemStack) {
        if (!isDiseaseTool(itemStack)) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(CropKey.DISEASE_TOOL, PersistentDataType.STRING);
    }

    // Changes the original wheat crop growth stage to its first stage so it continues to grow
    public void setOriginalFirstStage(Block block) {
        if (block == null) {
            return;
        }

        applyAge(block, 0);
    }

    // Changes the original wheat crop growth stage to its last stage so it no longer grows
    public void setOriginalLastStage(Block block) {
        if (block == null) {
            return;
        }

        Ageable ageable = getAgeable(block.getBlockData());
        if (ageable == null) {
            return;
        }

        applyAge(block, ageable.getMaximumAge());
    }

    // Synchronisiert die Vanilla-Wachstumsstufe (liefert die Hitbox) proportional
    // zur Aethos-Stufe: Stufe 1 -> Age 0, Max-Stufe -> Age 7, dazwischen linear.
    public void syncOriginalStage(BlockState blockState, Crop crop) {
        if (blockState == null || crop == null) {
            return;
        }

        Ageable ageable = getAgeable(blockState.getBlockData());
        if (ageable == null) {
            return;
        }

        applyAge(blockState, scaledVanillaAge(crop, ageable.getMaximumAge()));
    }

    private int scaledVanillaAge(Crop crop, int maxAge) {
        // Vor der Max-Stufe erreicht scaleStageTo nie das Vanilla-Maximum, sonst
        // wuerden keine BlockGrowEvents mehr feuern und die Pflanze bliebe stehen.
        return crop.scaleStageTo(maxAge);
    }

    private void applyAge(Block block, int age) {
        Ageable ageable = getAgeable(block.getBlockData());
        if (ageable == null) {
            return;
        }

        ageable.setAge(age);
        block.setBlockData(ageable);
    }

    private void applyAge(BlockState blockState, int age) {
        Ageable ageable = getAgeable(blockState.getBlockData());
        if (ageable == null) {
            return;
        }

        ageable.setAge(age);
        blockState.setBlockData(ageable);
    }

    private Ageable getAgeable(org.bukkit.block.data.BlockData blockData) {
        if (!(blockData instanceof Ageable ageable)) {
            return null;
        }

        return ageable;
    }
}
