package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerDamageListener implements Listener {

    private final TienGioiPre plugin;

    public PlayerDamageListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(damager);
        if (data == null || data.getCultivationPath() == null || data.getCultivationPath().equals("none")) {
            return;
        }

        ConfigurationSection pathSection = plugin.getConfig().getConfigurationSection("paths." + data.getCultivationPath() + ".stats");
        if (pathSection == null) return;

        ItemStack itemInHand = damager.getInventory().getItemInMainHand();
        double bonusDamage = 0;

        if (itemInHand == null || itemInHand.getType().isAir()) {
            // Sát thương tay không
            bonusDamage = pathSection.getDouble("hand-damage-bonus", 0.0);
        } else {
            // Sát thương vũ khí
            bonusDamage = pathSection.getDouble("weapon-damage-bonus", 0.0);
        }
        
        // Xử lý modifier (giảm sát thương)
        bonusDamage += pathSection.getDouble("weapon-damage-modifier", 0.0);

        if (bonusDamage != 0) {
            event.setDamage(event.getDamage() + bonusDamage);
        }
    }
}