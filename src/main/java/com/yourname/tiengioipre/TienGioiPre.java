package com.yourname.tiengioipre;

import com.yourname.tiengioipre.api.PAPIExpansion;
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

    // Biến instance tĩnh để truy cập plugin từ bất kỳ đâu
    private static TienGioiPre instance;
    private static Economy econ = null;

    // Các trình quản lý (Manager) cho từng chức năng của plugin
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

        // Tải các file cấu hình
        saveDefaultConfig();
        // Bạn nên tạo một class riêng để quản lý messages.yml cho tối ưu

        // Khởi tạo tất cả các trình quản lý
        this.playerDataManager = new PlayerDataManager(this);
        this.realmManager = new RealmManager(this);
        this.cultivationManager = new CultivationManager(this);
        this.itemManager = new ItemManager(this);
        this.shopGUI = new ShopGUI(this);

        // Đăng ký lệnh và sự kiện
        registerCommands();
        registerListeners();

        // Tích hợp với các plugin khác
        if (!setupEconomy()) {
            getLogger().severe("!!! Khong tim thay Vault hoac plugin Economy! Chuc nang shop se bi vo hieu hoa.");
        } else {
            getLogger().info("Da tich hop thanh cong voi Vault (Economy).");
        }

        setupPlaceholderAPI();

        // Bắt đầu các task lặp lại (ví dụ: task cộng linh khí khi tu luyện)
        cultivationManager.startCultivationTask();

        getLogger().info("Plugin TienGioiPre da duoc bat thanh cong!");
        getLogger().info("------------------------------------");
    }

    @Override
    public void onDisable() {
        // Lưu dữ liệu người chơi trước khi tắt server để tránh mất mát
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
        getCommand("tiengioi").setTabCompleter(mainCommand); // Đăng ký TabCompleter cho lệnh admin

        getCommand("tuluyen").setExecutor(new TuLuyenCommand(this));
        getCommand("dotpha").setExecutor(new DotPhaCommand(this));
        getCommand("shoptiengioi").setExecutor(new ShopTienGioiCommand(this));
    }

    /**
     * Đăng ký tất cả các sự kiện (listeners) của plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
    }

    /**
     * Thiết lập và kết nối với plugin Vault để sử dụng API Economy.
     * @return true nếu kết nối thành công, ngược lại false.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * Thiết lập và kết nối với plugin PlaceholderAPI.
     */
    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Da tich hop thanh cong voi PlaceholderAPI.");
        } else {
            getLogger().warning("Khong tim thay PlaceholderAPI, cac placeholder se khong hoat dong.");
        }
    }

    // --- CÁC GETTER ĐỂ TRUY CẬP TỪ CÁC CLASS KHÁC ---

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
}    // ... các getter khác ...

    /**
     * Lấy instance của API để các plugin khác sử dụng.
     * @return một instance của TienGioiAPI.
     */
    public static TienGioiAPI getAPI() {
        // Chúng ta tạo một instance mới mỗi lần gọi để đảm bảo an toàn,
        // hoặc bạn có thể tạo một biến private final TienGioiAPI api; và khởi tạo trong onEnable().
        if (instance == null) {
            return null; // Plugin chưa được bật
        }
        return new TienGioiAPI(instance);
    }
}