package com.yourname.tiengioipre.api;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.entity.Player;

public class TienGioiAPI {

    private final TienGioiPre plugin;

    public TienGioiAPI(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    public String getPlayerRealmName(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return "Không rõ";
        return plugin.getRealmManager().getRealmDisplayName(data.getRealmId());
    }

    public String getPlayerRealmId(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return (data != null) ? data.getRealmId() : null;
    }

    public String getPlayerTierId(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return (data != null) ? data.getTierId() : null;
    }
    
    public double getPlayerLinhKhi(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return (data != null) ? data.getLinhKhi() : -1;
    }

    /**
     * Lấy số linh khí tối đa cho cảnh giới hiện tại của người chơi.
     * @param player Người chơi cần kiểm tra.
     * @return Số linh khí tối đa, hoặc -1 nếu không tìm thấy người chơi.
     */
    public double getPlayerMaxLinhKhi(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return -1;
        }
        
        // SỬA LỖI Ở ĐÂY: Dùng phương thức mới để lấy thông tin từ TierData
        RealmManager.TierData tierData = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
        
        return (tierData != null) ? tierData.maxLinhKhi() : -1;
    }

    public void addLinhKhi(Player player, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.addLinhKhi(amount);
        }
    }

    public void setLinhKhi(Player player, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.setLinhKhi(amount);
        }
    }
}