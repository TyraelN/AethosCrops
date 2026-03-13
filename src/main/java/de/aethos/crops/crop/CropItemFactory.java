package de.aethos.crops.crop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public final class CropItemFactory {
    private CropItemFactory() {

    }

    public static @NotNull ItemStack getItem(@NotNull CropType type) {
        return createItem(type, 10, NamedTextColor.WHITE);
    }

    private static @NotNull ItemStack createItem(@NotNull CropType type, int startValue, TextColor color) {
        ItemStack newItem = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = newItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CropType.TYPE_KEY, PersistentDataType.STRING, type.getName());
        ArrayList<Component> list = new ArrayList<>();
        String loreValue = ' ' + startValue + "/100";
        for (Gen gen : Gen.values()) {
            container.set(gen.getKey(), PersistentDataType.INTEGER, startValue);
            list.add(Component.text(gen.getDisplayName() + loreValue));
        }
        meta.lore(list);
        meta.displayName(Component.text(type.getName(), color));
        newItem.setItemMeta(meta);
        return newItem;
    }

    public static @NotNull ItemStack getAdminItem(@NotNull CropType type) {
        return createItem(type, 100, NamedTextColor.GOLD);
    }
}

