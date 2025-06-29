package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final TienGioiPre plugin;

    public PlayerConnectionListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().loadPlayerData(player);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            plugin.getRealmManager().applyRealmStats(player, data.getRealmId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Dừng tu luyện nếu người chơi thoát
        if (plugin.getCultivationManager().isCultivating(player)) {
            plugin.getCultivationManager().stopCultivating(player);
        }
        
        plugin.getRealmManager().removeRealmStats(player);
        plugin.getPlayerDataManager().unloadPlayerData(player);
    }
}