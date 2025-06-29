package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RealmManager {

    private final TienGioiPre plugin;
    private final Map<String, RealmData> realmsData = new HashMap<>();
    private List<String> realmProgression;
    private List<String> tierProgression;

    // --- CÁC ĐỐI TƯỢNG LƯU DỮ LIỆU ---
    public record TierData(String id, String displayName, double maxLinhKhi, double linhKhiGainPerSecond, double breakthroughLightningDamage, Map<String, Double> statBonuses, List<PotionEffect> potionEffects) {}
    public record RealmData(String id, String displayName, Map<String, TierData> tiers) {}
    
    // --- KHỞI TẠO VÀ TẢI DỮ LIỆU ---
    public RealmManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadRealms();
    }

    public void loadRealms() {
        realmsData.clear();
        ConfigurationSection realmsSection = plugin.getConfig().getConfigurationSection("realms");
        if (realmsSection == null) {
            plugin.getLogger().severe("Phan 'realms' trong config.yml khong ton tai!");
            return;
        }

        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection realmConfig = realmsSection.getConfigurationSection(realmId);
            String realmDisplayName = format(realmConfig.getString("display-name", "Tu Vi Loi"));
            
            Map<String, TierData> tiers = new HashMap<>();
            ConfigurationSection tiersSection = realmConfig.getConfigurationSection("tiers");
            if (tiersSection != null) {
                for (String tierId : tiersSection.getKeys(false)) {
                    ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierId);
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

        this.realmProgression = plugin.getConfig().getStringList("progression.realms-order");
        this.tierProgression = plugin.getConfig().getStringList("progression.tiers-order");
        plugin.getLogger().info("Da tai " + realmsData.size() + " Tu Vi va " + realmProgression.size() + " cap do dot pha.");
    }
    
    // --- CÁC HÀM GETTER ---
    public TierData getTierData(String realmId, String tierId) {
        if (realmId == null || tierId == null) return null;
        RealmData realm = realmsData.get(realmId);
        if (realm == null) return null;
        return realm.tiers().get(tierId);
    }
    
    public String getRealmDisplayName(String realmId) {
        if (realmId == null) return "Khong Ro";
        RealmData realm = realmsData.get(realmId);
        return realm != null ? realm.displayName() : "Khong Ro";
    }

    public String getTierDisplayName(String realmId, String tierId) {
        if (tierId == null) return "Khong Ro";
        TierData tier = getTierData(realmId, tierId);
        return tier != null ? tier.displayName() : "Khong Ro";
    }
    
    public Map<String, RealmData> getRealmsDataMap() {
        return this.realmsData;
    }
    
    // --- LOGIC CHÍNH ---
    public String[] getNextProgression(String currentRealmId, String currentTierId) {
        int currentTierIndex = tierProgression.indexOf(currentTierId);
        
        if (currentTierIndex != -1 && currentTierIndex < tierProgression.size() - 1) {
            String nextTierId = tierProgression.get(currentTierIndex + 1);
            if (getTierData(currentRealmId, nextTierId) != null) {
                return new String[]{currentRealmId, nextTierId};
            }
        }
        
        int currentRealmIndex = realmProgression.indexOf(currentRealmId);
        if (currentRealmIndex != -1 && currentRealmIndex < realmProgression.size() - 1) {
            String nextRealmId = realmProgression.get(currentRealmIndex + 1);
            String firstTierId = tierProgression.get(0);
            if (getTierData(nextRealmId, firstTierId) != null) {
                return new String[]{nextRealmId, firstTierId};
            }
        }
        return null;
    }

    public void applyRealmStats(Player player, String realmId, String tierId) {
        removeRealmStats(player);
        TierData tier = getTierData(realmId, tierId);
        if (tier == null) return;
        
        tier.statBonuses().forEach((stat, value) -> {
            if (value == 0) return;
            Attribute attribute = null;
            switch (stat) {
                case "max-health-bonus": attribute = Attribute.GENERIC_MAX_HEALTH; break;
                case "attack-damage-bonus": attribute = Attribute.GENERIC_ATTACK_DAMAGE; break;
                case "walk-speed-bonus": attribute = Attribute.GENERIC_MOVEMENT_SPEED; break;
            }
            if (attribute != null) {
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "tiengioi." + stat, value, AttributeModifier.Operation.ADD_NUMBER);
                player.getAttribute(attribute).addModifier(modifier);
            }
        });
        
        if (!tier.potionEffects().isEmpty()) {
            player.addPotionEffects(tier.potionEffects());
        }
    }

    public void removeRealmStats(Player player) {
        Arrays.asList(Attribute.GENERIC_MAX_HEALTH, Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_MOVEMENT_SPEED)
              .forEach(attribute -> {
                  if (player.getAttribute(attribute) != null) {
                      for (AttributeModifier modifier : new ArrayList<>(player.getAttribute(attribute).getModifiers())) {
                          if (modifier.getName().startsWith("tiengioi.")) {
                              player.getAttribute(attribute).removeModifier(modifier);
                          }
                      }
                  }
              });

        realmsData.values().forEach(realm -> realm.tiers().values().forEach(tier -> {
            tier.potionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }));
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}