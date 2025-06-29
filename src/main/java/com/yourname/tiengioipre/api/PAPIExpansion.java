package com.yourname.tiengioipre.api;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PAPIExpansion extends PlaceholderExpansion {

    private final TienGioiPre plugin;

    public PAPIExpansion(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tiengioi"; // Sẽ dùng là %tiengioi_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true; // Giữ PAPIExpansion được load
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getPlayer());
        if (data == null) {
            return "Đang tải...";
        }

        switch (params.toLowerCase()) {
            case "realm_name":
                // Cần một hàm trong RealmManager để lấy tên hiển thị từ ID
                return plugin.getRealmManager().getRealmDisplayName(data.getRealmId());
            case "linhkhi":
                return String.format("%.0f", data.getLinhKhi());
            case "linhkhi_max":
                // Cần một hàm trong RealmManager để lấy linh khí tối đa từ ID
                return String.valueOf(plugin.getRealmManager().getMaxLinhKhi(data.getRealmId()));
            case "linhkhi_formatted":
                String current = String.format("%.0f", data.getLinhKhi());
                String max = String.valueOf(plugin.getRealmManager().getMaxLinhKhi(data.getRealmId()));
                return current + "/" + max;
            // TODO: Thêm 76 placeholder khác ở đây
            // Ví dụ:
            case "realm_next_name":
            case "stat_bonus_health":
            case "stat_bonus_damage":
            // ...
        }

        return null; // Placeholder không hợp lệ
    }
}