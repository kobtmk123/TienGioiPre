package com.yourname.tiengioipre.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String realmId; // Tu Vi
    private String tierId;  // Bậc
    private double linhKhi;

    public PlayerData(UUID uuid, String realmId, String tierId, double linhKhi) {
        this.uuid = uuid;
        this.realmId = realmId;
        this.tierId = tierId;
        this.linhKhi = linhKhi;
    }

    // ... các getter/setter cho realmId và linhKhi ...

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }
    public double getLinhKhi() { return linhKhi; }
    public void setLinhKhi(double linhKhi) { this.linhKhi = linhKhi; }
    public void addLinhKhi(double amount) { this.linhKhi += amount; }
    public UUID getUuid() { return uuid; }
}