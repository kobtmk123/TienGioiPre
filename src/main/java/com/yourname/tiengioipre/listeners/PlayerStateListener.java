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
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) { // Chỉ quan tâm di chuyển X, Z
                 // Teleport người chơi về vị trí cũ để không bị "trôi"
                 from.setPitch(to.getPitch());
                 from.setYaw(to.getYaw());
                 event.setTo(from);
            }
        }
    }
}