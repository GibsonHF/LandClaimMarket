package me.gibson.landclaim.main.landclaimmarket.utils;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClaimInfo {
    private final UUID player;
    private final double price;
    private final long claimId; // Unique identifier for the claim

    private LocalDateTime dateAdded;



    public ClaimInfo(UUID player, double price, long claimId, LocalDateTime dateAdded) {
        this.player = player;
        this.price = price;
        this.claimId = claimId;
        this.dateAdded = dateAdded;

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

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }
}
