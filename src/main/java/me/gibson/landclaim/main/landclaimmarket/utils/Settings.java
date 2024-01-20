package me.gibson.landclaim.main.landclaimmarket.utils;

import me.gibson.landclaim.main.landclaimmarket.LandClaimMarket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class Settings {
    private LandClaimMarket plugin;

    public Settings(LandClaimMarket plugin) {
        this.plugin = plugin;
    }

    public void createDefaultConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.isConfigurationSection("settings")) {
            config.createSection("settings");
            config.set("settings.claimPrice", 750);
            config.set("settings.systemID", "Hack_the_Gibson");
        }

        if (!config.isConfigurationSection("messages")) {
            config.createSection("messages");
            config.set("messages.claimSold", "&aYou have sold your claim for &e%price%&a.");
            config.set("messages.claimNotForSale", "&cThis claim is not for sale.");
            config.set("messages.claimNotOwned", "&cYou do not own this claim.");
            config.set("messages.claimAlreadyForSale", "&cThis claim is already for sale.");
            config.set("messages.invalidItem", "&cInvalid item.");
            config.set("messages.noClaimsForSale", "&cNo claims for sale.");
            config.set("messages.claimNoLongerExists", "&cThis claim no longer exists.");
            config.set("messages.claimNoLongerAvailable", "&cThis claim is no longer available.");
            config.set("messages.claimNotForSale", "&cThis claim is not for sale.");
            config.set("messages.claimPurchased", "&aYou have purchased this claim for &e%price%&a.");
            config.set("messages.claimTeleporting", "&aTeleporting to claim in 5 seconds.");
            config.set("messages.claimTeleported", "&aYou have teleported to this claim.");
            config.set("messages.claimUnlisted", "&aYou have unlisted this claim.");
            config.set("messages.claimLogged", "&aClaim has been logged.");
            config.set("messages.claimDoesNotExist", "&cThis claim does not exist.");
            config.set("messages.teleportingToClaim", "&aTeleporting to claim in 5 seconds...");
            config.set("messages.teleportedToClaim", "&aYou have been teleported to the claim.");
            config.set("messages.cannotUnlistNotOwned", "&cYou cannot unlist a claim that you do not own.");
            config.set("messages.noPermissionToLogClaims", "&cYou do not have permission to log claims.");
            config.set("messages.invalidClaimItem", "&cThis item does not represent a valid claim.");
            config.set("messages.cannotPurchaseOwnClaim", "&cYou cannot purchase your own claim.");
            config.set("messages.notEnoughMoney", "&cYou do not have enough money to purchase this claim. You have &e%current_balance%&c, but the claim costs &e%claim_price%&c.");
        }

        if(!config.isConfigurationSection("booleans")) {
            config.createSection("booleans");
            config.set("booleans.enableClaimExpiration", true);
            config.set("booleans.requireMoney", true);
        }

        plugin.saveConfig();
    }

    public void sendPlayerMessage(Player player, String messageKey) {
        String message = plugin.getConfig().getString("messages." + messageKey, messageKey);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendPlayerMessage(Player player, String messageKey, double price) {
        String message = plugin.getConfig().getString("messages." + messageKey, messageKey);
        message = message.replace("%price%", String.valueOf(price));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendPlayerMessage(Player player, String messageKey, HashMap<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + messageKey, messageKey);
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace("%" + placeholder.getKey() + "%", placeholder.getValue());
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public String getFormattedMessage(String key, HashMap<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + key, key);
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace("%" + placeholder.getKey() + "%", placeholder.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    private HashMap<String, String> messages = new HashMap<>();

    public void loadLocale(String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            bundle.keySet().forEach(key -> messages.put(key, bundle.getString(key)));
        } catch (MissingResourceException e) {
            Bukkit.getLogger().warning("Could not find resource bundle for locale: " + languageTag);
        }
    }
}