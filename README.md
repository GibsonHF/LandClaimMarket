# **LandClaimMarket Plugin for Minecraft**

## **Overview**
**The LandClaimMarket** is a Java-based Minecraft plugin designed to integrate with the **GriefPrevention plugin**. It enables players to sell expired land claims, adding a unique dimension to the Minecraft experience.

## **Features**

### **Claim Expiration Handling**
- **Automatic Expiration Cancellation**: Cancels the expiration event of land claims.
- **Ownership Transfer**: Changes ownership of expired claims to a system account.
- **Price Calculation**: Calculates the price based on the claim's area.
- **Sales Listing**: Lists expired claims for sale.

### **Inventory Management**
- **User Interface**: Provides an inventory interface for viewing and purchasing claims.
- **Claim Information**: Displays details like price, owner, and size.
- **Purchase Confirmation**: Enables players to confirm their purchases.
- **Teleport Feature**: Allows teleportation to the claim.

### **Claim Logging**
- **Record Keeping**: Logs details such as price and the previous owner's UUID for auditing and history.

## **Usage**

1. **Prerequisites**: Requires a Minecraft server with Spigot or Bukkit API and the GriefPrevention plugin.
2. **Installation**: Place the LandClaimMarket plugin's JAR file in your server's plugins directory.
3. **Functionality**: Automates claim expiration handling and provides an inventory interface on server startup.

## **Development**

- **Language**: Java
- **API**: Spigot API
- **Event Handling**: Uses Bukkit event system for claim expiration.
- **Inventory API**: Employs Bukkit inventory API for interface creation.
- **Main Class**: `InventoryListener`, implementing the Listener interface.
- **Data Storage**: `ClaimInfo` class for claim data.

## **Contributing**

Contributions are what make the LandClaimMarket thrive. Open an issue for suggestions or bugs, and contribute code via pull requests.

## **License**

LandClaimMarket is open-source under the MIT License.
