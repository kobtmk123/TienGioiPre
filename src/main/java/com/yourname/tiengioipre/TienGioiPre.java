package com.yourname.tiengioipre;

import com.yourname.tiengioipre.api.PAPIExpansion;
import com.yourname.tiengioipre.api.TienGioiAPI;
import com.yourname.tiengioipre.commands.*;
import com.yourname.tiengioipre.core.*;
import com.yourname.tiengioipre.gui.ShopGUI;
import com.yourname.tiengioipre.listeners.*;
import com.yourname.tiengioipre.tasks.CultivationTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TienGioiPre extends JavaPlugin {

    private static TienGioiPre instance;
    private static Economy econ = null;

    private PlayerDataManager playerDataManager;
    private RealmManager realmManager;
    private CultivationManager cultivationManager;
    private ItemManager itemManager;
    private ShopGUI shopGUI;
    private TongMonManager tongMonManager;
    private BukkitTask mainTask; 

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("------------------------------------");
        getLogger().info("Dang khoi dong plugin TienGioiPre...");
        saveDefaultConfig();

        this.itemManager = new ItemManager(this);
        this.shopGUI = new ShopGUI(this);
        this.tongMonManager = new TongMonManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.realmManager = new RealmManager(this);
        this.cultivationManager = new CultivationManager(this);

        registerCommands();
        registerListeners();
        setupIntegrations();
        
        this.mainTask = new CultivationTask(this).runTaskTimer(this, 0L, 20L);
        getLogger().info("Da khoi dong Task Tu Luyen chinh.");

        getLogger().info("Plugin TienGioiPre da duoc bat thanh cong!");
        getLogger().info("------------------------------------");
    }

    @Override
    public void onDisable() {
        if (this.mainTask != null && !this.mainTask.isCancelled()) {
            this.mainTask.cancel();
        }
        if (tongMonManager != null) {
            tongMonManager.saveTongMonData();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        getLogger().info("Plugin TienGioiPre da duoc tat.");
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

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilRefineListener(this), this);
        getServer().getPluginManager().registerEvents(new OreSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new TongMonListener(this), this);
        
        // === ĐĂNG KÝ CÁC LISTENER MỚI CHO HỆ THỐNG LUYỆN ĐAN ===
        getServer().getPluginManager().registerEvents(new HerbDropListener(this), this);
        getServer().getPluginManager().registerEvents(new CauldronAlchemyListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnaceRefinePillListener(this), this);
    }

    private void setupIntegrations() {
        // ... (Giữ nguyên)
    }

    // --- CÁC GETTER ---
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