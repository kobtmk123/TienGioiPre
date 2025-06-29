package com.yourname.tiengioipre.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String realmId;
    private String tierId;
    private double linhKhi;

    // HÀM KHỞI TẠO VỚI 4 THAM SỐ
    public PlayerData(UUID uuid, String realmId, String tierId, double linhKhi) {
        this.uuid = uuid;
        this.realmId = realmId;
        this.tierId = tierId;
        this.linhKhi = linhKhi;
    }

    public UUID getUuid() { return uuid; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }
    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }
    public double getLinhKhi() { return linhKhi; }
    public void setLinhKhi(double linhKhi) { this.linhKhi = linhKhi; }
    public void addLinhKhi(double amount) { this.linhKhi += amount; }
}