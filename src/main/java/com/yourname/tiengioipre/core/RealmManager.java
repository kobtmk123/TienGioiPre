package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RealmManager {
    private final TienGioiPre plugin;
    private final Map<String, Realm> realms = new HashMap<>();

    public RealmManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadRealms();
    }

    public void loadRealms() {
        realms.clear();
        ConfigurationSection realmsSection = plugin.getConfig().getConfigurationSection("realms");
        if (realmsSection == null) return;

        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection section = realmsSection.getConfigurationSection(realmId);
            if (section == null) continue;

            String displayName = section.getString("display-name", "Unknown Realm");
            String nextRealmId = section.getString("next-realm", "none");
            double maxLinhKhi = section.getDouble("max-linh-khi", 100);
            double linhKhiGain = section.getDouble("linh-khi-gain-per-tick", 1) * 20; // Chuyển từ tick sang giây
            double lightningDamage = section.getDouble("breakthrough.lightning-damage", 0);

            // Load Stats
            Map<String, Double> statBonuses = new HashMap<>();
            ConfigurationSection statsSection = section.getConfigurationSection("stats");
            if (statsSection != null) {
                statBonuses.put("max-health-bonus", statsSection.getDouble("max-health-bonus", 0));
                statBonuses.put("attack-damage-bonus", statsSection.getDouble("attack-damage-bonus", 0));
                statBonuses.put("walk-speed-bonus", statsSection.getDouble("walk-speed-bonus", 0));
            }

            // Load Potion Effects
            List<PotionEffect> potionEffects = new ArrayList<>();
            List<String> effectStrings = section.getStringList("stats.potion-effects");
            for (String effectString : effectStrings) {
                String[] parts = effectString.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                int amplifier = Integer.parseInt(parts[1]);
                if (type != null) {
                    potionEffects.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
                }
            }

            Realm realm = new Realm(realmId, displayName, nextRealmId, maxLinhKhi, linhKhiGain, lightningDamage, statBonuses, potionEffects);
            realms.put(realmId, realm);
        }
        plugin.getLogger().info("Đã tải " + realms.size() + " cảnh giới.");
    }

    public Realm getRealm(String realmId) {
        return realms.get(realmId);
    }
    
    public String getRealmDisplayName(String realmId) {
        Realm realm = getRealm(realmId);
        return realm != null ? realm.displayName() : "Không rõ";
    }

    public double getMaxLinhKhi(String realmId) {
        Realm realm = getRealm(realmId);
        return realm != null ? realm.maxLinhKhi() : 100;
    }

    public void applyRealmStats(Player player, String realmId) {
        Realm realm = getRealm(realmId);
        if (realm == null) return;

        // Xóa các hiệu ứng cũ của plugin trước khi áp dụng cái mới
        removeRealmStats(player);

        // Áp dụng máu, sát thương, tốc độ
        realm.statBonuses().forEach((stat, value) -> {
            if (value == 0) return;
            Attribute attribute = null;
            String modifierName = "tiengioi-" + stat;
            switch (stat) {
                case "max-health-bonus":
                    attribute = Attribute.GENERIC_MAX_HEALTH;
                    break;
                case "attack-damage-bonus":
                    attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                    break;
                case "walk-speed-bonus":
                    attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                    break;
            }
            if (attribute != null) {
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), modifierName, value, AttributeModifier.Operation.ADD_NUMBER);
                player.getAttribute(attribute).addModifier(modifier);
            }
        });

        // Áp dụng hiệu ứng thuốc
        player.addPotionEffects(realm.potionEffects());
    }

    public void removeRealmStats(Player player) {
        // Xóa Attribute Modifiers
        Arrays.asList(Attribute.GENERIC_MAX_HEALTH, Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_MOVEMENT_SPEED)
              .forEach(attribute -> {
                  if (player.getAttribute(attribute) != null) {
                      for (AttributeModifier modifier : player.getAttribute(attribute).getModifiers()) {
                          if (modifier.getName().startsWith("tiengioi-")) {
                              player.getAttribute(attribute).removeModifier(modifier);
                          }
                      }
                  }
              });

        // Xóa Potion Effects từ config
        realms.values().forEach(realm -> {
            realm.potionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        });
    }
}