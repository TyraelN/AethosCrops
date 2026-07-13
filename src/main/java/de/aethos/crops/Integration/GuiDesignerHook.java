package de.aethos.crops.Integration;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Events.SeedAnalysisMenu;
import de.aethos.guidesigner.api.AethosGuiActions;

/**
 * Optionale Anbindung an AethosGuiDesigner: registriert das Samen-Analyse-
 * Menue als Custom-Funktion "aethoscrops:analyze", damit Admins es im
 * Web-Editor an GUI-Buttons haengen koennen.
 * <p>
 * Eigene Klasse als Ladeguard: Sie wird NUR betreten, wenn AethosGuiDesigner
 * geladen ist (Check im Aufrufer) - sonst wuerde die API-Klasse fehlen
 * (optionale Dependency mit join-classpath).
 */
public final class GuiDesignerHook {

    public static final String ACTION_ANALYZE = "aethoscrops:analyze";

    private GuiDesignerHook() {
    }

    public static void register(AethosCrops plugin) {
        AethosGuiActions.register(ACTION_ANALYZE, player -> {
            if (player.hasPermission("aethoscrops.analyze")) {
                new SeedAnalysisMenu(plugin).open(player);
            } else {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Dazu hast du keine Berechtigung.",
                        net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        });
        plugin.getLogger().info("GuiDesigner-Funktion registriert: " + ACTION_ANALYZE);
    }

    public static void unregister() {
        AethosGuiActions.unregister(ACTION_ANALYZE);
    }
}
