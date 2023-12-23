package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadConfigCommand implements CommandExecutor {

    public LandClaimMarket plugin;

    public ReloadConfigCommand(LandClaimMarket plugin) {
        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("landclaimmarket.reloadconfig")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if(args.length != 0) {
            sender.sendMessage("This command does not take any arguments.");
            return true;
        }

        sender.sendMessage("Reloading config...");
        plugin.reloadConfig();
        sender.sendMessage("Config reloaded.");

        return true;
    }
}
