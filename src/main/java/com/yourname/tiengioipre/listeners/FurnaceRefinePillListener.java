package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
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
    @EventHandler
    public void onPillSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource(); // Vật phẩm đang được nung
        if (source == null || !source.hasItemMeta()) return;

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        // Kiểm tra xem đây có phải là Đan Dược Nửa Mùa không bằng NBT tag
        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            return;
        }

        // Lấy thông tin về đan dược từ NBT (ví dụ: "luyen_khi_dan:nhat_pham")
        String pillData = container.get(semiPillKey, PersistentDataType.STRING);
        String[] parts = pillData.split(":");
        if (parts.length < 2) return;

        String pillId = parts[0];
        String tierId = parts[1];

        // Tạo ra viên Đan Dược hoàn chỉnh
        ItemStack finalPill = createFinalPill(pillId, tierId);
        
        // Thay thế kết quả nung mặc định (thường là không có gì) bằng viên đan dược của chúng ta
        if (finalPill != null) {
            event.setResult(finalPill);
        }
    }

    /**
     * Sự kiện này kích hoạt khi lò bắt đầu cháy (đốt nhiên liệu).
     * Chúng ta dùng nó để đặt thời gian nung tùy chỉnh.
     */
    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        // Lấy item trong ô nguyên liệu của lò (ô trên cùng)
        // Cần kiểm tra xem block có phải là Furnace không để tránh lỗi
        if (!(event.getBlock().getState() instanceof Furnace)) return;
        
        Furnace furnace = (Furnace) event.getBlock().getState();
        ItemStack source = furnace.getInventory().getSmelting();

        if (source == null || !source.hasItemMeta()) return;

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        // Nếu là Đan Dược Nửa Mùa, đặt thời gian nung theo config
        if (container.has(semiPillKey, PersistentDataType.STRING)) {
            // Lấy thời gian từ config (giây) và chuyển sang tick (1 giây = 20 ticks)
            int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20);
            event.setBurnTime(cookTimeInSeconds * 20);
            
            // Đặt CanBurn là true để đảm bảo lò cháy
            event.setBurning(true);
        }
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
            plugin.getLogger().warning("Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM); // Ngoại hình là Kem Dung Nham
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        // Lấy bonus và tạo lore
        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            // Thay thế placeholder %bonus% bằng giá trị thực
            lore.add(format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        // Thêm hiệu ứng phát sáng
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // QUAN TRỌNG: Lưu NBT tag vào item hoàn chỉnh
        // Tag này sẽ được DotPhaCommand đọc để cộng tỷ lệ thành công
        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        return pill;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}