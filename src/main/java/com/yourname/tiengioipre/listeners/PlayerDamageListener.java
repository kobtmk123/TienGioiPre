package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerDamageListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();

    public PlayerDamageListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    // Xử lý sát thương GÂY RA bởi người chơi
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDealsDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        ItemStack weapon = damager.getInventory().getItemInMainHand();
        double finalDamage = event.getDamage();

        // 1. Áp dụng bonus từ Con Đường Tu Luyện
        finalDamage += getPathDamageBonus(damager, weapon);

        // 2. Áp dụng bonus từ vật phẩm đã rèn
        if (weapon != null && weapon.hasItemMeta()) {
            ItemMeta meta = weapon.getItemMeta();
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                double critChance = getAttributeValueFromLore(lore, "Tỷ lệ chí mạng");
                double lifestealPercent = getAttributeValueFromLore(lore, "Hút máu");

                // Xử lý Chí mạng
                if (critChance > 0 && random.nextDouble() * 100 < critChance) {
                    finalDamage *= 2; // Gấp đôi sát thương
                    damager.getWorld().spawnParticle(org.bukkit.Particle.CRIT_MAGIC, event.getEntity().getLocation().add(0, 1, 0), 20);
                }

                // Xử lý Hút máu
                if (lifestealPercent > 0) {
                    double healthToHeal = finalDamage * (lifestealPercent / 100.0);
                    double maxHealth = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    damager.setHealth(Math.min(maxHealth, damager.getHealth() + healthToHeal));
                }
            }
        }
        
        event.setDamage(finalDamage);
    }
    
    // Xử lý sát thương NHẬN VÀO bởi người chơi (cho Phản đòn)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTakesDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof LivingEntity)) {
            return;
        }

        Player damagedPlayer = (Player) event.getEntity();
        LivingEntity attacker = (LivingEntity) event.getDamager();
        
        double thornsPercent = 0;
        
        // Cộng dồn % phản đòn từ tất cả các món giáp
        for (ItemStack armorPiece : damagedPlayer.getInventory().getArmorContents()) {
            if (armorPiece != null && armorPiece.hasItemMeta() && armorPiece.getItemMeta().hasLore()) {
                thornsPercent += getAttributeValueFromLore(armorPiece.getItemMeta().getLore(), "Phản đòn");
            }
        }
        
        if (thornsPercent > 0) {
            double thornsDamage = event.getDamage() * (thornsPercent / 100.0);
            attacker.damage(thornsDamage);
            damagedPlayer.getWorld().spawnParticle(org.bukkit.Particle.CRIT, attacker.getLocation().add(0, 1, 0), 15);
        }
    }
    
    private double getPathDamageBonus(Player damager, ItemStack weapon) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(damager);
        if (data == null || data.getCultivationPath() == null || data.getCultivationPath().equals("none")) {
            return 0;
        }

        ConfigurationSection pathSection = plugin.getConfig().getConfigurationSection("paths." + data.getCultivationPath() + ".stats");
        if (pathSection == null) return 0;
        
        double bonus = 0;
        if (weapon == null || weapon.getType().isAir()) {
            bonus = pathSection.getDouble("hand-damage-bonus", 0.0);
        } else {
            bonus = pathSection.getDouble("weapon-damage-bonus", 0.0);
        }
        bonus += pathSection.getDouble("weapon-damage-modifier", 0.0);
        return bonus;
    }

    /**
     * Đọc một giá trị số từ lore của vật phẩm.
     * Ví dụ: đọc "5.0" từ dòng lore "Sát thương: +5.0"
     */
    private double getAttributeValueFromLore(List<String> lore, String attributeName) {
        Pattern pattern = Pattern.compile(ChatColor.stripColor(attributeName) + ": \\+?(-?[0-9.]+)");
        for (String line : lore) {
            Matcher matcher = pattern.matcher(ChatColor.stripColor(line));
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}