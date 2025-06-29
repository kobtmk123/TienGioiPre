package com.yourname.tiengioipre;

import com.yourname.tiengioipre.api.PAPIExpansion;
import com.yourname.tiengioipre.api.TienGioiAPI;
import com.yourname.tiengioipre.commands.DotPhaCommand;
import com.yourname.tiengioipre.commands.MainCommand;
import com.yourname.tiengioipre.commands.PathCommand;
import com.yourname.tiengioipre.commands.ShopTienGioiCommand;
import com.yourname.tiengioipre.commands.TuLuyenCommand;
import com.yourname.tiengioipre.core.CultivationManager;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.core.PlayerDataManager;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.gui.ShopGUI;
import com.yourname.tiengioipre.listeners.PlayerConnectionListener;
import com.yourname.tiengioipre.listeners.PlayerDamageListener;
import com.yourname.tiengioipre.listeners.PlayerInteractListener;
import com.yourname.tiengioipre.listeners.PlayerStateListener;
import com.yourname.tiengioipre.listeners.ShopListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TienGioiPre extends JavaPlugin {

    private static TienGioiPre instance;
    private static Economy econ = null;

    private PlayerDataManager playerDataManager;
    private RealmManager realmManager;
    private CultivationManager cultivationManager;
    private ItemManager itemManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("------------------------------------");
        getLogger().info("Dang khoi dong plugin TienGioiPre...");

        // --- HỆ THỐNG CONFIG MỚI ---
        setupConfig();

        // Khởi tạo tất cả các trình quản lý
        this.itemManager = new ItemManager(this);
        this.shopGUI = new ShopGUI(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.realmManager = new RealmManager(this);
        this.cultivationManager = new CultivationManager(this);

        // Đăng ký lệnh và sự kiện
        registerCommands();
        registerListeners();

        // Tích hợp với các plugin khác
        setupIntegrations();

        getLogger().info("Plugin TienGioiPre da duoc bat thanh cong!");
        getLogger().info("------------------------------------");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        getLogger().info("Plugin TienGioiPre da duoc tat.");
    }

    /**
     * Tự động tạo và kiểm tra file config.yml
     */
    private void setupConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().info("Khong tim thay config.yml, dang tao file moi...");
            // Sao chép file config.yml mẫu từ bên trong file .jar ra thư mục plugin
            saveResource("config.yml", false);
        }
        // Sau này có thể thêm logic kiểm tra config-version để tự động cập nhật
        // getConfig().options().copyDefaults(true);
        // saveConfig();
    }

    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("tiengioi").setExecutor(mainCommand);
        getCommand("tiengioi").setTabCompleter(mainCommand);
        getCommand("tuluyen").setExecutor(new TuLuyenCommand(this));
        getCommand("dotpha").setExecutor(new DotPhaCommand(this));
        getCommand("shoptiengioi").setExecutor(new ShopTienGioiCommand(this));
        PathCommand pathCommand = new PathCommand(this);
        getCommand("conduongtuluyen").setExecutor(pathCommand);
        getCommand("kiemtu").setExecutor(pathCommand);
        getCommand("matu").setExecutor(pathCommand);
        getCommand("phattu").setExecutor(pathCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
    }

    private void setupIntegrations() {
        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("!!! Khong tim thay Vault! Chuc nang shop se bi vo hieu hoa.");
        } else {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                getLogger().severe("!!! Khong tim thay plugin Economy (EssentialsX, etc.)! Chuc nang shop se bi vo hieu hoa.");
            } else {
                econ = rsp.getProvider();
                getLogger().info("Da tich hop thanh cong voi Vault (Economy).");
            }
        }
        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Da tich hop thanh cong voi PlaceholderAPI.");
        } else {
            getLogger().warning("Khong tim thay PlaceholderAPI, cac placeholder se khong hoat dong.");
        }
    }

    // --- CÁC GETTER VÀ API ---
    public static TienGioiPre getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public RealmManager getRealmManager() { return realmManager; }
    public CultivationManager getCultivationManager() { return cultivationManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
    public static TienGioiAPI getAPI() {
        return (instance != null) ? new TienGioiAPI(instance) : null;
    }
}