package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        // SỬA LỖI Ở ĐÂY: Gọi hàm khởi tạo với đúng 4 tham số
        if (!playerFile.exists()) {
            String defaultRealm = plugin.getConfig().getString("settings.default-realm-id", "phannhan");
            String defaultTier = plugin.getConfig().getString("settings.default-tier-id", "soky");
            PlayerData newPlayerData = new PlayerData(uuid, defaultRealm, defaultTier, 0);
            playerDataMap.put(uuid, newPlayerData);
            savePlayerData(newPlayerData); 
        } else {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            String realmId = config.getString("realm-id");
            String tierId = config.getString("tier-id");
            double linhKhi = config.getDouble("linh-khi");
            playerDataMap.put(uuid, new PlayerData(uuid, realmId, tierId, linhKhi));
        }
        
        PlayerData data = getPlayerData(player);
        if (data != null && data.getRealmId() != null && data.getTierId() != null) {
             plugin.getRealmManager().applyRealmStats(player, data.getRealmId(), data.getTierId());
        }
    }

    public void savePlayerData(PlayerData data) {
        if (data == null) return;
        File playerFile = new File(dataFolder, data.getUuid().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("realm-id", data.getRealmId());
        config.set("tier-id", data.getTierId());
        config.set("linh-khi", data.getLinhKhi());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Khong the luu du lieu cho nguoi choi " + data.getUuid());
            e.printStackTrace();
        }
    }
    
    public void unloadPlayerData(Player player) {
        PlayerData data = playerDataMap.remove(player.getUniqueId());
        if (data != null) {
            savePlayerData(data);
        }
    }
    
    public void saveAllPlayerData() {
        playerDataMap.values().forEach(this::savePlayerData);
    }
}