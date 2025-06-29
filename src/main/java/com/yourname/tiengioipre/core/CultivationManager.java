package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask; // Import BukkitTask

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CultivationManager {
    private final TienGioiPre plugin;
    private final Map<UUID, ArmorStand> cultivatingPlayers = new HashMap<>();
    private BukkitTask cultivationTask; // Lưu trữ task để kiểm tra

    public CultivationManager(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    public boolean isCultivating(Player player) {
        return cultivatingPlayers.containsKey(player.getUniqueId());
    }

    public void startCultivating(Player player) {
        if (isCultivating(player)) {
            player.sendMessage("§cBạn đã đang trong trạng thái tu luyện rồi.");
            return;
        }

        Location loc = player.getLocation();
        if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            player.sendMessage("§cBạn cần đứng trên mặt đất vững chắc để tu luyện!");
            return;
        }

        ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        
        as.addPassenger(player);
        cultivatingPlayers.put(player.getUniqueId(), as);
        
        System.out.println("[TienGioi-Debug] Player " + player.getName() + " started cultivating. Map size: " + cultivatingPlayers.size());
        player.sendMessage("§aBạn đã bắt đầu trạng thái tu luyện. Nhấn SHIFT để thoát.");
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            if (!as.getPassengers().isEmpty()) {
                as.eject();
            }
            as.remove();
        }
        System.out.println("[TienGioi-Debug] Player " + player.getName() + " stopped cultivating. Map size: " + cultivatingPlayers.size());
        player.sendMessage("§cBạn đã dừng tu luyện.");
    }

    public void startCultivationTask() {
        // Hủy task cũ nếu có để tránh chạy nhiều task cùng lúc
        if (cultivationTask != null && !cultivationTask.isCancelled()) {
            cultivationTask.cancel();
        }

        cultivationTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                // DEBUG: In ra console mỗi 5 giây để xác nhận task đang chạy
                if (tickCounter % 100 == 0) {
                    System.out.println("[TienGioi-Debug] Cultivation Task is running... Players in map: " + cultivatingPlayers.size());
                }
                
                if (cultivatingPlayers.isEmpty()) {
                    tickCounter = 0;
                    return;
                }
                tickCounter++;

                // Sử dụng new HashMap để tránh ConcurrentModificationException khi duyệt và xóa
                for (UUID uuid : new HashMap<>(cultivatingPlayers).keySet()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p == null || !p.isOnline()) {
                        // Xóa người chơi đã thoát
                        ArmorStand as = cultivatingPlayers.remove(uuid);
                        if(as != null) as.remove();
                        continue;
                    }

                    // CỘNG LINH KHÍ MỖI GIÂY (20 ticks)
                    if (tickCounter % 20 == 0) {
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        RealmManager.TierData tier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
                        if (data != null && tier != null && data.getLinhKhi() < tier.maxLinhKhi()) {
                            double amountToAdd = tier.linhKhiGainPerSecond();
                            data.addLinhKhi(amountToAdd);
                            System.out.println("[TienGioi-Debug] Added " + amountToAdd + " Linh Khi to " + p.getName() + ". Total: " + data.getLinhKhi());
                        }
                    }

                    // TẠO HIỆU ỨNG MỖI 10 TICKS
                    if (tickCounter % 10 == 0) {
                        spawnParticleCircle(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        System.out.println("[TienGioi-Debug] Cultivation Task has been started!");
    }

    private void spawnParticleCircle(Player player) {
        Location center = player.getLocation().add(0, 1, 0);
        Particle particle = Particle.CLOUD;
        
        for (int i = 0; i < 360; i += 30) {
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