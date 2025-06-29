package com.yourname.tiengioipre.api;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
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
        // Đây là "tên" của nhóm placeholder. Sẽ dùng là %tiengioi_...%
        return "tiengioi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0.0"; // Cập nhật phiên bản để phản ánh thay đổi lớn
    }

    @Override
    public boolean persist() {
        return true; // Giữ PAPIExpansion được load
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return null;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getPlayer());
        if (data == null) {
            return "Đang tải...";
        }

        RealmManager realmManager = plugin.getRealmManager();

        switch (params.toLowerCase()) {
            // === CÁC PLACEHOLDER MỚI THEO YÊU CẦU ===

            case "tuvi":
                // Trả về tên của Tu Vi. Ví dụ: "Luyện Khí"
                // Cách dùng: %tiengioi_tuvi%
                return realmManager.getRealmDisplayName(data.getRealmId());

            case "bac":
                // Trả về tên của Bậc. Ví dụ: "Sơ Kỳ"
                // Cách dùng: %tiengioi_bac%
                return realmManager.getTierDisplayName(data.getRealmId(), data.getTierId());

            // === CÁC PLACEHOLDER CŨ ĐƯỢC CẬP NHẬT CHO HỆ THỐNG MỚI ===

            case "realm_name":
                // Trả về tên đầy đủ. Ví dụ: "Luyện Khí Sơ Kỳ"
                // Cách dùng: %tiengioi_realm_name%
                String realmName = realmManager.getRealmDisplayName(data.getRealmId());
                String tierName = realmManager.getTierDisplayName(data.getRealmId(), data.getTierId());
                return realmName + " " + tierName;
                
            case "linhkhi":
                // Trả về số linh khí hiện tại. Ví dụ: "150"
                // Cách dùng: %tiengioi_linhkhi%
                return String.format("%.0f", data.getLinhKhi());

            case "linhkhi_max":
                // Trả về số linh khí tối đa của bậc hiện tại. Ví dụ: "200"
                // Cách dùng: %tiengioi_linhkhi_max%
                RealmManager.TierData tierDataMax = realmManager.getTierData(data.getRealmId(), data.getTierId());
                return tierDataMax != null ? String.valueOf((int)tierDataMax.maxLinhKhi()) : "0";

            case "linhkhi_formatted":
                // Trả về định dạng "hiện tại/tối đa". Ví dụ: "150/200"
                // Cách dùng: %tiengioi_linhkhi_formatted%
                String current = String.format("%.0f", data.getLinhKhi());
                RealmManager.TierData tierDataFormatted = realmManager.getTierData(data.getRealmId(), data.getTierId());
                String max = tierDataFormatted != null ? String.valueOf((int)tierDataFormatted.maxLinhKhi()) : "0";
                return current + "/" + max;
            
            case "linhkhi_percent":
                // Trả về tỷ lệ phần trăm linh khí. Ví dụ: "75"
                // Cách dùng: %tiengioi_linhkhi_percent%
                 RealmManager.TierData tierDataPercent = realmManager.getTierData(data.getRealmId(), data.getTierId());
                 if (tierDataPercent == null || tierDataPercent.maxLinhKhi() == 0) return "0";
                 double percent = (data.getLinhKhi() / tierDataPercent.maxLinhKhi()) * 100;
                 return String.format("%.0f", Math.min(percent, 100)); // Đảm bảo không vượt quá 100%

            // Bạn có thể thêm các placeholder khác về stats ở đây nếu cần
            // Ví dụ: %tiengioi_stat_health%, %tiengioi_stat_damage%,...
        }

        return null; // Trả về null nếu placeholder không hợp lệ
    }
}