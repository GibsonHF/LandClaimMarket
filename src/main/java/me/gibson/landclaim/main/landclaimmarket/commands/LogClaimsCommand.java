package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.utils.AsyncClaimLogger;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogClaimsCommand implements CommandExecutor
{
    public LandClaimMarket plugin;

    public LogClaimsCommand(final LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
     if(command.getName().equalsIgnoreCase("logclaims")) {
            if (!sender.hasPermission("landclaimmarket.logclaims")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            if(args.length != 0) {
                sender.sendMessage("Usage: /logclaims");
                return true;
            }
            Player player = (Player) sender;

            if(!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
            if(claim == null) {
                player.sendMessage("You must be in a claim to log it.");
                return true;
            }

         AsyncClaimLogger asyncClaimLogger = new AsyncClaimLogger(plugin);
            asyncClaimLogger.logClaimAsync(claim);
            sender.sendMessage("Claim has been logged.");
            return true;
        }
        return false;
    }
}
