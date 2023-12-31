package me.gibson.landclaim.main.landclaimmarket.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import me.ryanhamshire.GriefPrevention.Claim;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class AsyncClaimLogger {
    private final JavaPlugin plugin;

    public AsyncClaimLogger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void logClaimAsync(Claim claim) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> customItems = new ArrayList<>();

            getAllBlocksInClaim(claim, blocks -> {

                for (Block block : blocks) {
                    if (block.getState() instanceof InventoryHolder) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            InventoryHolder holder = (InventoryHolder) block.getState();
                            for (ItemStack item : holder.getInventory().getContents()) {
                                if (item != null && item.hasItemMeta()) {
                                    if (item.getItemMeta().hasCustomModelData()) {
                                        plugin.getLogger().info("Added item with custom model data.");

                                        customItems.add(item);

                                        saveItem(customItems, "Customs/" + claim.getID() + ".yml", block.getLocation());
                                    }
                                }
                            }
                            customItems.clear();

                        });
                    }
                }

            });

        });
    }

    private void getAllBlocksInClaim(Claim claim, Consumer<List<Block>> callback) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Block> blocks = new ArrayList<>();
            List<Material> materialsToSkip = Arrays.asList(Material.AIR, Material.GRASS, Material.STONE, Material.DIRT, Material.COBBLESTONE);
            int blocksPerTick = 1000; // Adjust this value as needed
            for (int x = claim.getLesserBoundaryCorner().getBlockX(); x <= claim.getGreaterBoundaryCorner().getBlockX(); x++) {
                for (int y = -30; y <= 300; y++) {
                    for (int z = claim.getLesserBoundaryCorner().getBlockZ(); z <= claim.getGreaterBoundaryCorner().getBlockZ(); z++) {
                        Material blockType = claim.getLesserBoundaryCorner().getWorld().getBlockAt(x, y, z).getType();
                        Material blockTypeAbove = claim.getGreaterBoundaryCorner().getWorld().getBlockAt(x, y, z).getType();
                        if (materialsToSkip.contains(blockType) || materialsToSkip.contains(blockTypeAbove)) {
                            continue;
                        }
                        blocks.add(claim.getLesserBoundaryCorner().getWorld().getBlockAt(x, y, z));
                        if (blocks.size() >= blocksPerTick) {
                            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>(blocks)));
                            blocks.clear();
                        }
                    }
                }
            }
            if (!blocks.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(blocks));
            }
        });
    }

    private void saveItem(List<ItemStack> items, String filePath, Location location) {
        plugin.getLogger().info("Saving items to file...");
        File file = new File(plugin.getDataFolder(), filePath);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            plugin.getLogger().info("Creating directories...");
            parentDir.mkdirs(); // Create the directories if they don't exist
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Create a unique key for each chest using its location
        String key = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();

        List<Map<String, Object>> itemList = new ArrayList<>();

        for (ItemStack item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("customModelData", item.getItemMeta().getCustomModelData());
            itemMap.put("itemType", item.getType().toString());
            itemMap.put("amount", item.getAmount());
            itemMap.put("lore", item.getItemMeta().getLore()); // Assuming the old owner is stored in the lore
            itemMap.put("displayName", item.getItemMeta().getDisplayName());
            itemList.add(itemMap);
        }

        config.set(key, itemList);

        try {
            config.save(file);
            plugin.getLogger().info("Items saved to file.");
        } catch (IOException e) {
            plugin.getLogger().info("Error while saving items to file:");
            e.printStackTrace();
        }
    }
}