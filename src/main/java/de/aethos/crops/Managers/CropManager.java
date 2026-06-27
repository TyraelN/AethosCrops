package de.aethos.crops.Managers;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.Gen.IGen;
import de.aethos.crops.Utils.DropEntry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
            boolean shouldRemainActive = ThreadLocalRandom.current().nextDouble() <= chance;

            if (shouldRemainActive) {
                if (wasActive) {
                    crop.reduceHealthPercent(previousLevel * 5.0D);
                    crop.setDiseaseLevel(disease, Math.min(previousLevel + 1, 5));
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
        if (block == null || crop == null || crop.getDropTable() == null) {
            return;
        }

        double healthMultiplier = crop.getHealth() / 100.0D;

        for (DropEntry entry : crop.getDropTable().getEntries()) {
            if (entry == null || entry.getItem() == null || entry.getChance() <= 0) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() <= entry.getChance()) {
                ItemStack scaledDrop = entry.getItem().clone();
                int scaledAmount = (int) Math.floor(scaledDrop.getAmount() * healthMultiplier);
                if (scaledAmount <= 0) {
                    continue;
                }

                scaledDrop.setAmount(scaledAmount);
                block.getWorld().dropItem(block.getLocation(), scaledDrop);
            }
        }
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

        return new Crop(
                template.getId(),
                template.getDisplayName(),
                template.getStageModelPaths(),
                template.getGrowSpeed(),
                template.getDropTable(),
                template.getDiseaseChances(),
                template.getMaxStage()
        );
    }

    // Turns a crop object into its respective seed item
    public ItemStack toSeed(Crop crop) {
        if (crop == null) {
            return new ItemStack(Material.WHEAT_SEEDS);
        }

        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = seed.getItemMeta();
        if (meta == null) {
            return seed;
        }

        meta.setDisplayName(ChatColor.GREEN + crop.getDisplayName() + ChatColor.YELLOW + " Seed");
        meta.getPersistentDataContainer().set(CropKey.CROP_TYPE, PersistentDataType.STRING, crop.getId());
        seed.setItemMeta(meta);

        return seed;

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

    // Changes the original wheat crop growth stage to its first stage on a pending block state
    public void setOriginalFirstStage(BlockState blockState) {
        if (blockState == null) {
            return;
        }

        applyAge(blockState, 0);
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

    // Changes the original wheat crop growth stage to its last stage on a pending block state
    public void setOriginalLastStage(BlockState blockState) {
        if (blockState == null) {
            return;
        }

        Ageable ageable = getAgeable(blockState.getBlockData());
        if (ageable == null) {
            return;
        }

        applyAge(blockState, ageable.getMaximumAge());
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
