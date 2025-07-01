package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RealmManager {

    private final TienGioiPre plugin;
    private final Map<String, RealmData> realmsData = new HashMap<>();
    private List<String> realmProgression;
    private List<String> tierProgression;

    public record TierData(String id, String displayName, double maxLinhKhi, double linhKhiGainPerSecond, double breakthroughLightningDamage, Map<String, Double> statBonuses, List<PotionEffect> potionEffects) {}
    public record RealmData(String id, String displayName, Map<String, TierData> tiers) {}
    
    public RealmManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadRealms();
    }

    public void loadRealms() {
        realmsData.clear();
        FileConfiguration config = plugin.getConfig();
        
        // Kiểm tra xem mục 'realms' có thực sự tồn tại và là một section không
        if (!config.isConfigurationSection("realms")) {
            plugin.getLogger().severe("Phan 'realms' trong config.yml bi thieu hoac khong hop le. Plugin se khong hoat dong dung chuc nang canh gioi.");
            return;
        }

        ConfigurationSection realmsSection = config.getConfigurationSection("realms");
        
        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection realmConfig = realmsSection.getConfigurationSection(realmId);
            if (realmConfig == null) continue;

            String realmDisplayName = format(realmConfig.getString("display-name", "Tu Vi Loi"));
            
            Map<String, TierData> tiers = new HashMap<>();
            ConfigurationSection tiersSection = realmConfig.getConfigurationSection("tiers");
            if (tiersSection != null) {
                for (String tierId : tiersSection.getKeys(false)) {
                    ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierId);
                    if (tierConfig == null) continue;

                    String tierDisplayName = format(tierConfig.getString("display-name", "Bac Loi"));
                    double maxLinhKhi = tierConfig.getDouble("max-linh-khi", 100);
                    double linhKhiGain = tierConfig.getDouble("linh-khi-gain-per-second", 1);
                    double lightningDamage = tierConfig.getDouble("breakthrough.lightning-damage", 0);

                    Map<String, Double> statBonuses = new HashMap<>();
                    ConfigurationSection statsSection = tierConfig.getConfigurationSection("stats");
                    if (statsSection != null) {
                        statBonuses.put("max-health-bonus", statsSection.getDouble("max-health-bonus", 0));
                        statBonuses.put("attack-damage-bonus", statsSection.getDouble("attack-damage-bonus", 0));
                        statBonuses.put("walk-speed-bonus", statsSection.getDouble("walk-speed-bonus", 0));
                    }

                    List<PotionEffect> potionEffects = new ArrayList<>();
                    List<String> effectStrings = tierConfig.getStringList("stats.potion-effects");
                    for (String s : effectStrings) {
                        try {
                            String[] parts = s.split(":");
                            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                            int amplifier = Integer.parseInt(parts[1]);
                            if (type != null) {
                                potionEffects.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Loi doc hieu ung thuoc: " + s);
                        }
                    }
                    tiers.put(tierId, new TierData(tierId, tierDisplayName, maxLinhKhi, linhKhiGain, lightningDamage, statBonuses, potionEffects));
                }
            }
            realmsData.put(realmId, new RealmData(realmId, realmDisplayName, tiers));
        }

        this.realmProgression = config.getStringList("progression.realms-order");
        this.tierProgression = config.getStringList("progression.tiers-order");
        plugin.getLogger().info("Da tai " + realmsData.size() + " Tu Vi.");
    }
    
    // ... các hàm getter giữ nguyên ...

    public void applyAllStats(Player player) {
        removeRealmStats(player); 

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        Map<String, Double> totalBonuses = new HashMap<>();
        
        // Lấy stats từ Cảnh Giới
        TierData tier = getTierData(data.getRealmId(), data.getTierId());
        if (tier != null) {
             totalBonuses.putAll(tier.statBonuses());
        }
        
        // Cộng gộp stats từ Con Đường
        String pathId = data.getCultivationPath();
        if (pathId != null && !pathId.equals("none")) {
            ConfigurationSection pathSection = plugin.getConfig().getConfigurationSection("paths." + pathId + ".stats");
            if (pathSection != null) {
                for (String key : pathSection.getKeys(false)) {
                    totalBonuses.merge(key, pathSection.getDouble(key), Double::sum);
                }
            }
        }
        
        // Áp dụng TỔNG chỉ số
        totalBonuses.forEach((stat, value) -> {
            // ... (logic áp dụng stats)
        });
        
        // Áp dụng hiệu ứng Potion từ cảnh giới
        if (tier != null && !tier.potionEffects().isEmpty()) {
            player.addPotionEffects(tier.potionEffects());
        }
    }
    
    // ... các hàm còn lại giữ nguyên ...
}