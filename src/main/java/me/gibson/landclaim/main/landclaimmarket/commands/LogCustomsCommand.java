package me.gibson.landclaim.main.landclaimmarket.commands;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import me.gibson.landclaim.main.landclaimmarket.listeners.InventoryListener;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class LogCustomsCommand implements CommandExecutor {
    private final LandClaimMarket plugin;
    private final InventoryListener inventoryListener;

    public LogCustomsCommand(LandClaimMarket plugin, InventoryListener inventoryListener) {
        this.plugin = plugin;
        this.inventoryListener = inventoryListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null || !player.hasPermission("landclaimmarket.bypass")) {
            player.sendMessage("You must be in your claim to log custom items.");
            return true;
        }

        List<Block> blocks = new ArrayList<>();
        for (int x = claim.getLesserBoundaryCorner().getBlockX(); x <= claim.getGreaterBoundaryCorner().getBlockX(); x++) {
            for (int y = claim.getLesserBoundaryCorner().getBlockY(); y <= 300; y++) {
                for (int z = claim.getLesserBoundaryCorner().getBlockZ(); z <= claim.getGreaterBoundaryCorner().getBlockZ(); z++) {
                    Block block = claim.getLesserBoundaryCorner().getWorld().getBlockAt(x, y, z);
                    if (block.getState() instanceof InventoryHolder) {
                        blocks.add(block);
                    }
                }
            }
        }

        inventoryListener.logCustomChestContents(claim, player.getUniqueId(), blocks);
        player.sendMessage(ChatColor.GREEN + "Logged custom items in your claim.");
        return true;
    }
}