package de.aethos.crops;

import de.aethos.crops.crop.CropItemFactory;
import de.aethos.crops.crop.CropManager;
import de.aethos.crops.crop.CropType;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CropsCommand extends Command implements PluginIdentifiableCommand {
    private final AethosCrops plugin;

    protected CropsCommand(@NotNull AethosCrops plugin) {
        super("AethosCrops");
        this.plugin = plugin;
        setPermission("AethosCrops.admin");
        setUsage("/<command> give <CropName>");
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 0) {
                Inventory inv = player.getInventory();
                switch (args[0]) {
                    case "giveSeeds" -> {
                        if (args.length < 2) {
                            player.sendMessage("Ungenügend Argumente");
                            return false;
                        }

                        ItemStack item = CropItemFactory.getItem(CropType.valueOf(args[1]));
                        inv.addItem(item);
                    }
                    case "giveAdminSeeds" -> {
                        if (args.length < 2) {
                            player.sendMessage("Ungenügend Argumente");
                            return false;
                        }
                        CropType type = CropType.valueOf(args[1]);
                        inv.addItem(CropItemFactory.getAdminItem(type));
                    }
                    case "debug" -> {
                        for (Location loc : CropManager.getCrops().keySet()) {
                            player.sendMessage(loc.toString());
                        }
                    }
                    case "giveTools" -> {
                        if (args.length < 2) {
                            player.sendMessage("Ungenügend Argumente");
                            return false;
                        }
                        switch (args[1]) {
                            case "lupe" -> inv.addItem(PlayerItems.getHoeItem());
                            case "pestizit" -> inv.addItem(PlayerItems.getPestizit());
                            case "mühle" -> inv.addItem(PlayerItems.getHandMühle());
                            case "hacke" -> inv.addItem(PlayerItems.getUnkrautHacke());
                            case "schere" -> inv.addItem(PlayerItems.getSchere());
                            case "all" -> {
                                inv.addItem(PlayerItems.getHoeItem());
                                inv.addItem(PlayerItems.getHandMühle());
                                inv.addItem(PlayerItems.getPestizit());
                                inv.addItem(PlayerItems.getUnkrautHacke());
                                inv.addItem(PlayerItems.getSchere());
                            }
                        }
                    }
                    case "giveAdminTools" -> {
                        if (args.length < 2) {
                            player.sendMessage("Ungenügend Argumente");
                            return false;
                        }
                        switch (args[1]) {
                            case "healwand" -> inv.addItem(AdminItems.getHealWand());

                            case "käferwand" -> inv.addItem(AdminItems.getKaeferWand());

                            case "unkrautwand" -> inv.addItem(AdminItems.getUnkrautWand());

                            case "pilzwand" -> inv.addItem(AdminItems.getPilzWand());

                            case "all" -> {
                                inv.addItem(AdminItems.getHealWand());
                                inv.addItem(AdminItems.getKaeferWand());
                                inv.addItem(AdminItems.getUnkrautWand());
                                inv.addItem(AdminItems.getPilzWand());
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return complete(args, List.of("giveSeeds", "debug", "giveTools", "giveAdminTools", "giveAdminSeeds"));
        }
        switch (args[0]) {
            case "giveSeeds", "giveAdminSeeds" -> {
                List<String> list = new ArrayList<>(CropType.values().length);
                for (CropType t : CropType.values()) {
                    list.add(t.name());
                }
                return complete(args, list);
            }
            case "giveTools" -> {
                return complete(args, List.of("lupe", "pestizit", "mühle", "hacke", "all", "schere"));
            }
            case "giveAdminTools" -> {
                return complete(args, List.of("healwand", "käferwand", "unkrautwand", "pilzwand", "all"));
            }
        }

        return new ArrayList<>();

    }

    List<String> complete(String @NotNull [] args, List<String> toComplete) {
        if (toComplete == null || toComplete.size() == 0) {
            return null;
        }
        String lastArg = args[args.length - 1];
        List<String> out = new ArrayList<>(List.of());
        for (String completion : toComplete) {
            if (lastArg.matches(" *") || completion.toLowerCase(Locale.ROOT).startsWith(lastArg.toLowerCase(Locale.ROOT))) {
                out.add(completion);
            }
        }
        return out;
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }
}
