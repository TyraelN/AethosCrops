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

public class AdminItems {
    public static final NamespacedKey HEAL_WAND = new NamespacedKey(AethosCrops.getInstance(), "Healer");
    public static final NamespacedKey KAEFER_WAND = new NamespacedKey(AethosCrops.getInstance(), "Kaefer");
    public static final NamespacedKey PILZ_WAND = new NamespacedKey(AethosCrops.getInstance(), "Pilze");
    public static final NamespacedKey UNKRAUT_WAND = new NamespacedKey(AethosCrops.getInstance(), "Unkraut");

    public static boolean itemIsHealWand(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(HEAL_WAND);
    }

    public static @NotNull ItemStack getHealWand() {
        ItemStack newItem = new ItemStack(Material.STICK);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Heal Wand"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(AdminItems.HEAL_WAND, PersistentDataType.STRING, "Healer");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static boolean itemIsKaeferWand(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(KAEFER_WAND);
    }

    public static @NotNull ItemStack getKaeferWand() {
        ItemStack newItem = new ItemStack(Material.STICK);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Käfer Wand"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(AdminItems.KAEFER_WAND, PersistentDataType.STRING, "Kaefer");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static boolean itemIsPilzWand(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(PILZ_WAND);
    }

    public static @NotNull ItemStack getPilzWand() {
        ItemStack newItem = new ItemStack(Material.STICK);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Pilz Wand"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(AdminItems.PILZ_WAND, PersistentDataType.STRING, "Pilze");
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static boolean itemIsUnkrautWand(@NotNull ItemStack item) {
        if (item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(UNKRAUT_WAND);
    }

    public static @NotNull ItemStack getUnkrautWand() {
        ItemStack newItem = new ItemStack(Material.STICK);
        ItemMeta meta = newItem.getItemMeta();
        meta.displayName(Component.text(ChatColor.DARK_GREEN + "Unkraut Wand"));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(AdminItems.UNKRAUT_WAND, PersistentDataType.STRING, "Unkraut");
        newItem.setItemMeta(meta);
        return newItem;
    }
}
