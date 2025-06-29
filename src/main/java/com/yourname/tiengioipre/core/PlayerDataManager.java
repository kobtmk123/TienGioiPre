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
        if (!playerFile.exists()) {
            // Người chơi mới
            String defaultRealm = plugin.getConfig().getString("settings.default-realm", "phannhan_soky_tang1");
            PlayerData newPlayerData = new PlayerData(uuid, defaultRealm, 0);
            playerDataMap.put(uuid, newPlayerData);
            savePlayerData(player); // Lưu file lần đầu
        } else {
            // Người chơi cũ
            FileConfiguration playerDataConfig = YamlConfiguration.loadConfiguration(playerFile);
            String realmId = playerDataConfig.getString("realm");
            double linhKhi = playerDataConfig.getDouble("linh-khi");
            playerDataMap.put(uuid, new PlayerData(uuid, realmId, linhKhi));
        }
        // TODO: Áp dụng stats cho người chơi
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration playerDataConfig = YamlConfiguration.loadConfiguration(playerFile);
        
        playerDataConfig.set("realm", data.getRealmId());
        playerDataConfig.set("linh-khi", data.getLinhKhi());

        try {
            playerDataConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unloadPlayerData(Player player) {
        savePlayerData(player);
        playerDataMap.remove(player.getUniqueId());
    }
    
    public void saveAllPlayerData() {
        playerDataMap.keySet().forEach(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                savePlayerData(player);
            }
        });
    }
}