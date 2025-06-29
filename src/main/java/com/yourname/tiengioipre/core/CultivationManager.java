package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CultivationManager {
    private final TienGioiPre plugin;
    private final Map<UUID, ArmorStand> cultivatingPlayers = new HashMap<>();
    private BukkitTask cultivationTask;

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
        plugin.getLogger().info("[Debug] Player " + player.getName() + " started cultivating. Map size: " + cultivatingPlayers.size());
        player.sendMessage("§aBạn đã bắt đầu trạng thái tu luyện. Nhấn SHIFT để thoát.");
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            if (!as.getPassengers().isEmpty()) as.eject();
            as.remove();
        }
        plugin.getLogger().info("[Debug] Player " + player.getName() + " stopped cultivating. Map size: " + cultivatingPlayers.size());
        player.sendMessage("§cBạn đã dừng tu luyện.");
    }

    public void startCultivationTask() {
        if (cultivationTask != null && !cultivationTask.isCancelled()) {
            cultivationTask.cancel();
        }

        cultivationTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;
                if (cultivatingPlayers.isEmpty()) return;

                // === SỬA LỖI VÒNG LẶP Ở ĐÂY ===
                // Duyệt qua một bản sao của entrySet để tránh lỗi và đảm bảo logic chạy đúng
                for (Map.Entry<UUID, ArmorStand> entry : new HashMap<>(cultivatingPlayers).entrySet()) {
                    Player p = plugin.getServer().getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) {
                        ArmorStand as = cultivatingPlayers.remove(entry.getKey());
                        if(as != null) as.remove();
                        continue;
                    }

                    if (tickCounter % 20 == 0) { // Cộng linh khí mỗi giây
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        RealmManager.TierData tier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
                        if (data != null && tier != null && data.getLinhKhi() < tier.maxLinhKhi()) {
                            double amountToAdd = tier.linhKhiGainPerSecond();
                            data.addLinhKhi(amountToAdd);
                            // Dùng logger của plugin thay vì System.out
                            plugin.getLogger().info("[Debug] Added " + amountToAdd + " Linh Khi to " + p.getName() + ". Total: " + data.getLinhKhi());
                        }
                    }

                    if (tickCounter % 10 == 0) { // Tạo hiệu ứng
                        spawnParticleCircle(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        plugin.getLogger().info("[Debug] Cultivation Task has been started!");
    }

    private void spawnParticleCircle(Player player) {
        Location center = player.getLocation().add(0, 1, 0);
        Particle particle = Particle.CLOUD;
        
        // (Optional) Đổi particle theo cảnh giới
        // ...

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