package com.myname.tiengioipre.listeners;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TongMonListener implements Listener {
    private final TienGioiPre plugin;

    public TongMonListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        Player damaged = (Player) event.getEntity();

        PlayerData damagerData = plugin.getPlayerDataManager().getPlayerData(damager);
        PlayerData damagedData = plugin.getPlayerDataManager().getPlayerData(damaged);

        if (damagerData == null || damagedData == null) return;

        String damagerTongMon = damagerData.getTongMonId();
        String damagedTongMon = damagedData.getTongMonId();

        if (damagerTongMon != null && !damagerTongMon.equals("none") && damagerTongMon.equals(damagedTongMon)) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "Không thể tấn công đồng môn!");
        }
    }
}