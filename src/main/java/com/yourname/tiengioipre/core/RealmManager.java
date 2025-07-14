package com.yourname.tiengioipre.core; // <-- Đảm bảo package này đúng

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import com.yourname.tiengioipre.utils.DebugLogger; // <-- IMPORT MỚI
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Quản lý tất cả các hoạt động liên quan đến cảnh giới (Tu Vi và Bậc).
 * Bao gồm tải cấu hình, áp dụng chỉ số, và xử lý logic tăng/giảm cấp.
 */
public class RealmManager {

    private final TienGioiPre plugin;
    private final Map<String, RealmData> realmsData = new HashMap<>();
    private List<String> realmProgression;
    private List<String> tierProgression;

    // --- CÁC ĐỐI TƯỢNG LƯU DỮ LIỆU CẢNH GIỚI ---
    public record TierData(String id, String displayName, double maxLinhKhi, double linhKhiGainPerSecond, ConfigurationSection breakthroughConfig, Map<String, Double> statBonuses, List<PotionEffect> potionEffects) {}
    public record RealmData(String id, String displayName, Map<String, TierData> tiers) {}
    
    public RealmManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadRealms();
    }

    /**
     * Tải dữ liệu cảnh giới từ config.yml.
     */
    public void loadRealms() {
        realmsData.clear();
        FileConfiguration config = plugin.getConfig();
        
        if (!config.isConfigurationSection("realms")) {
            DebugLogger.warn("RealmManager", "Phan 'realms' trong config.yml bi thieu hoac khong hop le. Plugin se khong hoat dong dung chuc nang canh gioi.");
            return;
        }

        ConfigurationSection realmsSection = config.getConfigurationSection("realms");
        
        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection realmConfig = realmsSection.getConfigurationSection(realmId);
            if (realmConfig == null) continue;

            String realmDisplayName = format(realmConfig.getString("display-name", "Tu Vi Lỗi"));
            
            Map<String, TierData> tiers = new HashMap<>();
            ConfigurationSection tiersSection = realmConfig.getConfigurationSection("tiers");
            if (tiersSection != null) {
                for (String tierId : tiersSection.getKeys(false)) {
                    ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierId);
                    if (tierConfig == null) continue;

                    String tierDisplayName = format(tierConfig.getString("display-name", "Bậc Lỗi"));
                    double maxLinhKhi = tierConfig.getDouble("max-linh-khi", 100);
                    double linhKhiGain = tierConfig.getDouble("linh-khi-gain-per-second", 1);
                    ConfigurationSection breakthroughConfig = tierConfig.getConfigurationSection("breakthrough");

                    Map<String, Double> statBonuses = new HashMap<>();
                    ConfigurationSection statsSection = tierConfig.getConfigurationSection("stats");
                    if (statsSection != null) {
                        for (String key : statsSection.getKeys(false)) {
                            statBonuses.put(key, statsSection.getDouble(key));
                        }
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
                            DebugLogger.warn("RealmManager", "Lỗi đọc hiệu ứng thuốc: " + s);
                        }
                    }
                    tiers.put(tierId, new TierData(tierId, tierDisplayName, maxLinhKhi, linhKhiGain, breakthroughConfig, statBonuses, potionEffects));
                }
            }
            realmsData.put(realmId, new RealmData(realmId, realmDisplayName, tiers));
        }

        this.realmProgression = config.getStringList("progression.realms-order");
        this.tierProgression = config.getStringList("progression.tiers-order");
        DebugLogger.log("RealmManager", "Đã tải " + realmsData.size() + " Tu Vi và " + realmProgression.size() + " cấp độ đột phá.");
    }
    
    // --- CÁC HÀM GETTER CHO DỮ LIỆU CẢNH GIỚI ---

    public TierData getTierData(String realmId, String tierId) {
        if (realmId == null || tierId == null) return null;
        RealmData realm = realmsData.get(realmId);
        if (realm == null) return null;
        return realm.tiers().get(tierId);
    }
    
    public String getRealmDisplayName(String realmId) {
        if (realmId == null) return "Không Rõ";
        RealmData realm = realmsData.get(realmId);
        return realm != null ? realm.displayName() : "Không Rõ";
    }

    public String getTierDisplayName(String realmId, String tierId) {
        if (tierId == null) return "Không Rõ";
        TierData tier = getTierData(realmId, tierId);
        return tier != null ? tier.displayName() : "Không Rõ";
    }
    
    public Map<String, RealmData> getRealms() {
        return this.realmsData;
    }
    
    // --- LOGIC TĂNG/GIẢM CẤP ---

    /**
     * Tìm ra Tu Vi và Bậc tiếp theo cho người chơi.
     * @return một String array [nextRealmId, nextTierId], hoặc null nếu đã max.
     */
    public String[] getNextProgression(String currentRealmId, String currentTierId) {
        int currentTierIndex = tierProgression.indexOf(currentTierId);
        
        // Trường hợp 1: Còn bậc để lên trong Tu Vi hiện tại
        if (currentTierIndex != -1 && currentTierIndex < tierProgression.size() - 1) {
            String nextTierId = tierProgression.get(currentTierIndex + 1);
            if (getTierData(currentRealmId, nextTierId) != null) {
                return new String[]{currentRealmId, nextTierId};
            }
        }
        
        // Trường hợp 2: Đã hết bậc trong Tu Vi hiện tại, cần lên Tu Vi mới
        int currentRealmIndex = realmProgression.indexOf(currentRealmId);
        if (currentRealmIndex != -1 && currentRealmIndex < realmProgression.size() - 1) {
            String nextRealmId = realmProgression.get(currentRealmIndex + 1);
            String firstTierId = tierProgression.get(0); // Bắt đầu từ bậc đầu tiên (Sơ Kỳ)
            if (getTierData(nextRealmId, firstTierId) != null) {
                return new String[]{nextRealmId, firstTierId};
            }
        }
        return null; // Đã max Tu Vi và Bậc
    }

    /**
     * Áp dụng TOÀN BỘ chỉ số cho người chơi, bao gồm cả Cảnh Giới và Con Đường Tu Luyện.
     */
    public void applyAllStats(Player player) {
        removeRealmStats(player); // Luôn xóa stats cũ trước khi áp dụng cái mới

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        Map<String, Double> totalBonuses = new HashMap<>();
        
        // Lấy stats từ Cảnh Giới (Tu Vi/Bậc)
        TierData tier = getTierData(data.getRealmId(), data.getTierId());
        if (tier != null) {
             totalBonuses.putAll(tier.statBonuses());
        }
        
        // Cộng gộp stats từ Con Đường Tu Luyện
        String pathId = data.getCultivationPath();
        if (pathId != null && !pathId.equals("none")) {
            ConfigurationSection pathSection = plugin.getConfig().getConfigurationSection("paths." + pathId + ".stats");
            if (pathSection != null) {
                for (String key : pathSection.getKeys(false)) {
                    totalBonuses.merge(key, pathSection.getDouble(key), Double::sum);
                }
            }
        }
        
        // Áp dụng TỔNG chỉ số vào các thuộc tính của người chơi
        totalBonuses.forEach((stat, value) -> {
            if (value == 0) return;
            Attribute attribute = null;
            switch (stat) {
                case "max-health-bonus": attribute = Attribute.GENERIC_MAX_HEALTH; break;
                case "attack-damage-bonus": attribute = Attribute.GENERIC_ATTACK_DAMAGE; break;
                case "walk-speed-bonus": attribute = Attribute.GENERIC_MOVEMENT_SPEED; break;
                default: return; // Bỏ qua các stat không phải attribute (như weapon-damage-modifier)
            }
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "tiengioi.stat." + stat, value, AttributeModifier.Operation.ADD_NUMBER);
            player.getAttribute(attribute).addModifier(modifier);
        });
        
        // Áp dụng hiệu ứng Potion từ cảnh giới
        if (tier != null && !tier.potionEffects().isEmpty()) {
            player.addPotionEffects(tier.potionEffects());
        }
    }

    /**
     * Xóa tất cả các chỉ số và hiệu ứng thuốc đã được áp dụng bởi plugin.
     */
    public void removeRealmStats(Player player) {
        // Xóa Attribute Modifiers
        Arrays.asList(Attribute.GENERIC_MAX_HEALTH, Attribute.GENERIC_ATTACK_DAMAGE, Attribute.GENERIC_MOVEMENT_SPEED)
              .forEach(attribute -> {
                  if (player.getAttribute(attribute) != null) {
                      player.getAttribute(attribute).getModifiers().stream()
                            .filter(m -> m.getName().startsWith("tiengioi.stat."))
                            .forEach(m -> player.getAttribute(attribute).removeModifier(m));
                  }
              });

        // Xóa Potion Effects từ tất cả các cảnh giới (để đảm bảo xóa hết)
        realmsData.values().forEach(realm -> realm.tiers().values().forEach(tier -> {
            tier.potionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }));
    }

    /**
     * Giảm Tu Vi hoặc Bậc của người chơi sau khi luyện đan thất bại.
     * @param player Người chơi bị giảm cấp.
     * @param tierDecrease Số bậc sẽ giảm.
     * @param realmDecrease Số Tu Vi sẽ giảm.
     */
    public void decreasePlayerRealmOrTier(Player player, int tierDecrease, int realmDecrease) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            DebugLogger.warn("RealmManager", "decreasePlayerRealmOrTier: PlayerData is null for " + player.getName());
            return;
        }

        String currentRealmId = data.getRealmId();
        String currentTierId = data.getTierId();

        int currentRealmIndex = realmProgression.indexOf(currentRealmId);
        int currentTierIndex = tierProgression.indexOf(currentTierId);

        if (currentRealmIndex == -1 || currentTierIndex == -1) {
            DebugLogger.warn("RealmManager", "decreasePlayerRealmOrTier: Invalid current realm/tier for " + player.getName() + ": " + currentRealmId + "/" + currentTierId);
            return;
        }

        int newTierIndex = currentTierIndex - tierDecrease;
        int newRealmIndex = currentRealmIndex - realmDecrease;

        // Xử lý giảm bậc xuống dưới 0 (tức là cần giảm cả Tu Vi)
        while (newTierIndex < 0) {
            if (newRealmIndex <= 0) { // Không thể giảm thấp hơn Phàm Nhân Sơ Kỳ
                data.setRealmId(realmProgression.get(0));
                data.setTierId(tierProgression.get(0));
                data.setLinhKhi(0); // Reset linh khí về 0 khi bị giáng về Phàm Nhân Sơ Kỳ
                applyAllStats(player);
                player.sendMessage(format("&cBạn đã bị giáng xuống Phàm Nhân Sơ Kỳ!"));
                DebugLogger.log("RealmManager", player.getName() + " was reset to Phàm Nhân Sơ Kỳ due to failure.");
                return;
            }
            newRealmIndex--; // Giảm Tu Vi
            newTierIndex += tierProgression.size(); // Chuyển lên bậc tương ứng trong Tu Vi trước đó
        }

        // Đảm bảo không giảm xuống dưới Tu Vi Phàm Nhân
        if (newRealmIndex < 0) newRealmIndex = 0;

        String targetRealmId = realmProgression.get(newRealmIndex);
        String targetTierId = tierProgression.get(newTierIndex);

        // Kiểm tra xem bậc đích có tồn tại trong Tu Vi đó không
        // Nếu không, tìm bậc cao nhất của Tu Vi đó
        while (getTierData(targetRealmId, targetTierId) == null && tierProgression.indexOf(targetTierId) > 0) {
            targetTierId = tierProgression.get(tierProgression.indexOf(targetTierId) - 1);
        }
        // Nếu vẫn không tìm được, nghĩa là Tu Vi đó không có bậc nào, thì fallback về Phàm Nhân Sơ Kỳ
        if (getTierData(targetRealmId, targetTierId) == null) {
             targetRealmId = realmProgression.get(0);
             targetTierId = tierProgression.get(0);
        }


        data.setRealmId(targetRealmId);
        data.setTierId(targetTierId);
        data.setLinhKhi(Math.max(0, data.getLinhKhi() * 0.5)); // Giảm 50% linh khí khi bị giáng cấp
        
        applyAllStats(player);
        player.sendMessage(format("&cBạn đã bị giáng cấp xuống &4" + getRealmDisplayName(targetRealmId) + " - " + getTierDisplayName(targetRealmId, targetTierId) + "&c do luyện đan thất bại!"));
        DebugLogger.log("RealmManager", player.getName() + " was decreased to " + targetRealmId + " - " + targetTierId);
    }


    private String format(String msg) {
        return plugin.getTongMonManager().format(msg);
    }
}