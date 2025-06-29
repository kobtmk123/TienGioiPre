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

        Location groundLoc = player.getLocation();
        // Tìm vị trí mặt đất vững chắc bên dưới người chơi
        while (!groundLoc.getBlock().getType().isSolid() && groundLoc.getY() > player.getWorld().getMinHeight()) {
            groundLoc.subtract(0, 1, 0);
        }

        if (!groundLoc.getBlock().getType().isSolid()) {
            player.sendMessage("§cBạn cần đứng trên mặt đất vững chắc để tu luyện!");
            return;
        }

        // Vị trí an toàn để tu luyện, cách mặt đất 3 block
        Location cultivationLoc = groundLoc.clone().add(0, 3, 0);

        // --- LOGIC MỚI ĐỂ TRÁNH LÚN ĐẤT ---
        // 1. Tạo ArmorStand vô hình ngay tại vị trí của người chơi
        ArmorStand as = player.getWorld().spawn(player.getLocation(), ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false); // Rất quan trọng: không bị rơi xuống
            armorStand.setInvulnerable(true);
            armorStand.setMarker(true); // Để không có hitbox
        });

        // 2. Dịch chuyển cả người chơi và ArmorStand lên vị trí tu luyện an toàn
        player.teleport(cultivationLoc);
        as.teleport(cultivationLoc);
        
        // 3. Cho người chơi cưỡi lên sau khi đã ở vị trí an toàn
        as.addPassenger(player);

        cultivatingPlayers.put(player.getUniqueId(), as);
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            // Gỡ người chơi ra khỏi ArmorStand trước khi xóa nó
            as.getPassengers().forEach(entity -> {
                Location safeExitLoc = entity.getLocation().clone();
                // Tìm vị trí an toàn để đáp xuống
                while (!safeExitLoc.getBlock().getType().isSolid() && safeExitLoc.getY() > entity.getWorld().getMinHeight()) {
                    safeExitLoc.subtract(0, 1, 0);
                }
                entity.teleport(safeExitLoc.add(0, 1, 0)); // Teleport người chơi về mặt đất
                as.removePassenger(entity);
            });
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

                cultivatingPlayers.entrySet().removeIf(entry -> {
                    Player p = plugin.getServer().getPlayer(entry.getKey());
                    if (p == null || !p.isOnline() || !p.getVehicle().equals(entry.getValue())) {
                        ArmorStand as = entry.getValue();
                        if (as != null) as.remove();
                        return true;
                    }
                    
                    if (tick % 20 == 0) { // Mỗi giây
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        RealmManager.TierData tier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
                        if (data != null && tier != null && data.getLinhKhi() < tier.maxLinhKhi()) {
                            data.addLinhKhi(tier.linhKhiGainPerSecond());
                        }
                    }

                    if (tick % 5 == 0) { // Hiệu ứng hạt
                        // Hiệu ứng lửa cho 3 Tu Vi cao nhất
                        String realmId = plugin.getPlayerDataManager().getPlayerData(p).getRealmId();
                        boolean isHighRealm = realmId.equals("hopthe") || realmId.equals("daithua") || realmId.equals("dokiep");
                        Particle particle = isHighRealm ? Particle.FLAME : Particle.CLOUD;
                        spawnParticleCircle(p.getLocation().add(0, 1, 0), particle);
                    }
                    return false;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L); // Chạy bất đồng bộ để giảm lag
    }

    private void spawnParticleCircle(Location center, Particle particle) {
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