package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FurnaceRefinePillListener implements Listener {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;

    public FurnaceRefinePillListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    // Sự kiện này được kích hoạt ngay trước khi một vật phẩm được nung xong
    @EventHandler
    public void onPillSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource(); // Vật phẩm đang được nung
        if (source == null || !source.hasItemMeta()) return;

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        // Kiểm tra xem đây có phải là Đan Dược Nửa Mùa không
        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            return;
        }

        // Lấy thông tin về đan dược từ NBT
        String pillData = container.get(semiPillKey, PersistentDataType.STRING); // Ví dụ: "luyen_khi_dan:nhat_pham"
        String[] parts = pillData.split(":");
        if (parts.length < 2) return;

        String pillId = parts[0];
        String tierId = parts[1];

        // Tạo ra viên Đan Dược hoàn chỉnh
        ItemStack finalPill = createFinalPill(pillId, tierId);
        
        // Thay thế kết quả nung mặc định bằng viên đan dược của chúng ta
        if (finalPill != null) {
            event.setResult(finalPill);
        }
    }

    // Sự kiện này kích hoạt khi lò bắt đầu cháy, dùng để set thời gian nung
    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        // Lấy item trong ô nguyên liệu của lò
        ItemStack source = event.getBlock().getState() instanceof org.bukkit.block.Furnace ? 
                           ((org.bukkit.block.Furnace) event.getBlock().getState()).getInventory().getSmelting() : null;

        if (source == null || !source.hasItemMeta()) return;

        // Nếu là Đan Dược Nửa Mùa, đặt thời gian nung theo config
        if (source.getItemMeta().getPersistentDataContainer().has(semiPillKey, PersistentDataType.STRING)) {
            int cookTime = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20) * 20;
            event.setBurnTime(cookTime);
        }
    }

    /**
     * Tạo ra một ItemStack Đan Dược hoàn chỉnh.
     * @param pillId ID của loại đan, ví dụ: "luyen_khi_dan"
     * @param tierId ID của phẩm chất, ví dụ: "nhat_pham"
     * @return ItemStack Đan Dược hoàn chỉnh.
     */
    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) return null;

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        // Lấy bonus và tạo lore
        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        // Thêm hiệu ứng phát sáng
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Lưu NBT để DotPhaCommand nhận biết và sử dụng
        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        return pill;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}