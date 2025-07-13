package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.utils.DebugLogger; // <-- IMPORT MỚI
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block; // <-- IMPORT ĐÃ ĐƯỢC THÊM
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý logic nung "Đan Dược Nửa Mùa" trong lò để tạo ra Đan Dược hoàn chỉnh.
 */
public class FurnaceRefinePillListener implements Listener {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;

    public FurnaceRefinePillListener(TienGioiPre plugin) {
        this.plugin = plugin;
        // Key để nhận biết Đan Dược Nửa Mùa
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        // Key để lưu trữ bonus tỷ lệ vào Đan Dược hoàn chỉnh
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    /**
     * Sự kiện này được kích hoạt ngay trước khi một vật phẩm được nung xong.
     * Chúng ta sẽ can thiệp vào đây để thay đổi kết quả nung.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPillSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource(); // Vật phẩm đang được nung
        if (source == null || !source.hasItemMeta()) {
            DebugLogger.log("FurnaceRefine", "onPillSmelt: Source item is null or has no meta. -> No Action");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            DebugLogger.log("FurnaceRefine", "onPillSmelt: Source item is NOT a semi-finished pill (no NBT). -> No Action");
            return;
        }
        DebugLogger.log("FurnaceRefine", "onPillSmelt: Detected SEMI-FINISHED PILL in smelt slot!");

        String pillData = container.get(semiPillKey, PersistentDataType.STRING);
        DebugLogger.log("FurnaceRefine", "onPillSmelt: Pill NBT data: " + pillData);

        String[] parts = pillData.split(":");
        if (parts.length < 2) {
            DebugLogger.warn("FurnaceRefine", "onPillSmelt: Invalid pill data format: " + pillData);
            return;
        }

        String pillId = parts[0];
        String tierId = parts[1];
        DebugLogger.log("FurnaceRefine", "onPillSmelt: Processing pill: ID=" + pillId + ", Tier=" + tierId);

        ItemStack finalPill = createFinalPill(pillId, tierId);
        
        if (finalPill != null) {
            event.setResult(finalPill);
            DebugLogger.log("FurnaceRefine", "onPillSmelt: Successfully set final pill result: " + finalPill.getItemMeta().getDisplayName());
        } else {
            DebugLogger.warn("FurnaceRefine", "onPillSmelt: createFinalPill returned null for " + pillId + ":" + tierId);
        }
    }

    /**
     * Sự kiện này kích hoạt khi lò bắt đầu cháy (đốt nhiên liệu).
     * Chúng ta dùng nó để đặt thời gian nung tùy chỉnh.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Furnace)) {
            DebugLogger.log("FurnaceRefine", "onFurnaceBurn: Block is not a furnace. -> No Action");
            return;
        }
        
        Furnace furnace = (Furnace) block.getState();
        ItemStack source = furnace.getInventory().getSmelting();

        if (source == null || !source.hasItemMeta()) {
            DebugLogger.log("FurnaceRefine", "onFurnaceBurn: Smelting item is null or has no meta. -> No Action");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            DebugLogger.log("FurnaceRefine", "onFurnaceBurn: Smelting item is NOT a semi-finished pill (no NBT). -> No Action");
            return;
        }
        DebugLogger.log("FurnaceRefine", "onFurnaceBurn: Detected SEMI-FINISHED PILL in furnace! Setting custom burn time.");

        int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20);
        
        event.setBurnTime(cookTimeInSeconds * 20); // Thời gian đốt nhiên liệu
        
        // Củng cố lò nung bằng cách đặt lại thời gian nung và cập nhật trạng thái
        furnace.setCookTime((short) 0); // Đặt lại cook time về 0
        furnace.setCookTimeTotal((short)(cookTimeInSeconds * 20)); // Ép tổng thời gian nấu
        furnace.update(true); // Cập nhật trạng thái lò ngay lập tức
        
        event.setBurning(true); // Đảm bảo event báo lò đang cháy
        
        DebugLogger.log("FurnaceRefine", "onFurnaceBurn: Custom burn time set to " + cookTimeInSeconds + " seconds for " + source.getItemMeta().getDisplayName());
    }

    /**
     * Tạo ra một ItemStack Đan Dược hoàn chỉnh dựa trên ID và phẩm chất.
     * @param pillId ID của loại đan, ví dụ: "luyen_khi_dan"
     * @param tierId ID của phẩm chất, ví dụ: "nhat_pham"
     * @return ItemStack Đan Dược hoàn chỉnh, hoặc null nếu có lỗi.
     */
    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            DebugLogger.warn("FurnaceRefine", "createFinalPill: Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM); // Ngoại hình là Kem Dung Nham
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        DebugLogger.log("FurnaceRefine", "createFinalPill: Final pill created with NBT bonus: " + bonus);
        return pill;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}