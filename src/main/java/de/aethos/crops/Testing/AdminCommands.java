package de.aethos.crops.Testing;

import de.aethos.crops.AethosCrops;
import de.aethos.crops.Managers.SeedItemManager;
import de.aethos.crops.Utils.Crop;
import de.aethos.crops.Utils.Gen.IGen;
import de.aethos.crops.Utils.SeedGenes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AdminCommands {

    public void execute(CommandSender sender, String[] args) {
        // reload darf auch von der Konsole (Aethos-Panel) kommen.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aethoscrops.admin")) {
                sender.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
                return;
            }
            handleReload(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("analyze")) {
            if (!player.hasPermission("aethoscrops.analyze")) {
                player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
                return;
            }
            AethosCrops.getSeedAnalysisListener().open(player);
            return;
        }

        if (!player.hasPermission("aethoscrops.admin")) {
            player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
            return;
        }

        switch (subCommand) {
            case "chunkinfo" -> handleChunkInfo(player);
            case "blockinfo" -> handleBlockInfo(player);
            case "giveseed" -> handleGiveSeed(player, args);
            case "givediseasetool" -> handleGiveDiseaseTool(player, args);
            default -> sendUsage(player);
        }
    }

    private void handleChunkInfo(Player player) {
        Map<Block, Crop> crops = AethosCrops.getDataManager().loadAllCrops(player.getLocation().getChunk());

        if (crops.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No custom crops were found in this chunk.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Crops in current chunk: " + ChatColor.AQUA + crops.size());
        crops.entrySet().stream()
                .sorted((left, right) -> {
                    Location a = left.getKey().getLocation();
                    Location b = right.getKey().getLocation();
                    int yComp = Integer.compare(a.getBlockY(), b.getBlockY());
                    if (yComp != 0) return yComp;
                    int xComp = Integer.compare(a.getBlockX(), b.getBlockX());
                    if (xComp != 0) return xComp;
                    return Integer.compare(a.getBlockZ(), b.getBlockZ());
                })
                .forEach(entry -> {
                    Block block = entry.getKey();
                    Crop crop = entry.getValue();
                    String location = block.getX() + "," + block.getY() + "," + block.getZ();

                    player.sendMessage(
                            ChatColor.GRAY + "- " + ChatColor.WHITE + crop.getId() +
                                    ChatColor.DARK_GRAY + " (" + crop.getStage() + "/" + crop.getMaxStage() + ") " +
                                    ChatColor.GRAY + "at " + ChatColor.AQUA + location
                    );
                });
    }

    private void handleBlockInfo(Player player) {
        Block target = player.getTargetBlockExact(8);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target block in range.");
            return;
        }

        Location loc = target.getLocation();
        player.sendMessage(ChatColor.GOLD + "Target block: " + ChatColor.WHITE + target.getType() + ChatColor.GRAY + " at " +
                ChatColor.AQUA + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        Crop crop = AethosCrops.getDataManager().loadCrop(target);
        if (crop == null) {
            player.sendMessage(ChatColor.YELLOW + "No custom crop data stored on this block.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Custom crop info:");
        player.sendMessage(ChatColor.GRAY + "- id: " + ChatColor.WHITE + crop.getId());
        player.sendMessage(ChatColor.GRAY + "- display-name: " + ChatColor.WHITE + crop.getDisplayName());
        player.sendMessage(ChatColor.GRAY + "- stage: " + ChatColor.WHITE + crop.getStage() + ChatColor.GRAY + "/" + ChatColor.WHITE + crop.getMaxStage());
        player.sendMessage(ChatColor.GRAY + "- health: " + ChatColor.WHITE + String.format("%.2f%%", crop.getHealth()));
        SeedGenes genes = crop.getGenes();
        player.sendMessage(ChatColor.GRAY + "- genes: " + ChatColor.WHITE
                + "growth=" + genes.getGrowth() + ", yield=" + genes.getYield() + ", resistance=" + genes.getResistance()
                + ChatColor.GRAY + " → " + SeedItemManager.formatStars(genes.getStars()));
        player.sendMessage(ChatColor.GRAY + "- item-model: " + ChatColor.WHITE + crop.getItemModel()
                + ChatColor.GRAY + " (model-stage " + ChatColor.WHITE + crop.scaleStageTo(7) + ChatColor.GRAY + ")");
        player.sendMessage(ChatColor.GRAY + "- diseases: " + formatDiseaseInfo(crop));
    }

    private String formatDiseaseInfo(Crop crop) {
        Map<IGen, Double> diseaseChances = crop.getDiseaseChances();
        if (diseaseChances.isEmpty()) {
            return ChatColor.YELLOW + "none configured";
        }

        StringJoiner configuredDiseases = new StringJoiner(ChatColor.GRAY + ", ");
        crop.getDiseaseChances().forEach((disease, chance) -> configuredDiseases.add(
                ChatColor.WHITE + disease.getDisplayName() +
                        ChatColor.DARK_GRAY + " [id=" + disease.getId() + ", chance=" + Math.round(chance * 100.0D) + "%]"
        ));

        if (crop.getDiseases().isEmpty()) {
            return ChatColor.GRAY + "configured: " + configuredDiseases + ChatColor.GRAY + " | active: " + ChatColor.YELLOW + "none";
        }

        String activeDiseases = crop.getDiseases().stream()
                .map(disease -> ChatColor.WHITE + disease.getDisplayName() + ChatColor.DARK_GRAY + " [id=" + disease.getId() + ", level=" + crop.getDiseaseLevel(disease) + "]")
                .collect(Collectors.joining(ChatColor.GRAY + ", "));

        return ChatColor.GRAY + "configured: " + configuredDiseases + ChatColor.GRAY + " | active: " + activeDiseases;
    }

    private void handleGiveDiseaseTool(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /aethoscrops givediseasetool <disease-id>");
            return;
        }

        String diseaseId = args[1].toLowerCase();
        IGen disease = AethosCrops.getGenRegistry().findById(diseaseId);
        if (disease == null) {
            player.sendMessage(ChatColor.RED + "Unknown disease id: " + diseaseId);
            return;
        }

        ItemStack tool = AethosCrops.getConfigManager().createDiseaseToolItem(diseaseId);
        if (tool == null) {
            player.sendMessage(ChatColor.RED + "No configured treatment item for disease id: " + diseaseId);
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(tool);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }

        player.sendMessage(ChatColor.GREEN + "Given disease treatment tool for: " + ChatColor.WHITE + disease.getDisplayName());
    }

    private void handleGiveSeed(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /aethoscrops giveseed <ID> [Anzahl]");
            return;
        }

        String cropId = args[1];
        Crop crop = AethosCrops.getCropRegistry().get(cropId);

        if (crop == null) {
            player.sendMessage(ChatColor.RED + "Unknown crop id: " + cropId);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                return;
            }
        }

        // Zufallsgene wuerfeln und nach Guete gruppieren - ein Stack je Sterne-Stufe.
        Map<Integer, List<SeedGenes>> byStars = new TreeMap<>();
        for (int i = 0; i < amount; i++) {
            SeedGenes genes = SeedGenes.random();
            byStars.computeIfAbsent(genes.getStars(), stars -> new ArrayList<>()).add(genes);
        }

        SeedItemManager seedItemManager = AethosCrops.getSeedItemManager();
        boolean dropped = false;
        for (List<SeedGenes> group : byStars.values()) {
            ItemStack seed = seedItemManager.createSeedStack(crop, group);
            if (seed == null) {
                continue;
            }

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(seed);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
                dropped = true;
            }
        }

        if (dropped) {
            player.sendMessage(ChatColor.YELLOW + "Inventory full, dropped seeds on the ground.");
        }

        String tiers = byStars.entrySet().stream()
                .map(entry -> entry.getValue().size() + "x " + entry.getKey() + "★")
                .collect(Collectors.joining(", "));
        player.sendMessage(ChatColor.GREEN + "Given " + amount + " seed(s) for crop " + ChatColor.WHITE + crop.getId()
                + ChatColor.GREEN + " (" + tiers + ")");
    }

    private void handleReload(CommandSender sender) {
        AethosCrops plugin = AethosCrops.getInstance();
        AethosCrops.getConfigManager().loadCrops(AethosCrops.getCropRegistry());
        plugin.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "AethosCrops config reloaded.");
    }

    private void sendUsage(Player player) {
        List<String> allowed = allowedSubCommands(player);
        if (allowed.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Dazu hast du keine Berechtigung.");
            return;
        }
        player.sendMessage(ChatColor.RED + "Usage: /aethoscrops <" + String.join("|", allowed) + ">");
    }

    // Subcommands, die der Sender laut Permission nutzen darf.
    private List<String> allowedSubCommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        if (sender.hasPermission("aethoscrops.analyze")) {
            allowed.add("analyze");
        }
        if (sender.hasPermission("aethoscrops.admin")) {
            allowed.addAll(List.of("chunkinfo", "blockinfo", "giveseed", "givediseasetool", "reload"));
        }
        return allowed;
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return allowedSubCommands(sender).stream()
                    .filter(entry -> entry.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("giveseed") && sender.hasPermission("aethoscrops.admin")) {
            return AethosCrops.getCropRegistry().getAll().stream()
                    .map(Crop::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("giveseed") && sender.hasPermission("aethoscrops.admin")) {
            return List.of("1", "8", "16", "32", "64").stream()
                    .filter(amount -> amount.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("givediseasetool") && sender.hasPermission("aethoscrops.admin")) {
            return AethosCrops.getGenRegistry().getAll().stream()
                    .map(IGen::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
