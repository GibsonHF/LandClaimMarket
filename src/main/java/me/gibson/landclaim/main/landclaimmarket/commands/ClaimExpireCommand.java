package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ClaimExpireCommand  implements CommandExecutor {

    public LandClaimMarket plugin;

    public ClaimExpireCommand(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("claimexpire")) {
            if (!sender.hasPermission("landclaimmarket.claimexpire")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            if(args.length != 0) {
                sender.sendMessage("Usage: /claimexpire");
                return true;
            }
            Player player = (Player) sender;
            //set claim expired
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
            if(claim == null) {
                player.sendMessage("You must be in a claim to expire it.");
                return true;
            }
            if(claim.isAdminClaim()) {
                player.sendMessage("You cannot expire an admin claim.");
                return true;
            }

            if(!claim.getOwnerName().equals(player.getName()) && !player.hasPermission("landclaimmarket.bypass")){
                player.sendMessage("You must be the owner of the claim to expire it.");
                return true;
            }
            player.sendMessage("start Claim has been expired.");
            ClaimExpirationEvent event = new ClaimExpirationEvent(claim);
            Bukkit.getServer().getPluginManager().callEvent(event);
            player.sendMessage("Claim has been expired.");
            return true;
        }

        return false;
    }

}
