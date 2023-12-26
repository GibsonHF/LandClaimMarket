package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClaimTeleportCommand implements CommandExecutor {

    public LandClaimMarket plugin;

    public ClaimTeleportCommand(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("claimteleport")) {
            if (!sender.hasPermission("landclaimmarket.claimteleport")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage("This command takes exactly one argument.");
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            int claimId;
            try {
                claimId = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("The argument must be a number.");
                return true;
            }
            if (claimId < 1) {
                sender.sendMessage("The argument must be a positive number.");
                return true;
            }
            // Check if the claim exists in the GriefPrevention data store
            Claim claim = GriefPrevention.instance.dataStore.getClaim(claimId);
            if (claim == null) {
                sender.sendMessage("There is no claim with that ID in the GriefPrevention data store.");
                return true;
            }
            Location center = claim.getLesserBoundaryCorner().add(claim.getGreaterBoundaryCorner()).multiply(0.5);
            for(int i = 255; i >= 0; i--) {
                if(center.clone().add(0, i, 0).getBlock().getType() != Material.AIR) {
                    center.add(0, i, 0);
                    break;
                }
            }
            player.teleport(center);
            return true;
        }
        return false;
    }
}
