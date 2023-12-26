package me.gibson.landclaim.main.landclaimmarket;

import me.gibson.landclaim.main.landclaimmarket.commands.*;
import me.gibson.landclaim.main.landclaimmarket.listeners.InventoryListener;
import me.gibson.landclaim.main.landclaimmarket.utils.ClaimInfo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class LandClaimMarket extends JavaPlugin {
    private static Economy econ = null;

    public HashMap<Claim, ClaimInfo> claimsForSale = new HashMap<Claim, ClaimInfo>();
    public InventoryListener inventoryListener = new InventoryListener(this);

    @Override
    public void onEnable() {
        this.getCommand("sellclaim").setExecutor(new SellClaimCommand(this));
        this.getCommand("listrealestate").setExecutor(new ListClaimsCommand(this));
        this.getCommand("reloadrealestate").setExecutor(new ReloadConfigCommand(this));
        this.getCommand("logcustoms").setExecutor(new LogCustomsCommand(this, inventoryListener));
        this.getCommand("showupcoming").setExecutor(new ShowUpcomingCommand(this));
        this.getCommand("claimteleport").setExecutor(new me.gibson.landclaim.main.landclaimmarket.commands.ClaimTeleportCommand(this));
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        inventoryListener = new InventoryListener(this);
        if(!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "claims.yml");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LoadClaims();

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        saveConfig();
        SaveClaims();
        // Plugin shutdown logic
    }


    public HashMap<Claim, ClaimInfo> getClaimsForSale() {
        return claimsForSale;
    }

    public void SaveClaims() {
        File file = new File(getDataFolder(), "claims.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> claimIds = new HashSet<String>();
        for(Claim claim : claimsForSale.keySet()) {
            if(claim == null) {
                claimsForSale.remove(claim);
                SaveClaims();
                continue;
            }
            claimIds.add(claim.getID().toString());
        }
        config.set("claims", claimIds);
        for(Claim claim : claimsForSale.keySet()) {
            if(claim == null) {
                continue;
            }
            ClaimInfo claimInfo = claimsForSale.get(claim);
            if(claimInfo == null) {
                continue;
            }
            if(claimsForSale.get(claim) == null) {
                continue;
            }
            config.set("claims." + claim.getID().toString() + ".ownerUUID", claim.getOwnerID().toString());
            config.set("claims." + claim.getID().toString() + ".price", claimsForSale.get(claim).getPrice());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
            String dateAdded = claimInfo.getDateAdded().format(formatter);
            config.set("claims." + claim.getID().toString() + ".dateAdded", dateAdded);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void LoadClaims() {
        File file = new File(getDataFolder(), "claims.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection claimsSection = config.getConfigurationSection("claims");
        if (claimsSection != null) {
            Set<String> claimIds = claimsSection.getKeys(false);
            for(String claimId : claimIds) {
                Claim claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimId));
                UUID ownerUUID = UUID.fromString(config.getString("claims." + claimId + ".ownerUUID"));
                double price = config.getDouble("claims." + claimId + ".price");
                String dateAddedString = config.getString("claims." + claimId + ".dateAdded");
                LocalDateTime dateAdded;
                if (dateAddedString == null) {
                    dateAdded = LocalDateTime.now();
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
                    dateAdded = LocalDateTime.parse(dateAddedString, formatter);
                }
                claimsForSale.put(claim, new ClaimInfo(ownerUUID, price, Long.parseLong(claimId), dateAdded));            }
        }
    }


    public Economy getEconomy() {
        return econ;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
