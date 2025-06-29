package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
// ... các import khác ...
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealmManager {
    private final TienGioiPre plugin;
    // Lưu trữ theo cấu trúc mới
    private final Map<String, RealmData> realmsData = new HashMap<>();
    private List<String> realmProgression;
    private List<String> tierProgression;

    public RealmManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadRealms();
    }

    public void loadRealms() {
        realmsData.clear();
        ConfigurationSection realmsSection = plugin.getConfig().getConfigurationSection("realms");
        if (realmsSection == null) return;

        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection realmConfig = realmsSection.getConfigurationSection(realmId);
            String realmDisplayName = realmConfig.getString("display-name", "Tu Vi Lỗi");
            
            Map<String, TierData> tiers = new HashMap<>();
            ConfigurationSection tiersSection = realmConfig.getConfigurationSection("tiers");
            if (tiersSection != null) {
                for (String tierId : tiersSection.getKeys(false)) {
                    ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierId);
                    // Tạo đối tượng TierData và thêm vào map
                    // (Bạn cần tạo class TierData tương tự RealmData)
                    TierData tier = new TierData(/* ... đọc dữ liệu từ tierConfig ... */);
                    tiers.put(tierId, tier);
                }
            }
            realmsData.put(realmId, new RealmData(realmId, realmDisplayName, tiers));
        }

        // Tải "bản đồ" đột phá
        this.realmProgression = plugin.getConfig().getStringList("progression.realms-order");
        this.tierProgression = plugin.getConfig().getStringList("progression.tiers-order");
        
        plugin.getLogger().info("Đã tải " + realmsData.size() + " Tu Vi.");
    }
    
    // Hàm lấy dữ liệu của một bậc cụ thể
    public TierData getTierData(String realmId, String tierId) {
        RealmData realm = realmsData.get(realmId);
        if (realm == null) return null;
        return realm.tiers().get(tierId);
    }
    
    // Hàm lấy tên hiển thị của Tu Vi
    public String getRealmDisplayName(String realmId) {
        RealmData realm = realmsData.get(realmId);
        return realm != null ? realm.displayName() : "Không Rõ";
    }

    // Hàm lấy tên hiển thị của Bậc
    public String getTierDisplayName(String realmId, String tierId) {
        TierData tier = getTierData(realmId, tierId);
        return tier != null ? tier.displayName() : "Không Rõ";
    }

    /**
     * Tìm ra Tu Vi và Bậc tiếp theo cho người chơi.
     * @return một String array [nextRealmId, nextTierId], hoặc null nếu đã max.
     */
    public String[] getNextProgression(String currentRealmId, String currentTierId) {
        int currentTierIndex = tierProgression.indexOf(currentTierId);
        
        // Trường hợp 1: Còn bậc để lên trong Tu Vi hiện tại
        if (currentTierIndex < tierProgression.size() - 1) {
            String nextTierId = tierProgression.get(currentTierIndex + 1);
            // Kiểm tra xem Tu Vi hiện tại có bậc đó không
            if (getTierData(currentRealmId, nextTierId) != null) {
                return new String[]{currentRealmId, nextTierId};
            }
        }
        
        // Trường hợp 2: Đã hết bậc, cần lên Tu Vi mới
        int currentRealmIndex = realmProgression.indexOf(currentRealmId);
        if (currentRealmIndex < realmProgression.size() - 1) {
            String nextRealmId = realmProgression.get(currentRealmIndex + 1);
            String nextTierId = tierProgression.get(0); // Bắt đầu từ bậc đầu tiên (Sơ Kỳ)
            return new String[]{nextRealmId, nextTierId};
        }

        // Trường hợp 3: Đã max Tu Vi, max Bậc
        return null;
    }

    // Bạn cần tạo các class/record RealmData và TierData để lưu trữ dữ liệu
    public record RealmData(String id, String displayName, Map<String, TierData> tiers) {}
    public record TierData(String displayName, double maxLinhKhi, double linhKhiGainPerSecond, double breakthroughLightningDamage /*, ... các stats khác ...*/) {}
    
    // ... các hàm apply/remove stats cần được cập nhật để lấy dữ liệu từ TierData ...
}