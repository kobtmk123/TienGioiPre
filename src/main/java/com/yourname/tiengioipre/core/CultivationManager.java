package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CultivationManager {
    private final TienGioiPre plugin;
    private final Map<UUID, ArmorStand> cultivatingPlayers = new HashMap<>();

    public CultivationManager(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    public boolean isCultivating(Player player) {
        return cultivatingPlayers.containsKey(player.getUniqueId());
    }

    public void startCultivating(Player player) {
        if (isCultivating(player)) return;

        Location loc = player.getLocation();
        if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            player.sendMessage("§cBạn cần đứng trên mặt đất vững chắc để tu luyện!");
            return;
        }

        // SỬA LỖI LÚN XUỐNG ĐẤT
        // Tạo ArmorStand ngay tại vị trí của người chơi
        ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        // Không setMarker(true) để có thể đặt người chơi lên
        
        // Đặt người chơi làm hành khách
        as.addPassenger(player);
        cultivatingPlayers.put(player.getUniqueId(), as);
        
        player.sendMessage("§aBạn đã bắt đầu trạng thái tu luyện. Nhấn SHIFT để thoát.");
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            // Đảm bảo người chơi không bị kẹt
            if (!as.getPassengers().isEmpty()) {
                as.eject();
            }
            as.remove();
        }
        player.sendMessage("§cBạn đã dừng tu luyện.");
    }

    public void startCultivationTask() {
        // SỬA LỖI KHÔNG NHẬN LINH KHÍ
        // Chạy task trên luồng chính của server để đảm bảo an toàn khi thay đổi dữ liệu
        new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                if (cultivatingPlayers.isEmpty()) return;
                tickCounter++;

                cultivatingPlayers.entrySet().removeIf(entry -> {
                    Player p = plugin.getServer().getPlayer(entry.getKey());
                    if (p == null || !p.isOnline() || !isCultivating(p)) {
                        ArmorStand as = entry.getValue();
                        if (as != null) as.remove();
                        return true; // Xóa khỏi map
                    }

                    // CỘNG LINH KHÍ MỖI GIÂY (20 ticks)
                    if (tickCounter % 20 == 0) {
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        RealmManager.TierData tier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
                        if (data != null && tier != null && data.getLinhKhi() < tier.maxLinhKhi()) {
                            data.addLinhKhi(tier.linhKhiGainPerSecond());
                        }
                    }

                    // TẠO HIỆU ỨNG MỖI 10 TICKS (Mỏng hơn)
                    if (tickCounter % 10 == 0) {
                        spawnParticleCircle(p);
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // SỬA LẠI HIỆU ỨNG HẠT
    private void spawnParticleCircle(Player player) {
        Location center = player.getLocation().add(0, 1, 0); // Vòng tròn ở ngang ngực
        Particle particle = Particle.CLOUD; // Mặc định là khói trắng

        // Tùy chọn: Đổi particle cho cảnh giới cao
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            String realmId = data.getRealmId();
            if (realmId.equals("hopthe") || realmId.equals("daithua")) {
                particle = Particle.FLAME; // Đổi thành lửa
            }
        }
        
        // Vòng tròn mỏng hơn, bán kính 1 block
        for (int i = 0; i < 360; i += 30) { // Tăng bước nhảy để hạt thưa hơn
            double angle = Math.toRadians(i);
            double x = 1.0 * Math.cos(angle);
            double z = 1.0 * Math.sin(angle);
            player.getWorld().spawnParticle(particle, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }

    public Location getArmorStandLocation(Player player) {
        ArmorStand as = cultivatingPlayers.get(player.getUniqueId());
        return (as != null) ? as.getLocation() : null;
    }
}