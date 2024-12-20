package me.gibson.landclaim.main.landclaimmarket.listeners;

import com.griefprevention.visualization.VisualizationType;
import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.utils.AsyncClaimLogger;
import me.gibson.landclaim.main.landclaimmarket.utils.ClaimInfo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.*;
import net.milkbowl.vault.chat.Chat;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InventoryListener implements Listener {
    public static LandClaimMarket plugin;

    private String currentSortType = "oldest"; // Add this field to store the current sort type
    private String[] sortTypes = {"oldest", "newest", "biggest", "smallest"}; // Add this field to store the sort types
    private int currentSortIndex = Arrays.asList(sortTypes).indexOf(currentSortType); // Initialize currentSortIndex based on currentSortType

    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();





    public InventoryListener(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!event.getView().getTitle().startsWith("Claims for Sale - Page ") && !event.getView().getTitle().startsWith("Owned Claims - Page ") && !event.getView().getTitle().startsWith("Real Estate") && !event.getView().getTitle().startsWith("Confirm Purchase") && !event.getView().getTitle().startsWith("Upcoming Expired Claims - Page ")) {
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

        if (event.getView().getTitle().startsWith("Real Estate")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null) {
                return;
            }

            String itemName = clickedItem.getItemMeta().getDisplayName();

            if(event.getSlot() == 12){
                player.closeInventory();
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Inventory playerClaimsInv = createPlayerClaimsGUI(player, 1);
                        player.openInventory(playerClaimsInv);
                    }
                }, 5L);
            } else if (event.getSlot() == 14) {
                player.closeInventory();
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Inventory realEstateInv = getClaimsInventory(1);

                        player.openInventory(realEstateInv);
                    }
                }, 5L);
            }  else if (event.getSlot() == 26)
            {
                player.closeInventory();
            }
        }

        if (event.getView().getTitle().startsWith("Owned Claims - Page")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            //add support for pages
            if (event.getSlot() == 48 || event.getSlot() == 50) {
                String title = event.getView().getTitle();
                int currentPage = extractPageNumber(title); // Extract the current page number from the title
                int newPage = event.getSlot() == 48 ? currentPage - 1 : currentPage + 1;

                Inventory newInv = createPlayerClaimsGUI(player, newPage);
                player.openInventory(newInv);
                return;
            }

            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();

            if (!meta.hasCustomModelData()) {
                return;
            }

            int claimId = meta.getCustomModelData();
            Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);

            if (claim == null) {
                plugin.getSettings().sendPlayerMessage(player, "claimNoLongerExists");
                return;
            }

            Location center = claim.getLesserBoundaryCorner().add(claim.getGreaterBoundaryCorner()).multiply(0.5);
            for(int i = 255; i >= 0; i--) {
                if(center.clone().add(0, i, 0).getBlock().getType() != Material.AIR) {
                    center.add(0, i, 0);
                    break;
                }
            }

            player.closeInventory();
            plugin.getSettings().sendPlayerMessage(player, "teleportingToClaim");
            teleportTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(center);
                plugin.getSettings().sendPlayerMessage(player, "teleportedToClaim");
                player.closeInventory();
                showClaimBorderWithParticles(player, claim);
                teleportTasks.remove(player.getUniqueId()); // Remove the task after teleporting
            }, 5 * 20L));
        }


        if(event.getView().getTitle().startsWith("Claims for Sale - Page")) {
            if (event.getSlot() == 48 || event.getSlot() == 50) {
                String title = event.getView().getTitle();
                int currentPage = extractPageNumber(title); // Extract the current page number from the title
                int newPage = event.getSlot() == 48 ? currentPage - 1 : currentPage + 1;

                Inventory newInv = getClaimsInventory(newPage);
                player.openInventory(newInv);
                return;
            }
        }

        if(event.getView().getTitle().startsWith("Upcoming Expired Claims - Page")) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                currentSortIndex = (currentSortIndex + 1) % sortTypes.length; // Update the current sort index
                String sortType = sortTypes[currentSortIndex]; // Get the new sort type
                getUpcomingExpiredClaims(upcomingExpiredClaims -> {
                    List<Claim> sortedClaims = sortClaims(upcomingExpiredClaims, sortType);
                    Inventory newInv = createUpcomingExpiredClaimsGUI(sortedClaims, 1); // Open the first page
                    player.openInventory(newInv);
                });
            }
            if (event.getSlot() == 48 || event.getSlot() == 50) {
                String title = event.getView().getTitle();
                int currentPage = extractPageNumber(title); // Extract the current page number from the title
                int newPage = event.getSlot() == 48 ? currentPage - 1 : currentPage + 1;

                plugin.inventoryListener.getUpcomingExpiredClaims(upcomingExpiredClaims -> {
                    Inventory newInv = plugin.inventoryListener.createUpcomingExpiredClaimsGUI(upcomingExpiredClaims, newPage);
                    player.openInventory(newInv);
                });
                return;
            }
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
                    plugin.getSettings().sendPlayerMessage(player, "invalidItem");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidClaimItem");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                UUID ownerUUID = claimInfo.getUUID();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                if(claim.ownerID.equals(player.getUniqueId())) {
                    plugin.getSettings().sendPlayerMessage(player, "cannotPurchaseOwnClaim");
                    return;
                }
                UUID BuyerID = player.getUniqueId();
                if(plugin.getEconomy().getBalance(player) <= claimInfo.getPrice()) {
                    HashMap<String, String> placeholders = new HashMap<>();
                    placeholders.put("current_balance", String.format("%.2f", plugin.getEconomy().getBalance(player)));
                    placeholders.put("claim_price", String.format("%.2f", claimInfo.getPrice()));
                    plugin.getSettings().sendPlayerMessage(player, "notEnoughMoney", placeholders);
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
                String content = createPurchaseContent(claim, claimInfo, BuyerID);
                String avatarUrl = "https://cravatar.eu/helmavatar/" + Bukkit.getOfflinePlayer(BuyerID).getUniqueId() + "/128";
                sendDiscordWebhook(content, avatarUrl);
                plugin.getSettings().sendPlayerMessage(player, "claimPurchased", claimInfo.getPrice());
                player.closeInventory();
            }
            else if(event.getSlot() == 13) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidItem");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidClaimItem");
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
                plugin.getSettings().sendPlayerMessage(player, "teleportingToClaim");
                teleportTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(center);
                    plugin.getSettings().sendPlayerMessage(player, "teleportedToClaim");
                    player.closeInventory();
                    showClaimBorderWithParticles(player, claim);
                    teleportTasks.remove(player.getUniqueId()); // Remove the task after teleporting
                }, 5 * 20L));
            }
            else if(event.getSlot() == 15) {
                player.closeInventory();
            }else if(event.getSlot() == 26)
            {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidItem");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidClaimItem");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                UUID ownerUUID = claimInfo.getUUID();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                if(claimInfo == null) {
                    plugin.getSettings().sendPlayerMessage(player, "claimNoLongerAvailable");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    player.closeInventory();
                    return;
                }
                if(owner.getPlayer() == null)
                {
                    plugin.getSettings().sendPlayerMessage(player, "claimNoLongerAvailable");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    return;
                }
                if(owner.getPlayer().getName() != player.getName() || !player.hasPermission("landclaimmarket.unlist"))
                {
                    plugin.getSettings().sendPlayerMessage(player, "cannotUnlistNotOwned");
                    return;
                }
                removeClaim(claim);
                event.getInventory().remove(clickedItem);
                plugin.getSettings().sendPlayerMessage(player, "claimUnlisted");
                //discord webhook here
                String content = createunlistedContent(claim, claimInfo);
                String avatarUrl = "https://cravatar.eu/helmavatar/" + Bukkit.getOfflinePlayer(ownerUUID).getUniqueId() + "/128";
                sendDiscordWebhook(content, avatarUrl);
                player.closeInventory();
            }else if(event.getSlot() == 25)
            {
                if(!player.hasPermission("landclaimmarket.claimlog"))
                {
                    plugin.getSettings().sendPlayerMessage(player, "noPermissionToLogClaims");
                    return;
                }
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidItem");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidClaimItem");
                    return;
                }
                Claim claim = GriefPrevention.instance.dataStore.getClaim(meta.getCustomModelData());
                if(claim == null)
                {
                    plugin.getSettings().sendPlayerMessage(player, "claimNoLongerExists");
                    return;
                }
                AsyncClaimLogger asyncClaimLogger = new AsyncClaimLogger(plugin);
                asyncClaimLogger.logClaimAsync(claim);
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Claim has been logged.");
            }
        }
        else if(event.getView().getTitle().startsWith("Claims for Sale - Page "))
        {
            if (event.isLeftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidItem");
                    return;
                }
                ItemMeta meta = clickedItem.getItemMeta();
                if (!meta.hasCustomModelData()) {
                    plugin.getSettings().sendPlayerMessage(player, "invalidClaimItem");
                    return;
                }

                int claimId = meta.getCustomModelData();
                Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
                ClaimInfo claimInfo = plugin.claimsForSale.get(claim);
                if (claimInfo == null) {
                    plugin.getSettings().sendPlayerMessage(player, "claimNoLongerAvailable");
                    removeClaim(claim);
                    event.getInventory().remove(clickedItem);
                    player.closeInventory();
                    return;
                }
                if (claim.ownerID == null) {
                    plugin.getSettings().sendPlayerMessage(player, "claimNotForSale");
                    return;
                }
                UUID BuyerID = player.getUniqueId();
                if (claim.ownerID == null) {
                    plugin.getSettings().sendPlayerMessage(player, "claimDoesNotExist");
                    return;
                }
                if (plugin.getEconomy().getBalance(player) <= claimInfo.getPrice() && !player.hasPermission("landclaimmarket.bypass") && plugin.getConfig().getBoolean("booleans.requireMoney")) {
                    HashMap<String, String> placeholders = new HashMap<>();
                    placeholders.put("current_balance", String.format("%.2f", plugin.getEconomy().getBalance(player)));
                    placeholders.put("claim_price", String.format("%.2f", claimInfo.getPrice()));
                    plugin.getSettings().sendPlayerMessage(player, "notEnoughMoney", placeholders);
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
        if(!plugin.getConfig().getBoolean("booleans.enableClaimExpiration")) {
            return;
        }
        event.setCancelled(true);

        Claim claim = event.getClaim();
        UUID oldOwner = claim.ownerID;
        UUID taxId = UUID.fromString(Bukkit.getOfflinePlayer(plugin.getConfig().getString("settings.systemID")).getUniqueId().toString());
        changeOwner(claim, taxId);

        double price = plugin.getConfig().getDouble("settings.claimPrice") * claim.getArea();
        LocalDateTime dateAdded = LocalDateTime.now();

        plugin.getClaimsForSale().put(claim, new ClaimInfo(taxId, price, claim.getID(), dateAdded));
        plugin.SaveClaims();

        ClaimInfo claimInfo = new ClaimInfo(oldOwner, price, claim.getID(), dateAdded);
        String content = createContent(claim, claimInfo);
        String avatarUrl = "https://cravatar.eu/helmavatar/" + Bukkit.getOfflinePlayer(claimInfo.getUUID()).getUniqueId() + "/128";
        sendDiscordWebhook(content, avatarUrl);
    }

    private String createContent(Claim claim, ClaimInfo claimInfo) {
        DecimalFormat df = new DecimalFormat("#,###");
        double price = claimInfo.getPrice();
        String formattedPrice = df.format(price);
        return String.format("``EXPIRED CLAIM\n\nClaim ID: %s\n\nOld Owner: %s\n\nPrice: $%s\n\nArea size: %d blocks``",
                claim.getID(), Bukkit.getOfflinePlayer(claimInfo.getUUID()).getName(), formattedPrice, claim.getArea());
    }

    private String createunlistedContent(Claim claim, ClaimInfo claimInfo) {
        DecimalFormat df = new DecimalFormat("#,###");
        double price = claimInfo.getPrice();
        String formattedPrice = df.format(price);
        //make it say Player unlisted claim

        return String.format("``UNLISTED CLAIM\n\nClaim ID: %s\n\nOwner: %s\n\nPrice: $%s\n\nArea size: %d blocks``",
                claim.getID(), Bukkit.getOfflinePlayer(claimInfo.getUUID()).getName(), formattedPrice, claim.getArea());
    }

    //create Purchase content which is just basic code message saying player bought x claim from x player for x price
    private String createPurchaseContent(Claim claim, ClaimInfo claimInfo, UUID buyerID) {
        DecimalFormat df = new DecimalFormat("#,###");
        double price = claimInfo.getPrice();
        String formattedPrice = df.format(price);
        //include new owner as well as old owner
        //lets just make it a 1 liner in `` block saying buyerid bought claimid for price no new lines
        return String.format("``%s Purchased Claim %s for %s. Area size: %d blocks``",
                Bukkit.getOfflinePlayer(buyerID).getName(), claim.getID(), formattedPrice, claim.getArea());
//        return String.format("``PURCHASED CLAIM\n\nClaim ID: %s\n\nNew Owner: %s\n\nPrice: $%s\n\nArea size: %d blocks``",
//                claim.getID(), Bukkit.getOfflinePlayer(buyerID).getName(), formattedPrice, claim.getArea());
    }

    public static Inventory confirmPurchaseInventory(int claimId, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Confirm Purchase");
        Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
        inv.setItem(11, createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Confirm Purchase", claimId));
        inv.setItem(13, createItem(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Teleport to Claim", claimId));
        inv.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Cancel Purchase", claimId));
        if(player.hasPermission("landclaimmarket.unlist") || claim.ownerID.equals(player.getUniqueId()))
        {
            inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Unlist Claim", claimId));
        }
        if(player.hasPermission("landclaimmarket.claimlog"))
        {
            inv.setItem(25, createItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "Log Claim", claimId));
        }
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

        claimsList.sort(Comparator.comparing(claim -> plugin.getClaimsForSale().get(claim).getDateAdded()).reversed());

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
        String dateAdded = info.getDateAdded().format(formatter);

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
        lore.add(ChatColor.GOLD + "Date Added: " + ChatColor.BLUE + dateAdded);
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
        ArrayList<String> managers = claim.managers;
        for (String manager : managers) {
            claim.dropPermission(manager);
        }
    }

    private int extractPageNumber(String title) {
        try {
            return Integer.parseInt(title.substring(title.lastIndexOf(' ') + 1));
        } catch (NumberFormatException e) {
            return 1; // Default to the first page if the page number is not found
        }
    }

    public void sendDiscordWebhook(String content, String avatarUrl) {
        try {
            URL url = new URL("https://ptb.discord.com/api/webhooks/1188729419272028250/u1DertQ2VwQmneDCOAoY7OhRTY9B7ivtkIlC0b4PHTeF44fYw5YzLxyLV83xnflWXgIU");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");
            String escapedContent = StringEscapeUtils.escapeJava(content);
            String jsonPayload = String.format("{\"embeds\":[{\"title\":\"Claim Information\",\"description\":\"`%s`\",\"thumbnail\":{\"url\":\"%s\"}}]}", escapedContent, avatarUrl);

            OutputStream os = http.getOutputStream();
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);

            int responseCode = http.getResponseCode();
            System.out.println("Response Code : " + responseCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to get upcoming expired claims
    public void getUpcomingExpiredClaims(Consumer<List<Claim>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Claim> upcomingExpiredClaims = new ArrayList<>();
            for (Claim claim : getAllClaims()) {
                LocalDateTime expirationDate = getExpirationDate(claim);
                if (expirationDate == null) {
                    continue; // Skip this claim if the expiration date is null
                }
                if (isAboutToExpire(claim)) {
                    upcomingExpiredClaims.add(claim);
                }
            }
            upcomingExpiredClaims.sort(Comparator.comparing(this::getExpirationDate));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(upcomingExpiredClaims));
        });
    }


    // Method to check if a claim is about to expire
    public boolean isAboutToExpire(Claim claim) {
        LocalDateTime expirationDate = getExpirationDate(claim);
        if (expirationDate == null) {
            // Skip the claim if the expiration date is null
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilExpiration = Duration.between(now, expirationDate);

        // Return false if the claim has already expired
        if (timeUntilExpiration.isNegative()) {
            return false;
        }
        return timeUntilExpiration.toHours() <= 96; // Change this to adjust the threshold
    }

    public Inventory createUpcomingExpiredClaimsGUI(List<Claim> claims, int page) {
        int itemsPerPage = 45; // This can be adjusted as needed
        int totalItems = claims.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        // Ensure the page number is within the valid range
        page = Math.max(1, Math.min(page, totalPages));

        // Calculate the start and end index for the sublist of claims for this page
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        // Get the sublist of claims for this page
        List<Claim> claimsForPage = claims.subList(startIndex, endIndex);

        // Create the inventory
        Inventory inv = Bukkit.createInventory(null, 54, "Upcoming Expired Claims - Page " + page);

        // Add the claims to the inventory
        for (Claim claim : claimsForPage) {
            inv.addItem(createClaimItem2(claim));
        }

        //pretty up the colors
        inv.setItem(49, createSortButton(Material.HOPPER, ChatColor.GREEN + "Sort List " + sortTypes[currentSortIndex].toUpperCase(), sortTypes[currentSortIndex]));
        //inv.setItem(49, createSortButton(Material.HOPPER, "Sort by: " + sortTypes[currentSortIndex].toUpperCase(), sortTypes[currentSortIndex])); // Display the current sort type


        // Add navigation items
        if (page > 1) {
            // Add a "previous page" item if this isn't the first page
            inv.setItem(48, createNavigationItem(Material.ARROW, "Previous Page"));
        }
        if (page < totalPages) {
            // Add a "next page" item if this isn't the last page
            inv.setItem(50, createNavigationItem(Material.ARROW, "Next Page"));
        }

        return inv;
    }
    private ItemStack createSortButton(Material material, String displayName, String sortType) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(Collections.singletonList(ChatColor.DARK_AQUA+"Sort Type: "+ ChatColor.GREEN + sortType));
        item.setItemMeta(meta);
        return item;
    }
    private List<Claim> sortClaims(List<Claim> claims, String sortType) {
        switch (sortType) {
            case "oldest":
                return claims.stream().sorted(Comparator.comparing(this::getExpirationDate)).collect(Collectors.toList());
            case "newest":
                return claims.stream().sorted(Comparator.comparing(this::getExpirationDate).reversed()).collect(Collectors.toList());
            case "biggest":
                return claims.stream().sorted(Comparator.comparing(Claim::getArea).reversed()).collect(Collectors.toList());
            case "smallest":
                return claims.stream().sorted(Comparator.comparing(Claim::getArea)).collect(Collectors.toList());
            default:
                return claims;
        }
    }

    private ItemStack createClaimItem2(Claim claim) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD); // Change this to the material you want
        SkullMeta meta = (SkullMeta) item.getItemMeta(); // Cast the ItemMeta to SkullMeta

        LocalDateTime expirationDate = getExpirationDate(claim);
        Duration timeUntilExpiration = Duration.between(LocalDateTime.now(), expirationDate);
        long hoursUntilExpiration = timeUntilExpiration.toHours();

        // Format the expiration date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
        String formattedExpirationDate = expirationDate.format(formatter);

        meta.setDisplayName("Claim ID: " + claim.getID());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_AQUA + "Expires on: " + ChatColor.YELLOW + formattedExpirationDate);
        lore.add(ChatColor.BLUE + "Time until expiration: " + ChatColor.RED + hoursUntilExpiration + " hours");
        lore.add(ChatColor.DARK_GREEN + "Claim size: " + ChatColor.LIGHT_PURPLE + claim.getArea() + " blocks");
        lore.add(ChatColor.GOLD + "Location: " + ChatColor.WHITE + "X: " + claim.getLesserBoundaryCorner().getBlockX() + ", Y: " + claim.getLesserBoundaryCorner().getBlockY() + ", Z: " + claim.getLesserBoundaryCorner().getBlockZ());
        lore.add(ChatColor.DARK_RED + "Claim owner: " + ChatColor.AQUA + claim.getOwnerName());
        meta.setLore(lore);
        meta.setOwner(Bukkit.getOfflinePlayer(claim.getOwnerID()).getName()); // Set the owner of the skull to the owner of the claim
        item.setItemMeta(meta);
        return item;
    }

    public List<Claim> getAllClaims() {
        return new ArrayList<>(GriefPrevention.instance.dataStore.getClaims());
    }

    public LocalDateTime getExpirationDate(Claim claim) {
        UUID ownerUUID = claim.getOwnerID();

        if (ownerUUID == null) {
            // Handle the case where the owner's UUID is null
            // For example, return a default expiration date
            return null; // Change this to your default expiration date
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        long lastPlayed = owner.getLastPlayed(); // This is in milliseconds since the epoch
        LocalDateTime lastOnline = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastPlayed), ZoneId.systemDefault());

        // Assuming claims expire 90 days after the owner was last online
        LocalDateTime expirationDate = lastOnline.plusDays(75);

        return expirationDate;
    }

   //get player owned claims
    public List<Claim> getPlayerClaims(UUID playerUUID) {
        List<Claim> playerClaims = new ArrayList<>();
        for (Claim claim : getAllClaims()) {
            if (claim.getOwnerID() == null) {
                continue; // Skip this claim if the owner's UUID is null
            }
            if (claim.getOwnerID().equals(playerUUID)) {
                playerClaims.add(claim);
            }
        }

        playerClaims.sort(Comparator.comparing(claim -> plugin.getClaimsForSale().containsKey(claim) ? 0 : 1).reversed());


        return playerClaims;
    }

    //get player owned claims
    public List<Claim> getPlayerClaims(OfflinePlayer player) {
        return getPlayerClaims(player.getUniqueId());
    }

    private static ItemStack createItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    public Inventory createMainGUI() {
        Inventory inv = Bukkit.createInventory(null, 27, "Real Estate");

        inv.setItem(12, createItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Player Owned Claims"));
        inv.setItem(14, createItem(Material.OAK_SIGN, ChatColor.YELLOW + "Real Estate for Sale"));

        inv.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + " ");

        return inv;
    }

    //gui to display player owned claims
    public Inventory createPlayerClaimsGUI(OfflinePlayer player, int page) {
        List<Claim> playerClaims = getPlayerClaims(player);
        int itemsPerPage = 45; // This can be adjusted as needed
        int totalItems = playerClaims.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        // Ensure the page number is within the valid range
        page = Math.max(1, Math.min(page, totalPages));

        // Calculate the start and end index for the sublist of claims for this page
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        // Get the sublist of claims for this page
        List<Claim> claimsForPage = playerClaims.subList(startIndex, endIndex);

        // Create the inventory
        Inventory inv = Bukkit.createInventory(null, 54, "Owned Claims - Page " + page);

        // Add the claims to the inventory
        for (Claim claim : claimsForPage) {
            inv.addItem(createClaimItem3(claim));
        }

        //add item for total price of all claims getconfig.getdouble("options.claimprice") * claim.getarea()
        double totalClaimPrice = 0;
        for(Claim claim : playerClaims)
        {
            totalClaimPrice += plugin.getConfig().getDouble("settings.claimPrice") * claim.getArea();
        }
        DecimalFormat df = new DecimalFormat("#,###");
        String formattedPrice = df.format(totalClaimPrice);
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Total Claim Value: " + ChatColor.GREEN + "$" + formattedPrice);
        item.setItemMeta(meta);
        inv.setItem(49, item);

        // Add navigation items
        if (page > 1) {
            // Add a "previous page" item if this isn't the first page
            inv.setItem(48, createNavigationItem(Material.ARROW, "Previous Page"));
        }
        if (page < totalPages) {
            // Add a "next page" item if this isn't the last page
            inv.setItem(50, createNavigationItem(Material.ARROW, "Next Page"));
        }
        return inv;
    }

    private ItemStack createClaimItem3(Claim claim) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD); // Change this to the material you want

        // Create a list for the lore
        List<String> lore = new ArrayList<>();
        int claimid = Math.toIntExact(claim.getID());

        // Add the claim id, size, and location to the lore
        lore.add(ChatColor.GRAY + "Claim Size: " + ChatColor.GREEN + claim.getArea() + " Blocks");
        lore.add(ChatColor.GRAY + "Location: " + ChatColor.GREEN + "X: " + claim.getLesserBoundaryCorner().getBlockX() + " Y: " + claim.getLesserBoundaryCorner().getBlockY() + " Z: " + claim.getLesserBoundaryCorner().getBlockZ());
        lore.add(ChatColor.YELLOW + "Left click to teleport.");
        if(plugin.getClaimsForSale().containsKey(claim))
        {
            lore.add(ChatColor.RED + "This claim is for sale.");
        }
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        skullMeta.setOwner(Bukkit.getOfflinePlayer(claim.ownerID).getName());
        skullMeta.setDisplayName(ChatColor.GRAY + "Claim ID: " + ChatColor.GREEN + claim.getID());
        skullMeta.setLore(lore);
        skullMeta.setCustomModelData(claimid);
        item.setItemMeta(skullMeta);

        // Set the item meta to the item

        return item;
    }

    public void sendPlayerMessage(Player player, String message)
    {
        message.replace("%price%", String.valueOf(plugin.getConfig().getDouble("settings.claimPrice")));
        message.replace("%player%", player.getName());
        message.replace("%claimid%", String.valueOf(plugin.getConfig().getDouble("settings.claimPrice")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void showClaimBorderWithParticles(Player player, Claim claim) {
        World world = player.getWorld();
        int x1 = claim.getLesserBoundaryCorner().getBlockX();
        int z1 = claim.getLesserBoundaryCorner().getBlockZ();
        int x2 = claim.getGreaterBoundaryCorner().getBlockX();
        int z2 = claim.getGreaterBoundaryCorner().getBlockZ();

        // Calculate the area of the claim
        int claimArea = claim.getArea();

        // Calculate the duration based on the claim area
        // Minimum duration is 30 seconds, add 45 seconds for every 15000 blocks in the claim, cap at 180 seconds
        int duration = Math.min(30 + (claimArea / 15000) * 45, 180);

        new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                double y = player.getLocation().getY() + 1; // Get the player's y level inside the run method
                double y2 = player.getLocation().getY(); // Get the player's y level inside the run method

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (int x = x1; x <= x2; x++) {
                        for (int z = z1; z <= z2; z++) {
                            if (x == x1 || x == x2 || z == z1 || z == z2) {
                                world.spawnParticle(Particle.VILLAGER_HAPPY, x, y, z, 1);
                                world.spawnParticle(Particle.VILLAGER_HAPPY, x, y2, z, 1);
                            }
                        }
                    }
                });
                counter++;
                if (counter >= duration) { // Duration based on claim size
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20); // run every 1 second (20 ticks = 1 second)
    }
}
