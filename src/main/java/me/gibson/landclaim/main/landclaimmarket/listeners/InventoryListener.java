package me.gibson.landclaim.main.landclaimmarket.listeners;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.utils.ClaimInfo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class InventoryListener implements Listener {
    public static LandClaimMarket plugin;

    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();

    private BukkitTask logTask;




    public InventoryListener(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!event.getView().getTitle().startsWith("Claims for Sale - Page ") && !event.getView().getTitle().startsWith("Confirm Purchase")) {
            return;
        }
        if(!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        if (event.getSlot() == 48 || event.getSlot() == 50) {
            String title = event.getView().getTitle();
            int currentPage = extractPageNumber(title); // Extract the current page number from the title
            int newPage = event.getSlot() == 48 ? currentPage - 1 : currentPage + 1;

            Inventory newInv = getClaimsInventory(newPage);
            player.openInventory(newInv);
            return;
        }


        if(event.getSlot() == 53) {
            player.closeInventory();
            return;
        }

        if(event.getView().getTitle().equalsIgnoreCase("Confirm Purchase")) {
            if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }
            if(event.getSlot() == 11) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    player.sendMessage(ChatColor.RED + "Invalid item.");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    player.sendMessage(ChatColor.RED + "This item does not represent a valid claim.");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                UUID ownerUUID = claimInfo.getUUID();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                if(claim.ownerID.equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot purchase your own claim.");
                    return;
                }
                UUID BuyerID = player.getUniqueId();
                if(plugin.getEconomy().getBalance(player) <= claimInfo.getPrice()) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money to purchase this claim. You have "
                            + ChatColor.GREEN + plugin.getEconomy().getBalance(player) + ChatColor.RED
                            + " and the claim costs " + ChatColor.GREEN + claimInfo.getPrice());
                    return;
                }
                plugin.getEconomy().withdrawPlayer(player, claimInfo.getPrice());
                if(owner.isOnline())
                {
                    plugin.getEconomy().depositPlayer(owner.getPlayer(), claimInfo.getPrice());
                }
                else
                {
                    plugin.getEconomy().depositPlayer(owner, claimInfo.getPrice());
                }
                if(owner.isOnline()) {
                    owner.getPlayer().sendMessage(ChatColor.GREEN + "Your claim has been purchased by "
                            + ChatColor.AQUA + player.getName()
                            + ChatColor.GREEN + " for $" + claimInfo.getPrice());
                }
                changeOwner(claim, BuyerID);
                plugin.getClaimsForSale().remove(claim);
                event.getInventory().remove(clickedItem);
                plugin.SaveClaims();
                player.sendMessage(ChatColor.GREEN + "You have purchased this claim for $" + claimInfo.getPrice());
                player.closeInventory();
            }
            else if(event.getSlot() == 13) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    player.sendMessage(ChatColor.RED + "Invalid item.");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    player.sendMessage(ChatColor.RED + "This item does not represent a valid claim.");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                Location center = claim.getLesserBoundaryCorner().add(claim.getGreaterBoundaryCorner()).multiply(0.5);
                for(int i = 255; i >= 0; i--) {
                    if(center.clone().add(0, i, 0).getBlock().getType() != Material.AIR) {
                        center.add(0, i, 0);
                        break;
                    }
                }
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Teleporting to claim in 5 seconds.");
                teleportTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(center);
                    player.sendMessage(ChatColor.GREEN + "You have teleported to this claim.");
                    player.closeInventory();
                    teleportTasks.remove(player.getUniqueId()); // Remove the task after teleporting
                }, 5 * 20L));
            }
            else if(event.getSlot() == 15) {
                player.closeInventory();
            }else if(event.getSlot() == 26)
            {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    player.sendMessage(ChatColor.RED + "Invalid item.");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    player.sendMessage(ChatColor.RED + "This item does not represent a valid claim.");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                UUID ownerUUID = claimInfo.getUUID();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                if(claimInfo == null) {
                    player.sendMessage(ChatColor.RED + "This claim is no longer available.");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    player.closeInventory();
                    return;
                }
                if(owner.getPlayer() == null)
                {
                    player.sendMessage(ChatColor.RED + "This claim is no longer available.");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    return;
                }
                if(owner.getPlayer().getName() != player.getName() || !player.hasPermission("landclaimmarket.unlist"))
                {
                    player.sendMessage(ChatColor.RED + "You cannot unlist a claim you do not own.");
                    return;
                }
                removeClaim(claim);
                event.getInventory().remove(clickedItem);
                player.sendMessage(ChatColor.GREEN + "You have unlisted this claim.");
                player.closeInventory();
            } else if (event.getSlot() == 25) {
                // Log claim data
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    player.sendMessage(ChatColor.RED + "Invalid item.");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    player.sendMessage(ChatColor.RED + "This item does not represent a valid claim.");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                UUID ownerUUID = claimInfo.getUUID();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                if(claimInfo == null) {
                    player.sendMessage(ChatColor.RED + "This claim is no longer available.");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    player.closeInventory();
                    return;
                }
                if(owner.getPlayer() == null)
                {
                    player.sendMessage(ChatColor.RED + "This claim is no longer available.");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    return;
                }
                if(!player.hasPermission("landclaimmarket.claimlog"))
                {
                    player.sendMessage(ChatColor.RED + "You cannot log a claim as you do not have permission.");
                    return;
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    List<Location> locations = new ArrayList<>();

                    for (int x = claim.getLesserBoundaryCorner().getBlockX(); x <= claim.getGreaterBoundaryCorner().getBlockX(); x++) {
                        for (int y = claim.getLesserBoundaryCorner().getBlockY(); y <= 300; y++) {
                            for (int z = claim.getLesserBoundaryCorner().getBlockZ(); z <= claim.getGreaterBoundaryCorner().getBlockZ(); z++) {
                                locations.add(new Location(claim.getLesserBoundaryCorner().getWorld(), x, y, z));
                            }
                        }
                    }

                    // Process the blocks synchronously
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        List<Block> inventoryBlocks = new ArrayList<>();
                        for (Location loc : locations) {
                            Block block = loc.getBlock();
                            if (block.getState() instanceof InventoryHolder) {
                                inventoryBlocks.add(block);
                            }
                        }
                        logCustomChestContents(claim, ownerUUID, inventoryBlocks);
                        for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                            if (onlinePlayers.hasPermission("landclaimmarket.claimlog")) {
                                onlinePlayers.sendMessage(ChatColor.GREEN + "Claim " + claim.getID() + " has expired and is now for sale");
                            }
                        }
                    });
                });
                player.closeInventory();


            }
        }
        else if(event.getView().getTitle().startsWith("Claims for Sale - Page "))
        {
            if (event.isLeftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    player.sendMessage(ChatColor.RED + "Invalid item.");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    player.sendMessage(ChatColor.RED + "This item does not represent a valid claim.");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                if (claimInfo == null) {
                    player.sendMessage(ChatColor.RED + "This claim is no longer available.");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    player.closeInventory();
                    return;
                }
                if (claim.ownerID == null) {
                    player.sendMessage(ChatColor.RED + "This claim is not for sale.");
                    return;
                }
                UUID BuyerID = player.getUniqueId();
                if (claim.ownerID == null) {
                    player.sendMessage(ChatColor.RED + "This claim does not exist.");
                    return;
                }
                if (plugin.getEconomy().getBalance(player) <= claimInfo.getPrice()) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money to purchase this claim. You have "
                            + ChatColor.GREEN + plugin.getEconomy().getBalance(player) + ChatColor.RED
                            + " and the claim costs " + ChatColor.GREEN + claimInfo.getPrice());
                    return;
                }
                //open a confirmation gui with teleport or purchase buttons
                player.closeInventory();
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Inventory inv = confirmPurchaseInventory(claimId, player);
                        player.openInventory(inv);
                    }
                }, 5L);
                player.closeInventory();
            }
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BukkitTask teleportTask = teleportTasks.get(player.getUniqueId());

        if (teleportTask != null) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Ignore the event if the player has moved less than 0.5 blocks horizontally
            if (Math.abs(from.getX() - to.getX()) < 0.5 && Math.abs(from.getZ() - to.getZ()) < 0.5) {
                return;
            }

            teleportTask.cancel();
            player.sendMessage(ChatColor.RED + "Teleportation cancelled because you moved.");
            teleportTasks.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onClaimStuff(ClaimDeletedEvent event) {
        Claim claim = event.getClaim();
        if(plugin.getClaimsForSale().containsKey(claim)) {
            plugin.getClaimsForSale().remove(claim);
            plugin.SaveClaims();
        }
    }

    @EventHandler
    public void onClaimExpiration(ClaimExpirationEvent event) {
        if(!plugin.getConfig().getBoolean("options.enableClaimExpiration")) {
            return;
        }
        event.setCancelled(true);

        Claim claim = event.getClaim();
        //log old owner
        UUID oldOwner = claim.ownerID;
        UUID taxId = UUID.fromString(plugin.getConfig().getString("options.systemID"));
        changeOwner(claim, taxId);

        double price = plugin.getConfig().getDouble("options.claimPrice");
        int blocks = claim.getArea();
        price = price * blocks;
        plugin.getClaimsForSale().put(claim, new ClaimInfo(taxId, price, claim.getID()));
        plugin.SaveClaims();

        if(plugin.getConfig().getBoolean("options.logExpiredClaims")) {

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Location> locations = new ArrayList<>();

                for (int x = claim.getLesserBoundaryCorner().getBlockX(); x <= claim.getGreaterBoundaryCorner().getBlockX(); x++) {
                    for (int y = 0; y <= 300; y++) {
                        for (int z = claim.getLesserBoundaryCorner().getBlockZ(); z <= claim.getGreaterBoundaryCorner().getBlockZ(); z++) {
                            if(claim.getLesserBoundaryCorner().getWorld().getBlockAt(x, y, z).getType() == Material.AIR) {
                                continue;
                            }
                            if(claim.getLesserBoundaryCorner().getWorld().getBlockAt(x, y, z).getType() == Material.CAVE_AIR) {
                                continue;
                            }

                            locations.add(new Location(claim.getLesserBoundaryCorner().getWorld(), x, y, z));
                        }
                    }
                }

                // Process the blocks synchronously
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<Block> inventoryBlocks = new ArrayList<>();
                    for (Location loc : locations) {
                        Block block = loc.getBlock();
                        if (block.getState() instanceof InventoryHolder) {
                            inventoryBlocks.add(block);
                        }
                    }
                    logCustomChestContents(claim, oldOwner, inventoryBlocks);
                    for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayers.hasPermission("landclaimmarket.claimlog")) {
                            onlinePlayers.sendMessage(ChatColor.GREEN + "Claim " + claim.getID() + " has expired and is now for sale.");
                        }
                    }
                });
            });
        }
    }


    public void logCustomChestContents(Claim claim, UUID oldOwner, List<Block> blocks) {
        // Gather all necessary data before running the task
        String claimId = claim.getID().toString();
        Map<String, Map<String, List<Map<String, Object>>>> normalItemsMap = new HashMap<>();
        Map<String, Map<String, List<Map<String, Object>>>> customItemsMap = new HashMap<>();

        // Create a queue from the blocks list
        Queue<Block> blockQueue = new LinkedList<>(blocks);

        // Create a repeating task that processes one block from the queue every tick
        logTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!blockQueue.isEmpty()) {
                for (int i = 0; i < 10 && !blockQueue.isEmpty(); i++) {
                    Block block = blockQueue.poll(); // Retrieve and remove the head of the queue

                    if (block.getState() instanceof InventoryHolder) {
                        InventoryHolder holder = (InventoryHolder) block.getState();
                        Inventory inv = holder.getInventory();
                        Location loc = block.getLocation();
                        String locKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                        String blockType = block.getType().toString();

                        Map<String, List<Map<String, Object>>> normalBlockTypeMap = normalItemsMap.getOrDefault(locKey, new HashMap<>());
                        Map<String, List<Map<String, Object>>> customBlockTypeMap = customItemsMap.getOrDefault(locKey, new HashMap<>());
                        List<Map<String, Object>> normalItemList = normalBlockTypeMap.getOrDefault(blockType, new ArrayList<>());
                        List<Map<String, Object>> customItemList = customBlockTypeMap.getOrDefault(blockType, new ArrayList<>());

                        for (ItemStack item : inv.getContents()) {
                            if (item != null && item.hasItemMeta()) {
                                ItemMeta meta = item.getItemMeta();
                                Map<String, Object> itemData = new HashMap<>();
                                itemData.put("itemType", item.getType().toString());
                                itemData.put("displayName", meta.getDisplayName());
                                itemData.put("oldOwner", oldOwner.toString());
                                itemData.put("amount", item.getAmount());
                                if (meta.hasCustomModelData()) {
                                    itemData.put("customModelData", meta.getCustomModelData());
                                    customItemList.add(itemData);
                                } else {
                                    normalItemList.add(itemData);
                                }
                                if (meta.hasLore()) {
                                    itemData.put("lore", meta.getLore());
                                }
                            }
                        }

                        // Only add the itemLists to the blockTypeMaps if they're not empty
                        if (!normalItemList.isEmpty()) {
                            normalBlockTypeMap.put(blockType, normalItemList);
                            normalItemsMap.put(locKey, normalBlockTypeMap);
                        }
                        if (!customItemList.isEmpty()) {
                            customBlockTypeMap.put(blockType, customItemList);
                            customItemsMap.put(locKey, customBlockTypeMap);
                        }
                    }
                }
            } else {
                // Cancel the task and save the data to the file when all blocks have been processed
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    File dirNormals = new File(plugin.getDataFolder(), "Normals");
                    File dirCustoms = new File(plugin.getDataFolder(), "Customs");
                    dirNormals.mkdirs(); // Create the directory if it doesn't exist
                    dirCustoms.mkdirs(); // Create the directory if it doesn't exist

                    File fileNormals = new File(dirNormals, claimId + ".yml");
                    File fileCustoms = new File(dirCustoms, claimId + ".yml");
                    YamlConfiguration configNormals = YamlConfiguration.loadConfiguration(fileNormals);
                    YamlConfiguration configCustoms = YamlConfiguration.loadConfiguration(fileCustoms);

                    configNormals.set("items", normalItemsMap);
                    configCustoms.set("items", customItemsMap);

                    try {
                        configNormals.save(fileNormals);
                        configCustoms.save(fileCustoms);
                        System.out.println("Saved files: " + fileNormals.getPath() + ", " + fileCustoms.getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Failed to save files: " + fileNormals.getPath() + ", " + fileCustoms.getPath());
                    }
                });
                logTask.cancel();
            }
        }, 0L, 1L);
    }


    public static Inventory confirmPurchaseInventory(int claimId, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Confirm Purchase");

        inv.setItem(11, createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Confirm Purchase", claimId));
        inv.setItem(13, createItem(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Teleport to Claim", claimId));
        inv.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Cancel Purchase", claimId));
        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Unlist Claim", claimId));
        //only add this if the player in the gui has permission to log claims
        if(player.hasPermission("landclaimmarket.claimlog"))
            inv.setItem(25, createItem(Material.MAP, ChatColor.GRAY + "Log Claim Data", claimId));

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + " ");

        return inv;
    }

    private static ItemStack createItem(Material material, String displayName, int claimId) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(claimId);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillEmptySlots(Inventory inv, Material material, String displayName) {
        for(int i = 0; i < 27; i++) {
            if(inv.getItem(i) == null) {
                inv.setItem(i, createItem(material, displayName, 0));
            }
        }
    }

    public static Inventory getClaimsInventory(int page) {
        List<Claim> claimsList = getValidClaims();
        int totalClaims = claimsList.size();

        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) totalClaims / itemsPerPage);
        page = Math.max(1, Math.min(page, maxPages));

        Inventory inv = Bukkit.createInventory(null, 54, "Claims for Sale - Page " + page);

        if (totalClaims == 0) {
            inv.setItem(53, createNoClaimsItem());
            return inv;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalClaims);

        for (int i = startIndex; i < endIndex; i++) {
            Claim claim = claimsList.get(i);
            inv.setItem(i % itemsPerPage, createClaimItem(claim));
        }

        addNavigationItems(inv, page, maxPages);

        return inv;
    }

    private static List<Claim> getValidClaims() {
        List<Claim> claimsList = new ArrayList<>(plugin.getClaimsForSale().keySet());
        claimsList.removeIf(claim -> plugin.getClaimsForSale().get(claim) == null);
        return claimsList;
    }

    private static ItemStack createNoClaimsItem() {
        ItemStack noClaimsItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta noClaimsMeta = noClaimsItem.getItemMeta();
        noClaimsMeta.setDisplayName(ChatColor.YELLOW + "No claims for sale.");
        List<String> noClaimsLore = new ArrayList<>();
        noClaimsLore.add(ChatColor.RED + "Click to close.");
        noClaimsMeta.setLore(noClaimsLore);
        noClaimsItem.setItemMeta(noClaimsMeta);
        return noClaimsItem;
    }

    private static ItemStack createClaimItem(Claim claim) {
        ClaimInfo info = plugin.getClaimsForSale().get(claim);
        DecimalFormat df = new DecimalFormat("#,###");
        double price = info.getPrice();
        String formattedPrice = df.format(price);

        int claimId = (int) plugin.getClaimsForSale().get(claim).getClaimId();
        UUID playerUUID = plugin.getClaimsForSale().get(claim).getUUID();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        ItemStack claimItem = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) claimItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        meta.setCustomModelData(claimId);
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.GREEN + "$" + formattedPrice);
        lore.add(ChatColor.GOLD + "Owner: " + ChatColor.BLUE + offlinePlayer.getName());
        lore.add(ChatColor.GOLD + "Claim Size: " + ChatColor.LIGHT_PURPLE + claim.getArea() + " Blocks");
        lore.add(ChatColor.YELLOW + "Left click for options.");
        meta.setLore(lore);
        meta.setOwner(Bukkit.getOfflinePlayer(claim.ownerID).getName());
        meta.setDisplayName(ChatColor.GRAY + "Claim ID: " + ChatColor.GREEN + claim.getID());
        claimItem.setItemMeta(meta);
        return claimItem;
    }

    private static void addNavigationItems(Inventory inv, int page, int maxPages) {
        if (page > 1) {
            inv.setItem(48, createNavigationItem(Material.ARROW, "Previous Page"));
        }
        if (page < maxPages) {
            inv.setItem(50, createNavigationItem(Material.ARROW, "Next Page"));
        } else {
            ItemStack closeItem = new ItemStack(Material.BARRIER, 1);
            ItemMeta closeMeta = closeItem.getItemMeta();
            closeMeta.setDisplayName(ChatColor.RED + "Close");
            closeItem.setItemMeta(closeMeta);
            inv.setItem(53, closeItem);
        }
    }

    private static ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static void removeClaim(Claim claim) {
        plugin.claimsForSale.remove(claim);
        plugin.SaveClaims();
    }

    public void changeOwner(Claim claim, UUID newOwner) {
        GriefPrevention.instance.dataStore.changeClaimOwner(claim, newOwner);
    }

    private int extractPageNumber(String title) {
        try {
            return Integer.parseInt(title.substring(title.lastIndexOf(' ') + 1));
        } catch (NumberFormatException e) {
            return 1; // Default to the first page if the page number is not found
        }
    }

}