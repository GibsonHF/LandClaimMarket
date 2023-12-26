package me.gibson.landclaim.main.landclaimmarket.utils;

import java.util.UUID;

public class ClaimInfo {
    private final UUID player;
    private final double price;
    private final long claimId; // Unique identifier for the claim


    public ClaimInfo(UUID player, double price, long claimId) {
        this.player = player;
        this.price = price;
        this.claimId = claimId;
    }

    public UUID getUUID() {
        return player;
    }

    public double getPrice() {
        return price;
    }

    public long getClaimId() {
        return claimId;
    }
}
