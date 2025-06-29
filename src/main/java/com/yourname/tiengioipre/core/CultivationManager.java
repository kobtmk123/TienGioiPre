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
        player.sendMessage("§cBạn đã dừng tu luyện.");
    }

    public void startCultivationTask() {
        new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                // DEBUG: Kiểm tra xem task có chạy không
                if (tickCounter % 100 == 0) { // In ra console mỗi 5 giây
                    System.out.println("[TienGioi-Debug] Cultivation Task is running. Players cultivating: " + cultivatingPlayers.size());
                }
                
                if (cultivatingPlayers.isEmpty()) {
                    tickCounter = 0; // Reset counter khi không có ai tu luyện
                    return;
                }
                tickCounter++;

                cultivatingPlayers.entrySet().removeIf(entry -> {
                    Player p = plugin.getServer().getPlayer(entry.getKey());
                    if (p == null || !p.isOnline() || !isCultivating(p)) {
                        ArmorStand as = entry.getValue();
                        if (as != null) as.remove();
                        return true;
                    }

                    // CỘNG LINH KHÍ MỖI GIÂY (20 ticks)
                    if (tickCounter % 20 == 0) {
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(p);
                        RealmManager.TierData tier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());
                        if (data != null && tier != null && data.getLinhKhi() < tier.maxLinhKhi()) {
                            double amountToAdd = tier.linhKhiGainPerSecond();
                            data.addLinhKhi(amountToAdd);
                            // DEBUG: In ra console khi cộng linh khí
                            System.out.println("[TienGioi-Debug] Added " + amountToAdd + " Linh Khi to " + p.getName());
                        }
                    }

                    // TẠO HIỆU ỨNG MỖI 10 TICKS
                    if (tickCounter % 10 == 0) {
                        spawnParticleCircle(p);
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnParticleCircle(Player player) {
        Location center = player.getLocation().add(0, 1, 0);
        Particle particle = Particle.CLOUD;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            String realmId = data.getRealmId();
            if (realmId.equals("hopthe") || realmId.equals("daithua")) {
                particle = Particle.FLAME;
            }
        }
        
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