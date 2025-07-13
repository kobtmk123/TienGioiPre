package com.yourname.tiengioipre.api;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAPIExpansion extends PlaceholderExpansion {

    private final TienGioiPre plugin;

    public PAPIExpansion(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tiengioi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.1.0"; // Cập nhật phiên bản để phản ánh tính năng mới
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline() || player.getPlayer() == null) {
            return null;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getPlayer());
        if (data == null) {
            return "Đang tải...";
        }

        RealmManager realmManager = plugin.getRealmManager();

        switch (params.toLowerCase()) {
            // === CÁC PLACEHOLDER VỀ CẢNH GIỚI ===
            case "tuvi":
                return realmManager.getRealmDisplayName(data.getRealmId());
            case "bac":
                return realmManager.getTierDisplayName(data.getRealmId(), data.getTierId());
            case "realm_name":
                String realmName = realmManager.getRealmDisplayName(data.getRealmId());
                String tierName = realmManager.getTierDisplayName(data.getRealmId(), data.getTierId());
                return realmName + " " + tierName;
            
            // === CÁC PLACEHOLDER VỀ LINH KHÍ ===
            case "linhkhi":
                return String.format("%.0f", data.getLinhKhi());
            case "linhkhi_max":
                RealmManager.TierData tierDataMax = realmManager.getTierData(data.getRealmId(), data.getTierId());
                return tierDataMax != null ? String.valueOf((int)tierDataMax.maxLinhKhi()) : "0";
            case "linhkhi_formatted":
                String current = String.format("%.0f", data.getLinhKhi());
                RealmManager.TierData tierDataFormatted = realmManager.getTierData(data.getRealmId(), data.getTierId());
                String max = tierDataFormatted != null ? String.valueOf((int)tierDataFormatted.maxLinhKhi()) : "0";
                return current + "/" + max;
            case "linhkhi_percent":
                 RealmManager.TierData tierDataPercent = realmManager.getTierData(data.getRealmId(), data.getTierId());
                 if (tierDataPercent == null || tierDataPercent.maxLinhKhi() == 0) return "0";
                 double percent = (data.getLinhKhi() / tierDataPercent.maxLinhKhi()) * 100;
                 return String.format("%.0f", Math.min(percent, 100));

            // === CÁC PLACEHOLDER VỀ CON ĐƯỜNG TU LUYỆN ===
            case "path_name":
                 String pathId = data.getCultivationPath();
                 if (pathId == null || pathId.equals("none")) {
                     return "Chưa chọn";
                 }
                 // Lấy tên hiển thị từ config
                 return plugin.getConfig().getString("paths." + pathId + ".display-name", "Không Rõ");

            // === CÁC PLACEHOLDER MỚI VỀ TÔNG MÔN ===
            case "tongmon_ten":
                String tongMonId = data.getTongMonId();
                if (tongMonId == null || tongMonId.equalsIgnoreCase("none")) {
                    return "Vô Môn Phái";
                }
                // Sử dụng hàm getTenHienThi từ TongMonManager để lấy tên đã có màu
                return plugin.getTongMonManager().getTenHienThi(tongMonId);

            case "tongmon_prefix":
                 String tongMonIdPrefix = data.getTongMonId();
                 if (tongMonIdPrefix == null || tongMonIdPrefix.equalsIgnoreCase("none")) {
                     return ""; // Trả về chuỗi rỗng nếu không có tông môn
                 }
                 return plugin.getTongMonManager().getTenHienThi(tongMonIdPrefix) + " "; // Ví dụ: "[Hắc Kiếm Môn] "
        }

        return null; // Trả về null nếu placeholder không hợp lệ
    }
}