package de.aethos.crops.Integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Optionale Anbindung an AethosAchievements: meldet Spieler-Statistiken.
 * Ist das Plugin nicht installiert, sind alle Aufrufe No-ops. Die API-Klasse
 * wird nur ueber die innere Bridge beruehrt, die erst bei aktivem Plugin
 * classgeloadet wird (Ladeguard wie GuiDesignerHook).
 */
public final class AchievementsHook {

    private static boolean active;

    private AchievementsHook() {
    }

    public static void init(JavaPlugin plugin) {
        active = plugin.getServer().getPluginManager().getPlugin("AethosAchievements") != null;
        if (active) {
            plugin.getLogger().info("AethosAchievements angebunden - Statistiken werden gemeldet.");
        }
    }

    public static void increment(Player player, String key) {
        add(player, key, 1);
    }

    public static void add(Player player, String key, long amount) {
        if (active) {
            Bridge.add(player, key, amount);
        }
    }

    private static final class Bridge {
        static void add(Player player, String key, long amount) {
            de.aethos.achievements.api.AethosStatsApi.add(player, key, amount);
        }
    }
}
