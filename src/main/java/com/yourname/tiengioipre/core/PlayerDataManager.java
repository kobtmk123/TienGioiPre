package com.yourname.tiengioipre.core;

// ... import
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    // ...
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
        
        if (!playerFile.exists()) {
            String defaultRealm = plugin.getConfig().getString("settings.default-realm-id", "phannhan");
            String defaultTier = plugin.getConfig().getString("settings.default-tier-id", "soky");
            String defaultPath = plugin.getConfig().getString("paths.settings.default-path", "none");
            PlayerData newPlayerData = new PlayerData(uuid, defaultRealm, defaultTier, 0, defaultPath);
            playerDataMap.put(uuid, newPlayerData);
            savePlayerData(newPlayerData); 
        } else {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            String realmId = config.getString("realm-id");
            String tierId = config.getString("tier-id");
            double linhKhi = config.getDouble("linh-khi");
            String path = config.getString("cultivation-path", "none");
            playerDataMap.put(uuid, new PlayerData(uuid, realmId, tierId, linhKhi, path));
        }
        
        PlayerData data = getPlayerData(player);
        if (data != null) {
             plugin.getRealmManager().applyAllStats(player);
        }
    }

    public void savePlayerData(PlayerData data) {
        if (data == null) return;
        File playerFile = new File(dataFolder, data.getUuid().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("realm-id", data.getRealmId());
        config.set("tier-id", data.getTierId());
        config.set("linh-khi", data.getLinhKhi());
        config.set("cultivation-path", data.getCultivationPath());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // ... các hàm khác giữ nguyên ...
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