package com.yourname.tiengioipre.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String realmId;
    private String tierId;
    private double linhKhi;
    private String cultivationPath; // <-- TRƯỜNG MỚI

    public PlayerData(UUID uuid, String realmId, String tierId, double linhKhi, String cultivationPath) {
        this.uuid = uuid;
        this.realmId = realmId;
        this.tierId = tierId;
        this.linhKhi = linhKhi;
        this.cultivationPath = cultivationPath;
    }
    
    // ... các getter/setter khác ...
    
    public String getCultivationPath() {
        return cultivationPath;
    }

    public void setCultivationPath(String cultivationPath) {
        this.cultivationPath = cultivationPath;
    }
    
    // Getter/setter cũ
    public UUID getUuid() { return uuid; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }
    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }
    public double getLinhKhi() { return linhKhi; }
    public void setLinhKhi(double linhKhi) { this.linhKhi = linhKhi; }
    public void addLinhKhi(double amount) { this.linhKhi += amount; }
}