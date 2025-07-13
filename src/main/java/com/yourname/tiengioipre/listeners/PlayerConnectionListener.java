package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final TienGioiPre plugin;

    public PlayerConnectionListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Phương thức loadPlayerData đã bao gồm cả việc áp dụng stats,
        // vì vậy chúng ta chỉ cần gọi một dòng này.
        plugin.getPlayerDataManager().loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Dừng tu luyện nếu người chơi thoát giữa chừng
        if (plugin.getCultivationManager().isCultivating(player)) {
            plugin.getCultivationManager().stopCultivating(player);
        }
        // Xóa các hiệu ứng stats để tránh lỗi
        plugin.getRealmManager().removeRealmStats(player);
        // Lưu và dọn dẹp dữ liệu của người chơi
        plugin.getPlayerDataManager().unloadPlayerData(player);
    }
}