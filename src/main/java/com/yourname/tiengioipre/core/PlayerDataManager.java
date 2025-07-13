package com.myname.tiengioipre.core;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quản lý việc đọc và ghi dữ liệu của người chơi từ file.
 * Xử lý việc tải dữ liệu khi người chơi vào server và lưu lại khi họ thoát.
 */
public class PlayerDataManager {

    private final TienGioiPre plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final File dataFolder;

    public PlayerDataManager(TienGioiPre plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Lấy dữ liệu đang được lưu trong bộ nhớ (RAM) của người chơi.
     * @param player Người chơi cần lấy dữ liệu.
     * @return Đối tượng PlayerData, hoặc null nếu không có.
     */
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    /**
     * Tải dữ liệu của người chơi từ file vào bộ nhớ.
     * Nếu người chơi mới, tạo dữ liệu mặc định cho họ.
     * @param player Người chơi vừa vào server.
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            // Xử lý người chơi mới
            String defaultRealm = plugin.getConfig().getString("settings.default-realm-id", "phannhan");
            String defaultTier = plugin.getConfig().getString("settings.default-tier-id", "soky");
            String defaultPath = plugin.getConfig().getString("paths.settings.default-path", "none");
            String defaultTongMon = "none"; // Người chơi mới không có tông môn
            
            // Tạo đối tượng PlayerData mới với 6 tham số
            PlayerData newPlayerData = new PlayerData(uuid, defaultRealm, defaultTier, 0, defaultPath, defaultTongMon);
            playerDataMap.put(uuid, newPlayerData);
            savePlayerData(newPlayerData); // Lưu file lần đầu cho người chơi mới
        } else {
            // Xử lý người chơi cũ
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            String realmId = config.getString("realm-id");
            String tierId = config.getString("tier-id");
            double linhKhi = config.getDouble("linh-khi");
            String path = config.getString("cultivation-path", "none");
            String tongMonId = config.getString("tong-mon-id", "none"); // <-- ĐỌC DỮ LIỆU TÔNG MÔN
            
            // Tạo đối tượng PlayerData với 6 tham số
            playerDataMap.put(uuid, new PlayerData(uuid, realmId, tierId, linhKhi, path, tongMonId));
        }
        
        // Sau khi tải dữ liệu, áp dụng các chỉ số cho người chơi
        plugin.getRealmManager().applyAllStats(player);
    }

    /**
     * Lưu dữ liệu của một người chơi từ bộ nhớ ra file YAML.
     * @param data Đối tượng PlayerData cần lưu.
     */
    public void savePlayerData(PlayerData data) {
        if (data == null) return;
        File playerFile = new File(dataFolder, data.getUuid().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("realm-id", data.getRealmId());
        config.set("tier-id", data.getTierId());
        config.set("linh-khi", data.getLinhKhi());
        config.set("cultivation-path", data.getCultivationPath());
        config.set("tong-mon-id", data.getTongMonId()); // <-- LƯU DỮ LIỆU TÔNG MÔN

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu dữ liệu cho người chơi " + data.getUuid());
            e.printStackTrace();
        }
    }
    
    /**
     * Gỡ dữ liệu của người chơi khỏi bộ nhớ và lưu vào file (khi họ thoát game).
     * @param player Người chơi vừa thoát.
     */
    public void unloadPlayerData(Player player) {
        PlayerData data = playerDataMap.remove(player.getUniqueId());
        if (data != null) {
            savePlayerData(data);
        }
    }
    
    /**
     * Lưu dữ liệu của tất cả người chơi đang online (dùng khi tắt server).
     */
    public void saveAllPlayerData() {
        playerDataMap.values().forEach(this::savePlayerData);
    }
}