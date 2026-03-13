package de.aethos.crops;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PlayerItems {
    public static final NamespacedKey HOE_KEY = new NamespacedKey(AethosCrops.getInstance(), "Hoeitem");

    public static final NamespacedKey PESTIZIT_KEY = new NamespacedKey(AethosCrops.getInstance(), "Pestizit");

    public static final NamespacedKey SCHEREN_KEY = new NamespacedKey(AethosCrops.getInstance(), "Schere");

    public static final NamespacedKey HANDMÜHLE_KEY = new NamespacedKey(AethosCrops.getInstance(), "Handmuehle");

    public static final NamespacedKey HACKEN_KEY = new NamespacedKey(AethosCrops.getInstance(), "Hacke");

    public static boolean itemIsHoe(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(HOE_KEY);
    }

    public static boolean itemIsPestizit(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(PESTIZIT_KEY);
    }

    public static boolean itemIsHandMühle(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(HANDMÜHLE_KEY);
    }

    public static boolean itemIsUnkrautHacke(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(HACKEN_KEY);
    }

    public static boolean itemIsSchere(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(SCHEREN_KEY);
    }

    public static @NotNull ItemStack getHoeItem() {
        ItemStack newItem = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_PURPLE + "Lupe"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(PlayerItems.HOE_KEY, PersistentDataType.STRING, "Farmhoe");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static @NotNull ItemStack getPestizit() {
        ItemStack newItem = new ItemStack(Material.BLACK_DYE);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Pestizit"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(PlayerItems.PESTIZIT_KEY, PersistentDataType.STRING, "Antikäfer");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static @NotNull ItemStack getSchere() {
        ItemStack newItem = new ItemStack(Material.SHEARS);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Pilzschere"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(PlayerItems.SCHEREN_KEY, PersistentDataType.STRING, "Antipilz");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static @NotNull ItemStack getHandMühle() {
        ItemStack newItem = new ItemStack(Material.BOWL);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Handmühle"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(PlayerItems.HANDMÜHLE_KEY, PersistentDataType.STRING, "HandMühle");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static @NotNull ItemStack getUnkrautHacke() {
        ItemStack newItem = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Unkrauthacke"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(HACKEN_KEY, PersistentDataType.STRING, "UnrkautHacke");
        newItem.setItemMeta(meta);
        return newItem;
    }

}

