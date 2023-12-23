package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.utils.ClaimInfo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

       if(!claim.ownerID.equals(player.getUniqueId())) {
            player.sendMessage("You cannot sell a claim you do not own.");
            return true;
        }

        if(args.length != 1) {
            player.sendMessage("You must specify a price.");
            return true;
        }
        int price;
        try{
            price = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("You must specify a valid price.");
            return true;
        }

        plugin.getClaimsForSale().put(claim, new ClaimInfo(player.getUniqueId(), price, claim.getID()));
        plugin.SaveClaims();


        player.sendMessage("Your claim is now listed for sale at $" + price);
        return true;
    }
}