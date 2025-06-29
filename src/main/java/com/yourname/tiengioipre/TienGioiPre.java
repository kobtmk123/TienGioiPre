package com.yourname.tiengioipre;

import com.yourname.tiengioipre.api.PAPIExpansion;
import com.yourname.tiengioipre.api.TienGioiAPI; // Phải import class API
import com.yourname.tiengioipre.commands.DotPhaCommand;
import com.yourname.tiengioipre.commands.MainCommand;
import com.yourname.tiengioipre.commands.ShopTienGioiCommand;
import com.yourname.tiengioipre.commands.TuLuyenCommand;
import com.yourname.tiengioipre.core.CultivationManager;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.core.PlayerDataManager;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.gui.ShopGUI;
import com.yourname.tiengioipre.listeners.PlayerConnectionListener;
import com.yourname.tiengioipre.listeners.PlayerInteractListener;
import com.yourname.tiengioipre.listeners.PlayerStateListener;
import com.yourname.tiengioipre.listeners.ShopListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TienGioiPre extends JavaPlugin {

    // --- Các biến và manager ---
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
        saveDefaultConfig();
        this.playerDataManager = new PlayerDataManager(this);
        this.realmManager = new RealmManager(this);
        this.cultivationManager = new CultivationManager(this);
        this.itemManager = new ItemManager(this);
        this.shopGUI = new ShopGUI(this);
        registerCommands();
        registerListeners();
        if (!setupEconomy()) {
            getLogger().severe("!!! Khong tim thay Vault hoac plugin Economy! Chuc nang shop se bi vo hieu hoa.");
        } else {
            getLogger().info("Da tich hop thanh cong voi Vault (Economy).");
        }
        setupPlaceholderAPI();
        cultivationManager.startCultivationTask();
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

    // --- Các phương thức thiết lập ---
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("tiengioi").setExecutor(mainCommand);
        getCommand("tiengioi").setTabCompleter(mainCommand);
        getCommand("tuluyen").setExecutor(new TuLuyenCommand(this));
        getCommand("dotpha").setExecutor(new DotPhaCommand(this));
        getCommand("shoptiengioi").setExecutor(new ShopTienGioiCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Da tich hop thanh cong voi PlaceholderAPI.");
        } else {
            getLogger().warning("Khong tim thay PlaceholderAPI, cac placeholder se khong hoat dong.");
        }
    }

    // --- CÁC GETTER VÀ API ---

    public static TienGioiPre getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public RealmManager getRealmManager() {
        return realmManager;
    }

    public CultivationManager getCultivationManager() {
        return cultivationManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    /**
     * Lấy instance của API để các plugin khác sử dụng.
     * ĐÃ DI CHUYỂN VÀO ĐÚNG VỊ TRÍ (BÊN TRONG CLASS)
     * @return một instance của TienGioiAPI.
     */
    public static TienGioiAPI getAPI() {
        if (instance == null) {
            return null; // Plugin chưa được bật
        }
        return new TienGioiAPI(instance);
    }

} // <-- DẤU NGOẶC NHỌN ĐÓNG CLASS CUỐI CÙNG