package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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
        
        Location loc = player.getLocation().add(0, 2, 0);
        ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.addPassenger(player);
        });
        
        cultivatingPlayers.put(player.getUniqueId(), as);
        player.sendMessage("Bạn đã bắt đầu tu luyện.");
    }

    public void stopCultivating(Player player) {
        ArmorStand as = cultivatingPlayers.remove(player.getUniqueId());
        if (as != null) {
            as.remove();
        }
        player.sendMessage("Bạn đã dừng tu luyện.");
    }
    
    public void startCultivationTask() {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                for (UUID uuid : cultivatingPlayers.keySet()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p == null || !p.isOnline()) {
                        cultivatingPlayers.remove(uuid);
                        continue;
                    }
                    
                    // Thêm linh khí (cần thêm logic từ RealmManager)
                    // ...
                    
                    // Hiệu ứng hạt
                    if(tick % 5 == 0) { // Giảm tần suất để đỡ lag
                        spawnParticleCircle(p.getLocation());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Chạy mỗi giây (20 ticks)
    }

    private void spawnParticleCircle(Location center) {
        // Đây là ví dụ cho hiệu ứng khói trắng
        // Bạn sẽ cần thêm logic để chọn particle dựa trên cảnh giới
        Particle particle = Particle.CLOUD; 
        
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = 1.5 * Math.cos(angle);
            double z = 1.5 * Math.sin(angle);
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
        }
    }
}