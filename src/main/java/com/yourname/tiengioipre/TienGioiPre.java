package com.yourname.tiengioipre;

import com.yourname.tiengioipre.api.PAPIExpansion;
import com.yourname.tiengioipre.api.TienGioiAPI;
import com.yourname.tiengioipre.commands.*;
import com.yourname.tiengioipre.core.*;
import com.yourname.tiengioipre.gui.ShopGUI;
import com.yourname.tiengioipre.listeners.*;
import com.yourname.tiengioipre.tasks.AlchemyFurnaceTask; // <-- IMPORT MỚI
import com.yourname.tiengioipre.tasks.CultivationTask;
import com.yourname.tiengioipre.utils.DebugLogger; // <-- IMPORT MỚI
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TienGioiPre extends JavaPlugin {

    // Biến instance tĩnh để truy cập plugin từ bất kỳ đâu
    private static TienGioiPre instance;
    private static Economy econ = null;

    // Các trình quản lý (Manager) cho từng chức năng của plugin
    private PlayerDataManager playerDataManager;
    private RealmManager realmManager;
    private CultivationManager cultivationManager;
    private ItemManager itemManager;
    private ShopGUI shopGUI;
    private TongMonManager tongMonManager;
    
    // Các Task chính của plugin
    private BukkitTask cultivationMainTask; // Task cho tu luyện
    private BukkitTask alchemyFurnaceMainTask; // Task cho lò nung luyện đan

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("------------------------------------");
        getLogger().info("Dang khoi dong plugin TienGioiPre...");

        // Tải các file cấu hình
        saveDefaultConfig();

        // Cài đặt Debug Mode từ config
        DebugLogger.setDebugMode(getConfig().getBoolean("settings.debug-mode", true)); // Đọc từ config

        // Khởi tạo tất cả các trình quản lý THEO ĐÚNG THỨ TỰ
        this.itemManager = new ItemManager(this);
        this.shopGUI = new ShopGUI(this);
        this.tongMonManager = new TongMonManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.realmManager = new RealmManager(this);
        this.cultivationManager = new CultivationManager(this);

        // Đăng ký lệnh và sự kiện
        registerCommands();
        registerListeners();

        // Tích hợp với các plugin khác
        setupIntegrations();
        
        // === KHỞI ĐỘNG CÁC TASK CHÍNH ===
        this.cultivationMainTask = new CultivationTask(this).runTaskTimer(this, 0L, 20L); // Task Tu Luyện mỗi giây
        getLogger().info("Da khoi dong Task Tu Luyen chinh.");

        this.alchemyFurnaceMainTask = new AlchemyFurnaceTask(this).runTaskTimer(this, 20L, 20L); // Task Lò Luyện Đan mỗi giây
        getLogger().info("Da khoi dong Task Luyen Dan trong lo.");

        getLogger().info("Plugin TienGioiPre da duoc bat thanh cong!");
        getLogger().info("------------------------------------");
    }

    @Override
    public void onDisable() {
        // Hủy các task khi plugin tắt
        if (this.cultivationMainTask != null && !this.cultivationMainTask.isCancelled()) {
            this.cultivationMainTask.cancel();
        }
        if (this.alchemyFurnaceMainTask != null && !this.alchemyFurnaceMainTask.isCancelled()) {
            this.alchemyFurnaceMainTask.cancel();
        }
        
        // Lưu dữ liệu tông môn
        if (tongMonManager != null) {
            tongMonManager.saveTongMonData();
        }

        // Lưu dữ liệu người chơi
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        getLogger().info("Plugin TienGioiPre da duoc tat.");
    }

    /**
     * Đăng ký tất cả các lệnh của plugin.
     */
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("tiengioi").setExecutor(mainCommand);
        getCommand("tiengioi").setTabCompleter(mainCommand);

        getCommand("tuluyen").setExecutor(new TuLuyenCommand(this));
        getCommand("dotpha").setExecutor(new DotPhaCommand(this));
        getCommand("shoptiengioi").setExecutor(new ShopTienGioiCommand(this));

        PathCommand pathCommand = new PathCommand(this);
        getCommand("conduongtuluyen").setExecutor(pathCommand);
        getCommand("conduongtuluyen").setTabCompleter(pathCommand);
        getCommand("kiemtu").setExecutor(pathCommand);
        getCommand("matu").setExecutor(pathCommand);
        getCommand("phattu").setExecutor(pathCommand);
        getCommand("luyenkhisu").setExecutor(pathCommand);
        getCommand("luyendansu").setExecutor(pathCommand);
        
        TongMonCommand tongMonCommand = new TongMonCommand(this);
        getCommand("tongmon").setExecutor(tongMonCommand);
        getCommand("tongmon").setTabCompleter(tongMonCommand);
    }

    /**
     * Đăng ký tất cả các sự kiện (listeners) của plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilRefineListener(this), this);
        getServer().getPluginManager().registerEvents(new OreSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new TongMonListener(this), this);
        
        // Đăng ký các listener cho hệ thống Luyện Đan
        getServer().getPluginManager().registerEvents(new HerbDropListener(this), this);
        getServer().getPluginManager().registerEvents(new CauldronAlchemyListener(this), this);
        // FurnaceRefinePillListener không còn được đăng ký ở đây nữa vì logic đã chuyển sang AlchemyFurnaceTask
        // getServer().getPluginManager().registerEvents(new FurnaceRefinePillListener(this), this); 
    }

    /**
     * Thiết lập và kết nối với các plugin phụ thuộc.
     */
    private void setupIntegrations() {
        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                getLogger().info("Da tich hop thanh cong voi Vault (Economy).");
            } else {
                getLogger().severe("!!! Khong tim thay plugin Economy (EssentialsX, etc.)! Chuc nang shop se bi vo hieu hoa.");
            }
        } else {
             getLogger().severe("!!! Khong tim thay Vault! Chuc nang shop se bi vo hieu hoa.");
        }

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Da tich hop thanh cong voi PlaceholderAPI.");
        }
    }

    // --- CÁC GETTER ĐỂ TRUY CẬP TỪ CÁC CLASS KHÁC ---

    public static TienGioiPre getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public RealmManager getRealmManager() { return realmManager; }
    public CultivationManager getCultivationManager() { return cultivationManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
    public TongMonManager getTongMonManager() { return this.tongMonManager; }
    public static TienGioiAPI getAPI() {
        if (instance == null) return null;
        return new TienGioiAPI(instance);
    }
}