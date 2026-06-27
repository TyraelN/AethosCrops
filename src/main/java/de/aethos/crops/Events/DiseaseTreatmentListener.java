package de.aethos.crops.Events;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.ConfigManager;
import de.aethos.crops.Managers.CropManager;
import de.aethos.crops.Managers.DataManager;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.CropKey;
import de.aethos.crops.Utils.Gen.IGen;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DiseaseTreatmentListener implements Listener {

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "Disease Treatment";
    private static final List<Material> LEAF_MATERIALS = Arrays.asList(
            Material.OAK_LEAVES,
            Material.BIRCH_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES,
            Material.PALE_OAK_LEAVES
    );

    private final Map<UUID, TreatmentSession> sessions = new HashMap<>();

    @EventHandler
    public void onCropInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block.getType() != Material.WHEAT) {
            return;
        }

        CropManager cropManager = AethosCrops.getCropManager();
        ItemStack item = event.getItem();
        if (!cropManager.isDiseaseTool(item)) {
            return;
        }

        DataManager dataManager = AethosCrops.getDataManager();
        if (!dataManager.hasCrop(block)) {
            return;
        }

        Crop crop = dataManager.loadCrop(block);
        if (crop == null) {
            dataManager.removeCrop(block);
            AethosCrops.getDisplayManager().removeDisplay(block);
            return;
        }

        String diseaseId = cropManager.getDiseaseToolId(item);
        IGen disease = AethosCrops.getGenRegistry().findById(diseaseId);
        if (disease == null) {
            return;
        }

        int diseaseLevel = crop.getDiseaseLevel(disease);
        if (diseaseLevel <= 0) {
            return;
        }

        event.setCancelled(true);
        openTreatmentGui(event.getPlayer(), block.getLocation(), diseaseId, diseaseLevel);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        TreatmentSession session = sessions.get(clicker.getUniqueId());
        if (session == null) {
            return;
        }

        if (!event.getView().getTopInventory().equals(session.inventory)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(session.inventory)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType().isAir()) {
            return;
        }

        ItemMeta meta = currentItem.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(CropKey.DISEASE_GUI_ITEM)) {
            event.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            session.remainingDiseaseItems--;

            if (session.remainingDiseaseItems <= 0) {
                healCrop(session);
                clicker.closeInventory();
            }
            return;
        }

        if (isLeaf(currentItem.getType())) {
            event.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            punishWrongSelection(session);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void openTreatmentGui(Player player, Location cropLocation, String diseaseId, int diseaseLevel) {
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        int diseaseItems = calculateDiseaseItemCount(diseaseLevel);
        int leafItems = GUI_SIZE - diseaseItems;

        List<ItemStack> content = new ArrayList<>(GUI_SIZE);
        ConfigManager configManager = AethosCrops.getConfigManager();
        ItemStack diseaseItem = configManager.createDiseaseGuiItem(diseaseId);
        if (diseaseItem == null) {
            return;
        }

        for (int i = 0; i < diseaseItems; i++) {
            content.add(diseaseItem.clone());
        }

        for (int i = 0; i < leafItems; i++) {
            content.add(new ItemStack(randomLeafMaterial()));
        }

        shuffle(content);
        for (int i = 0; i < content.size(); i++) {
            inventory.setItem(i, content.get(i));
        }

        sessions.put(player.getUniqueId(), new TreatmentSession(cropLocation, diseaseId, inventory, diseaseItems));
        player.openInventory(inventory);
    }

    private void healCrop(TreatmentSession session) {
        Block block = session.location.getBlock();
        if (block.getType() != Material.WHEAT) {
            return;
        }

        DataManager dataManager = AethosCrops.getDataManager();
        Crop crop = dataManager.loadCrop(block);
        if (crop == null) {
            dataManager.removeCrop(block);
            AethosCrops.getDisplayManager().removeDisplay(block);
            return;
        }

        IGen disease = AethosCrops.getGenRegistry().findById(session.diseaseId);
        if (disease == null) {
            return;
        }

        crop.removeDisease(disease);
        dataManager.updateCrop(block, crop);
        AethosCrops.getDisplayManager().spawnOrUpdateDisplay(block, crop);
    }

    private void punishWrongSelection(TreatmentSession session) {
        Block block = session.location.getBlock();
        if (block.getType() != Material.WHEAT) {
            return;
        }

        DataManager dataManager = AethosCrops.getDataManager();
        Crop crop = dataManager.loadCrop(block);
        if (crop == null) {
            dataManager.removeCrop(block);
            AethosCrops.getDisplayManager().removeDisplay(block);
            return;
        }

        crop.reduceHealthPercent(AethosCrops.getConfigManager().getWrongLeafHealthPenalty());
        dataManager.updateCrop(block, crop);
        AethosCrops.getDisplayManager().spawnOrUpdateDisplay(block, crop);
    }

    private int calculateDiseaseItemCount(int diseaseLevel) {
        int clampedLevel = Math.max(1, Math.min(5, diseaseLevel));
        double ratio = clampedLevel * 0.1D;
        return Math.max(1, (int) Math.round(GUI_SIZE * ratio));
    }

    private boolean isLeaf(Material material) {
        return LEAF_MATERIALS.contains(material);
    }

    private Material randomLeafMaterial() {
        return LEAF_MATERIALS.get(ThreadLocalRandom.current().nextInt(LEAF_MATERIALS.size()));
    }

    private void shuffle(List<ItemStack> content) {
        for (int i = content.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            ItemStack temp = content.get(i);
            content.set(i, content.get(j));
            content.set(j, temp);
        }
    }

    private static class TreatmentSession {
        private final Location location;
        private final String diseaseId;
        private final Inventory inventory;
        private int remainingDiseaseItems;

        private TreatmentSession(Location location, String diseaseId, Inventory inventory, int remainingDiseaseItems) {
            this.location = location;
            this.diseaseId = diseaseId;
            this.inventory = inventory;
            this.remainingDiseaseItems = remainingDiseaseItems;
        }
    }
}
