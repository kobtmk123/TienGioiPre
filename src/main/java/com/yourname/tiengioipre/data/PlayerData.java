package com.yourname.tiengioipre.data;

import java.util.UUID;

/**
 * Class này là một đối tượng dữ liệu đơn giản (POJO - Plain Old Java Object)
 * để lưu trữ tất cả thông tin của một người chơi trong bộ nhớ (RAM).
 */
public class PlayerData {

    private final UUID uuid;
    private String realmId;         // ID của Tu Vi, ví dụ: "luyenkhi"
    private String tierId;          // ID của Bậc, ví dụ: "soky"
    private double linhKhi;         // Số linh khí hiện tại
    private String cultivationPath; // ID của Con Đường Tu Luyện, ví dụ: "kiemtu"
    private String tongMonId;       // ID của Tông Môn, ví dụ: "hac-kiem-mon"

    /**
     * Hàm khởi tạo (Constructor) để tạo một đối tượng dữ liệu người chơi mới.
     * @param uuid UUID của người chơi
     * @param realmId ID Tu Vi
     * @param tierId ID Bậc
     * @param linhKhi Lượng linh khí
     * @param cultivationPath ID Con Đường Tu Luyện
     * @param tongMonId ID Tông Môn
     */
    public PlayerData(UUID uuid, String realmId, String tierId, double linhKhi, String cultivationPath, String tongMonId) {
        this.uuid = uuid;
        this.realmId = realmId;
        this.tierId = tierId;
        this.linhKhi = linhKhi;
        this.cultivationPath = cultivationPath;
        this.tongMonId = tongMonId;
    }

    // --- GETTERS VÀ SETTERS ---
    // Các hàm này cho phép các phần khác của plugin đọc và thay đổi dữ liệu một cách an toàn.

    public UUID getUuid() {
        return uuid;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
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

    public String getCultivationPath() {
        return cultivationPath;
    }

    public void setCultivationPath(String cultivationPath) {
        this.cultivationPath = cultivationPath;
    }

    public String getTongMonId() {
        return tongMonId;
    }

    public void setTongMonId(String tongMonId) {
        this.tongMonId = tongMonId;
    }
}