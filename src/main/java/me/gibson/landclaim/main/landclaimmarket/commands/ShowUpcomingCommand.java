package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class ShowUpcomingCommand implements CommandExecutor {

    private final LandClaimMarket plugin;

    public ShowUpcomingCommand(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("showupcoming")) {

            if (!sender.hasPermission("landclaimmarket.showupcoming")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            plugin.inventoryListener.getUpcomingExpiredClaims(upcomingExpiredClaims -> {
                Inventory gui = plugin.inventoryListener.createUpcomingExpiredClaimsGUI(upcomingExpiredClaims, 1);
                player.openInventory(gui);
            });
            return true;
        }
        return false;
    }
}
