package com.yourname.tiengioipre.core;

import org.bukkit.potion.PotionEffect;
import java.util.List;
import java.util.Map;

// Sử dụng record của Java 16+ cho ngắn gọn, hoặc chuyển thành class nếu dùng Java cũ hơn.
public record Realm(
    String id,
    String displayName,
    String nextRealmId,
    double maxLinhKhi,
    double linhKhiGainPerSecond,
    double breakthroughLightningDamage,
    Map<String, Double> statBonuses, // "max-health", "attack-damage", "walk-speed"
    List<PotionEffect> potionEffects
) {}