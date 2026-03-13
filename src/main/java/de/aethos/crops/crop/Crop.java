package de.aethos.crops.crop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class Crop implements Listener {
    private final Location loc;

    private final CropType type;

    private final Material material;

    private Map<Gen, Integer> genMap = new EnumMap<>(Gen.class);

    private long zeitpunkt;

    private Krankheit krankheit = Krankheit.GESUND;

    public Crop(Location loc, ItemStack item) {
        this.loc = loc;
        this.material = item.getType();
        this.zeitpunkt = System.currentTimeMillis();
        this.type = CropType.valueOf(item.getItemMeta().getPersistentDataContainer().get(CropType.TYPE_KEY, PersistentDataType.STRING));
        setGenetik(item);
        CropManager.register(this);
    }

    private void setGenetik(ItemStack itemStack) {
        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
        for (Gen gen : Gen.values()) {
            genMap.put(gen, container.get(gen.getKey(), PersistentDataType.INTEGER));
        }
    }

    public boolean isTooOld() {
        long levensdauer = genMap.get(Gen.AUSDAUER) * 86400000;
        return getZeitpunkt() + levensdauer <= System.currentTimeMillis();
    }

    public long getZeitpunkt() {
        return zeitpunkt;
    }

    protected void setZeitpunkt(long zeitpunkt) {
        this.zeitpunkt = zeitpunkt;
    }

    public @NotNull ItemStack getNewItem() {
        ItemStack newItem = new ItemStack(material);
        ItemMeta meta = newItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CropType.TYPE_KEY, PersistentDataType.STRING, type.name());
        ArrayList<String> list = new ArrayList<>();
        for (Gen gen : Gen.values()) {
            container.set(gen.getKey(), PersistentDataType.INTEGER, evolution(genMap.get(gen)));
            list.add(gen.getDisplayName() + " " + genMap.get(gen) + "/100");
        }
        meta.displayName(Component.text(this.getTypeName(), rarety(this)));
        meta.setLore(list);
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static NamedTextColor rarety(Crop crop) {
        NamedTextColor color = NamedTextColor.WHITE;
        int level = 0;
        for (Gen gen : Gen.values()) {
            level += crop.getGen(gen);
        }
        //common: WHITE 0 - 200
        //uncommon: GREEN 201 - 300
        //rare: BLUE 301 - 400
        //Mythisch: DARK_PURPLE 401 - 500
        //legendary: GOLD 501 - 600
        int Umbruch = 201;
        int Stufe = 0;
        while (level >= Umbruch) {
            Stufe += 1;
            Umbruch += 100;
        }
        if (Stufe == 1) {
            color = NamedTextColor.GREEN;
        }
        if (Stufe == 2) {
            color = NamedTextColor.BLUE;
        }
        if (Stufe == 3) {
            color = NamedTextColor.DARK_PURPLE;
        }
        if (Stufe == 4) {
            color = NamedTextColor.GOLD;
        }
        return color;
    }

    public int getGen(Gen gen) {
        return genMap.get(gen);
    }

    public String getTypeName() {
        return type.name();
    }

    private static int evolution(int i) {
        if (i <= 10) {
            return i + 1;
        }
        if (Math.random() >= 0.5) {
            if (i >= 100) {
                return i;
            }
            return i + 1;
        }
        return i - 1;

    }

    public @NotNull ItemStack getItem() {
        ItemStack newItem = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = newItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CropType.TYPE_KEY, PersistentDataType.STRING, type.name());
        ArrayList<String> list = new ArrayList<>();
        for (Gen gen : Gen.values()) {
            container.set(gen.getKey(), PersistentDataType.INTEGER, genMap.get(gen));
            list.add(gen.getDisplayName() + " " + genMap.get(gen) + "/100");
        }
        meta.displayName(Component.text(this.getTypeName(), rarety(this)));
        meta.setLore(list);
        newItem.setItemMeta(meta);
        return newItem;
    }

    public boolean isValid() {
        return CropManager.isCrop(getLoc());
    }

    public Location getLoc() {
        return loc;
    }

    public boolean canGrow() {
        int tageslicht = getLoc().getBlock().getLightFromSky();
        if (!(tageslicht == 15)) {
            return false;
        }
        if (!getLoc().getWorld().isClearWeather()) {
            return false;
        }
        return getLoc().getWorld().isDayTime();
    }

    public boolean isKrank() {
        return !krankheit.equals(Krankheit.GESUND);
    }

    public boolean hatKrankheit(Krankheit krankheit) {
        return this.krankheit.equals(krankheit);
    }

    public CropType getType() {
        return type;
    }

    public Krankheit getKrankheit() {
        return krankheit;
    }

    public void setKrankheit(Krankheit krankheit) {
        this.krankheit = krankheit;
    }

    protected void setGenMap(Map<Gen, Integer> map) {
        this.genMap = map;
    }
}

