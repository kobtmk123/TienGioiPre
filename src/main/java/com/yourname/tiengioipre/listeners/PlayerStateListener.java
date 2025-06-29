package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerStateListener implements Listener {

    private final TienGioiPre plugin;

    public PlayerStateListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // Kiểm tra isSneaking() để đảm bảo người chơi đang bắt đầu ngồi xuống (SHIFT)
        if (event.isSneaking() && plugin.getCultivationManager().isCultivating(player)) {
            plugin.getCultivationManager().stopCultivating(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getCultivationManager().isCultivating(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            // Chỉ hủy nếu người chơi di chuyển vị trí (không phải chỉ xoay đầu)
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                 player.sendMessage("§cBạn không thể di chuyển khi đang tu luyện!");
                 // Dịch chuyển người chơi về vị trí cũ để tránh bị đẩy đi
                 Location standLocation = plugin.getCultivationManager().getArmorStandLocation(player);
                 if(standLocation != null){
                     player.teleport(standLocation.clone().add(0, -2, 0)); // Teleport về mặt đất dưới armor stand
                 }
            }
        }
    }
}