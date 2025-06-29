package com.yourname.tiengioipre.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String realmId;
    private double linhKhi;

    public PlayerData(UUID uuid, String realmId, double linhKhi) {
        this.uuid = uuid;
        this.realmId = realmId;
        this.linhKhi = linhKhi;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public double getLinhKhi() {
        return linhKhi;
    }

    public void setLinhKhi(double linhKhi) {
        this.linhKhi = linhKhi;
    }

    public void addLinhKhi(double amount) {
        this.linhKhi += amount;
    }
}