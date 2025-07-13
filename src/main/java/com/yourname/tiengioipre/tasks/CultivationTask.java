package com.myname.tiengioipre.tasks;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.core.CultivationManager;
import com.myname.tiengioipre.core.PlayerDataManager;
import com.myname.tiengioipre.core.RealmManager;
import com.myname.tiengioipre.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task này chịu trách nhiệm xử lý logic lặp lại cho việc tu luyện.
 * Nó chạy mỗi giây một lần để cộng linh khí và tạo hiệu ứng cho người chơi.
 */
public class CultivationTask extends BukkitRunnable {

    private final TienGioiPre plugin;
    private final CultivationManager cultivationManager;
    private final PlayerDataManager playerDataManager;
    private final RealmManager realmManager;

    public CultivationTask(TienGioiPre plugin) {
        this.plugin = plugin;
        // Lấy các manager cần thiết từ class chính để sử dụng
        this.cultivationManager = plugin.getCultivationManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.realmManager = plugin.getRealmManager();
    }

    @Override
    public void run() {
        // Lấy danh sách những người chơi đang online và có trong danh sách tu luyện
        for (Player player : cultivationManager.getOnlineCultivatingPlayers()) {
            
            // Lấy dữ liệu của người chơi từ các manager
            PlayerData data = playerDataManager.getPlayerData(player);
            if (data == null) {
                continue;
            }

            RealmManager.TierData tier = realmManager.getTierData(data.getRealmId(), data.getTierId());
            if (tier == null) {
                continue;
            }

            // 1. Logic Cộng Linh Khí
            // Chỉ cộng nếu linh khí hiện tại chưa đạt mức tối đa
            if (data.getLinhKhi() < tier.maxLinhKhi()) {
                double amountToAdd = tier.linhKhiGainPerSecond();
                data.addLinhKhi(amountToAdd);
            }

            // 2. Logic Tạo Hiệu Ứng
            // Gọi hàm tạo hiệu ứng từ CultivationManager
            cultivationManager.spawnParticleCircle(player);
        }
    }
}