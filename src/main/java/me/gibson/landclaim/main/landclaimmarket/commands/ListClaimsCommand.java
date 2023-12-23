package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListClaimsCommand implements CommandExecutor {
    private final LandClaimMarket plugin;

    public ListClaimsCommand(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if(!sender.hasPermission("landclaimmarket.listclaims")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if(args.length != 0) {
            sender.sendMessage("This command does not take any arguments.");
            return true;
        }

        Player player = (Player) sender;

        player.openInventory(plugin.inventoryListener.getClaimsInventory(1));



        return true;
    }
}
