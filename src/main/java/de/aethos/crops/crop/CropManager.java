package de.aethos.crops.crop;

import de.aethos.crops.AdminItems;
import de.aethos.crops.Helper;
import de.aethos.crops.PlayerItems;
import de.aethos.crops.gui.GUI;
import de.aethos.crops.gui.KäferUI;
import de.aethos.crops.gui.MühlenUI;
import de.aethos.crops.gui.UnkrautUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CropManager implements Listener {
    private static final Map<Location, Crop> CROPS = new HashMap<>(100);

    public static void register(Crop crop) {
        CROPS.putIfAbsent(crop.getLoc(), crop);
    }

    public static Map<Location, Crop> getCrops() {
        return CROPS;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isCrop(loc)) {
            return;
        }
        Crop crop = getCrop(loc);
        event.setDropItems(false);
        if (crop.isKrank()) {
            if (PlayerItems.itemIsSchere(event.getPlayer().getEquipment().getItemInMainHand())) {
                double rand = Math.random();
                if (rand < 0.4) {
                    loc.getWorld().dropItemNaturally(loc, crop.getItem());
                }
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.BROWN_MUSHROOM));
            }
            remove(crop);
            return;
        }
        ItemStack ergebnis = new ItemStack(Material.WHEAT);
        if (loc.getBlock().getBlockData() instanceof Ageable ageable) {
            event.getPlayer().sendMessage(String.valueOf(ageable.getAge()));
            event.getPlayer().sendMessage(String.valueOf(ageable.getMaximumAge()));
            if (ageable.getAge() == ageable.getMaximumAge()) {
                List<String> drops = Helper.zusatzItems("a", "b", crop.getGen(Gen.MENGE));
                System.out.println(drops);
                for (String n : drops) {
                    if ("a".equals(n)) {
                        loc.getWorld().dropItemNaturally(loc, ergebnis);
                    }
                    if ("b".equals(n)) {
                        loc.getWorld().dropItemNaturally(loc, crop.getNewItem());
                    }
                }
            } else if (ageable.getAge() == 0) {
                loc.getWorld().dropItemNaturally(loc, crop.getItem());

            }
        }
        remove(crop);
    }

    public static void remove(Crop crop) {
        CROPS.remove(crop.getLoc());
        crop.setKrankheit(Krankheit.GESUND);
    }

    public static boolean isCrop(Location loc) {
        return CROPS.containsKey(loc);
    }

    public static Crop getCrop(Location loc) {
        return CROPS.get(loc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) {
            return;
        }
        if (item.getItemMeta().getPersistentDataContainer().has(CropType.TYPE_KEY, PersistentDataType.STRING)) {
            Location loc = event.getBlock().getLocation();
            new Crop(loc, event.getItemInHand());
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlantGrow(BlockGrowEvent event) {
        Crop crop = getCrop(event.getBlock().getLocation());
        Location loc = event.getBlock().getLocation();
        if (isCrop(loc)) {
            event.setCancelled(!crop.canGrow());
        } else {
            return;
        }
        if (crop.isKrank()) {
            event.setCancelled(true);
            return;
        }

        List<Crop> nachbarPflanzen = Helper.neighbourCheck(loc);
        AtomicReference<Double> wahrscheinlichkeit = new AtomicReference<>((double) 1);
        System.out.println(nachbarPflanzen);
        nachbarPflanzen.forEach(n -> {
            if (n.hatKrankheit(Krankheit.KAEFER)) {
                wahrscheinlichkeit.updateAndGet(v -> (v - 0.1));
            }
        });
        double rand = Math.random();
        if (rand >= wahrscheinlichkeit.get()) {
            System.out.println("Käfer");
            crop.setKrankheit(Krankheit.KAEFER);
        }
        if (Math.random() * crop.getGen(Gen.KAEFER) * 100 < 150) {
            crop.setKrankheit(Krankheit.KAEFER);
            System.out.println("Käfer");
            event.setCancelled(true);
            //TODO Käferbefall visualisieren ? Andere Pflanze benutzen -> GUI
            return;
        }

        List<Crop> nachbarPflanze = Helper.neighbourCheck(loc);
        AtomicReference<Double> Wahrscheinlichkeit1 = new AtomicReference<>((double) 1);
        System.out.println(nachbarPflanze);
        nachbarPflanze.forEach(n -> {
            if (n.hatKrankheit(Krankheit.UNKRAUT)) {
                Wahrscheinlichkeit1.updateAndGet(v -> (v - 0.1));
            }
        });
        double rand1 = Math.random();
        System.out.println(Wahrscheinlichkeit1);
        if (rand1 >= Wahrscheinlichkeit1.get()) {
            System.out.println("Unkraut");
            crop.setKrankheit(Krankheit.UNKRAUT);
        }
        //TODO benachbarte Pflanzen erhalten beim Wachsen automatisch Käfer -> GUI
        if (Math.random() * crop.getGen(Gen.UNKRAUT) * 100 < 100) {
            crop.setKrankheit(Krankheit.UNKRAUT);
            System.out.println("Unkraut");
            event.setCancelled(true);
            //TODO Unkraut symbolisieren ? Andere Pflanze benutzen -> GUI
        } else if (Math.random() * crop.getGen(Gen.PILZ) * 1000 < 100) {
            crop.setKrankheit(Krankheit.PILZE);
            System.out.println("Pilze");
            event.setCancelled(true);
            //TODO Pilzbefall symbolisieren ? Andere Pflanze -> GUI
            //Pilzbefall breitet sich nicht aus. Einmal betroffen kann die Pflanze nicht mehr gerettet werden
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onHoeClick(PlayerInteractEvent event) {
        if (!PlayerItems.itemIsHoe(event.getPlayer().getEquipment().getItemInMainHand())) {
            return;
        }
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            System.out.println("onHoeClick()");
            event.setCancelled(true);
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Location loc = event.getClickedBlock().getLocation();
        if (isCrop(loc)) {
            Crop crop = getCrop(loc);
            new GUI(crop, event.getPlayer());
        } else {
            event.getPlayer().sendMessage("is not a Crop");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHealCrop(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            System.out.println("a");
            return;
        }
        if (!(PlayerItems.itemIsUnkrautHacke(event.getPlayer().getInventory().getItemInMainHand()) || PlayerItems.itemIsPestizit(event.getPlayer().getInventory().getItemInMainHand()))) {
            System.out.println("b");
            return;
        }
        if (event.getClickedBlock() == null) {
            System.out.println("c");
            return;
        }
        Location location = event.getClickedBlock().getLocation();
        if ((!isCrop(location))) {
            System.out.println("d");
            event.setCancelled(true);
            return;
        }
        System.out.println("e");
        Crop crop = getCrop(location);
        if (crop == null) {
            System.out.println("f");
            return;
        }
        if (PlayerItems.itemIsPestizit(event.getPlayer().getEquipment().getItemInMainHand())) {
            if (!crop.hatKrankheit(Krankheit.KAEFER)) {
                System.out.println("g");
                event.setCancelled(true);
                return;
            }
            System.out.println("h");
            new KäferUI(event.getPlayer(), crop);
            event.getPlayer().getInventory().remove(PlayerItems.getPestizit());
        }
        if (PlayerItems.itemIsUnkrautHacke(event.getPlayer().getEquipment().getItemInMainHand())) {
            System.out.println("i");
            if (!crop.hatKrankheit(Krankheit.UNKRAUT)) {
                System.out.println("j");
                event.setCancelled(true);
                return;
            }
            System.out.println("k");
            new UnkrautUI(event.getPlayer(), crop);
        }
    }

    @EventHandler
    public void onMühle(PlayerInteractEvent event) {
        if (PlayerItems.itemIsHandMühle(event.getPlayer().getEquipment().getItemInMainHand())
                && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            new MühlenUI(event.getPlayer());
        }
    }

    @EventHandler
    public void onAdminItems(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            System.out.println("1");
            return;
        }
        if (event.getClickedBlock() == null) {
            System.out.println("2");
            return;
        }
        ItemStack item = event.getPlayer().getEquipment().getItemInMainHand();
        if (!(AdminItems.itemIsHealWand(item) || AdminItems.itemIsUnkrautWand(item) || AdminItems.itemIsKaeferWand(item) || AdminItems.itemIsPilzWand(item))) {
            System.out.println("3");
            return;
        }
        System.out.println("4");
        Location location = event.getClickedBlock().getLocation();
        if ((!isCrop(location))) {
            System.out.println("5");
            return;
        }
        Crop crop = getCrop(location);
        if (crop == null) {
            System.out.println("6");
            return;
        }
        if (AdminItems.itemIsHealWand(item)) {
            System.out.println("7");
            heal(crop);
        }
        if (AdminItems.itemIsKaeferWand(item)) {
            System.out.println("8");
            crop.setKrankheit(Krankheit.KAEFER);
        }
        if (AdminItems.itemIsPilzWand(item)) {
            System.out.println("9");
            crop.setKrankheit(Krankheit.PILZE);
        }
        if (AdminItems.itemIsUnkrautWand(item)) {
            System.out.println("1ß");
            crop.setKrankheit(Krankheit.UNKRAUT);
        }
    }

    public static void heal(Crop crop) {
        crop.setKrankheit(Krankheit.GESUND);
    }

}
