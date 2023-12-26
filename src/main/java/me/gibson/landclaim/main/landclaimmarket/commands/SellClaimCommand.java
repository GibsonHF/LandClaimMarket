package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.utils.ClaimInfo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

public class SellClaimCommand implements CommandExecutor {
    private final LandClaimMarket plugin;

    public SellClaimCommand(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("onCommand called"); // Debug statement

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if(!sender.hasPermission("landclaimmarket.sellclaim")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        plugin.getLogger().info("Claim: " + claim); // Debug statement

        if (claim == null || !claim.ownerID.equals(player.getUniqueId())) {
            player.sendMessage("You must be in your claim to sell it.");
            return true;
        }

        if(claim.children.size() > 0) {
            player.sendMessage("You cannot sell a claim with subclaims.");
            return true;
        }

        if(claim.isAdminClaim()) {
            player.sendMessage("You cannot sell an admin claim.");
            return true;
        }

        if(claim.ownerID == null) {
            player.sendMessage("You cannot sell an unowned claim.");
            return true;
        }

       if(!claim.ownerID.equals(player.getUniqueId()) && !player.hasPermission("landclaimmarket.bypass")){
            player.sendMessage("You cannot sell a claim you do not own.");
            return true;
        }

        if(args.length != 1) {
            player.sendMessage("You must specify a price.");
            return true;
        }
        if(!args[0].matches("[0-9]+")) {
            player.sendMessage("You must specify a valid price.");
            return true;
        }
        if(Integer.parseInt(args[0]) < 0) {
            player.sendMessage("You must specify a valid price.");
            return true;
        }

        //make sure claim isnt already for sale
        if(plugin.getClaimsForSale().containsKey(claim)) {
            player.sendMessage("This claim is already for sale.");
            return true;
        }

        int price;
        try{
            price = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("You must specify a valid price.");
            return true;
        }
        LocalDateTime dateAdded = LocalDateTime.now(); // Get the current date and time

        ClaimInfo claimInfo = new ClaimInfo(player.getUniqueId(), price, claim.getID(), dateAdded);
        plugin.getClaimsForSale().put(claim, new ClaimInfo(player.getUniqueId(), price, claim.getID(), dateAdded));
        plugin.SaveClaims();
        DecimalFormat df = new DecimalFormat("#,###");
        double price1 = claimInfo.getPrice();
        String formattedPrice = df.format(price1);
        String content = String.format("``Claim ID: %s\n\nOwner: %s\n\nPrice: $%s\n\nArea size: %d blocks``",
                claim.getID(), Bukkit.getOfflinePlayer(claimInfo.getUUID()).getName(), formattedPrice, claim.getArea());
        String avatarUrl = "https://cravatar.eu/helmavatar/" + Bukkit.getOfflinePlayer(claimInfo.getUUID()).getUniqueId() + "/128"; // This URL will get the Minecraft avatar of the player
        plugin.inventoryListener.sendDiscordWebhook(content, avatarUrl);


        player.sendMessage("Your claim is now listed for sale at $" + price);
        return true;
    }
}