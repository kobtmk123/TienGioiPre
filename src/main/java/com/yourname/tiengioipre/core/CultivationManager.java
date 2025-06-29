package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

        // ĐÃ SỬA LỖI TƯƠNG THÍCH Ở ĐÂY
        // Cách tạo ArmorStand tương thích với cả Spigot và Paper
        ArmorStand as = loc.getWorld().spawn(loc.clone().add(0, -1.7, 0), ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        as.setMarker(true);

        as.addPassenger(player);
        cultivatingPlayers.put(player.getUniqueId(), as);
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            // Gỡ người chơi ra khỏi ArmorStand trước khi xóa nó
            as.getPassengers().forEach(entity -> as.removePassenger(entity));
            as.remove();
        }
    }

    public void startCultivationTask() {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (cultivatingPlayers.isEmpty()) return;

                tick++;

                // Sử dụng entrySet để duyệt an toàn hơn và có thể xóa trong lúc duyệt
                cultivatingPlayers.entrySet().removeIf(entry -> {
                    Player p = plugin.getServer().getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) {
                        ArmorStand as = entry.getValue();
                        if (as != null) as.remove();
                        return true; // Xóa khỏi map
                    }

                    // Thêm linh khí mỗi giây (20 ticks)
                    if (tick % 20 == 0) {
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        Realm realm = plugin.getRealmManager().getRealm(data.getRealmId());
                        if (data != null && realm != null && data.getLinhKhi() < realm.maxLinhKhi()) {
                            // Tăng 1 lượng tương ứng với mỗi giây
                            data.addLinhKhi(realm.linhKhiGainPerSecond());
                        }
                    }

                    // Hiệu ứng hạt mỗi 5 ticks
                    if (tick % 5 == 0) {
                         spawnParticleCircle(p.getLocation().add(0, 1, 0));
                    }
                    return false; // Giữ lại trong map
                });
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy mỗi tick
    }

    private void spawnParticleCircle(Location center) {
        Particle particle = Particle.CLOUD;
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double x = 1.2 * Math.cos(angle);
            double z = 1.2 * Math.sin(angle);
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0, z), 0, 0, 0, 0, 0);
        }
    }

    public Location getArmorStandLocation(Player player) {
        ArmorStand as = cultivatingPlayers.get(player.getUniqueId());
        return (as != null) ? as.getLocation() : null;
    }
}