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

        ArmorStand as = loc.getWorld().spawn(loc.clone().add(0, -1.7, 0), ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setMarker(true);
        });

        as.addPassenger(player);
        cultivatingPlayers.put(player.getUniqueId(), as);
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            as.getPassengers().forEach(as::removePassenger);
            as.remove();
        }
    }

    public void startCultivationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cultivatingPlayers.isEmpty()) return;

                for (UUID uuid : cultivatingPlayers.keySet()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p == null || !p.isOnline()) {
                        ArmorStand as = cultivatingPlayers.remove(uuid);
                        if (as != null) as.remove();
                        continue;
                    }

                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                    Realm realm = plugin.getRealmManager().getRealm(data.getRealmId());
                    if (data == null || realm == null || data.getLinhKhi() >= realm.maxLinhKhi()) {
                        continue;
                    }
                    
                    data.addLinhKhi(realm.linhKhiGainPerSecond() / 20.0);

                    spawnParticleCircle(p.getLocation().add(0, 1, 0));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy mỗi tick để hiệu ứng mượt hơn
    }

    private void spawnParticleCircle(Location center) {
        // Cần thêm logic để chọn particle dựa trên cảnh giới
        Particle particle = Particle.CLOUD;
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double x = 1.2 * Math.cos(angle);
            double z = 1.2 * Math.sin(angle);
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0, z), 0, 0, 0, 0, 0);
        }
    }

    /**
     * Lấy vị trí của ArmorStand mà người chơi đang cưỡi.
     * Cần thiết để ngăn người chơi di chuyển khi tu luyện.
     * @param player Người chơi cần kiểm tra.
     * @return Location của ArmorStand, hoặc null nếu người chơi không tu luyện.
     */
    public Location getArmorStandLocation(Player player) {
        ArmorStand as = cultivatingPlayers.get(player.getUniqueId());
        return (as != null) ? as.getLocation() : null;
    }
}