package com.myname.tiengioipre.core;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Quản lý trạng thái tu luyện của người chơi.
 * Nhiệm vụ chính là thêm/xóa người chơi khỏi danh sách tu luyện và cung cấp các hàm hỗ trợ.
 * Logic lặp lại đã được chuyển sang CultivationTask.
 */
public class CultivationManager {
    private final TienGioiPre plugin;
    private final Map<UUID, ArmorStand> cultivatingPlayers = new HashMap<>();

    public CultivationManager(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    /**
     * Kiểm tra xem một người chơi có đang trong trạng thái tu luyện không.
     * @param player Người chơi cần kiểm tra.
     * @return true nếu đang tu luyện, ngược lại false.
     */
    public boolean isCultivating(Player player) {
        return cultivatingPlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * Lấy danh sách những người chơi đang tu luyện và đang online.
     * Dùng cho CultivationTask.
     * @return Một danh sách các đối tượng Player.
     */
    public List<Player> getOnlineCultivatingPlayers() {
        return cultivatingPlayers.keySet().stream()
               .map(plugin.getServer()::getPlayer)
               .filter(Objects::nonNull)
               .collect(Collectors.toList());
    }

    /**
     * Bắt đầu trạng thái tu luyện cho một người chơi.
     * @param player Người chơi bắt đầu tu luyện.
     */
    public void startCultivating(Player player) {
        if (isCultivating(player)) {
            player.sendMessage("§cBạn đã đang trong trạng thái tu luyện rồi.");
            return;
        }
        Location loc = player.getLocation();
        // Kiểm tra xem có khối rắn bên dưới không
        if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            player.sendMessage("§cBạn cần đứng trên mặt đất vững chắc để tu luyện!");
            return;
        }
        
        // Tạo một ArmorStand vô hình tại vị trí của người chơi
        ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        
        // Đặt người chơi lên ArmorStand để tạo hiệu ứng ngồi lơ lửng
        as.addPassenger(player);
        
        // Thêm người chơi vào danh sách đang tu luyện
        cultivatingPlayers.put(player.getUniqueId(), as);
        
        player.sendMessage("§aBạn đã bắt đầu trạng thái tu luyện. Nhấn SHIFT để thoát.");
    }

    /**
     * Dừng trạng thái tu luyện cho một người chơi.
     * @param player Người chơi dừng tu luyện.
     */
    public void stopCultivating(Player player) {
        // Lấy và xóa ArmorStand khỏi map
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            // Đảm bảo người chơi được đưa ra khỏi ArmorStand trước khi xóa
            if (!as.getPassengers().isEmpty()) {
                as.eject();
            }
            as.remove();
        }
        player.sendMessage("§cBạn đã dừng tu luyện.");
    }
    
    /**
     * Tạo hiệu ứng hạt xoay tròn quanh người chơi.
     * Hàm này được gọi từ CultivationTask.
     * @param player Người chơi cần tạo hiệu ứng.
     */
    public void spawnParticleCircle(Player player) {
        Location center = player.getLocation().add(0, 1, 0); // Vòng tròn ở ngang ngực
        Particle particle = Particle.CLOUD; // Mặc định là khói trắng

        // Tùy chọn: Thay đổi loại hạt dựa trên cảnh giới hoặc con đường tu luyện
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            String realmId = data.getRealmId();
            if (realmId.equals("hopthe") || realmId.equals("daithua")) {
                particle = Particle.FLAME; // Đổi thành lửa cho cảnh giới cao
            }
        }
        
        // Vòng tròn mỏng, bán kính 1 block, 12 hạt
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double x = 1.0 * Math.cos(angle);
            double z = 1.0 * Math.sin(angle);
            player.getWorld().spawnParticle(particle, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }
}