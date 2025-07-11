package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lắng nghe các sự kiện của lò nung để xử lý việc hoàn thiện Đan Dược.
 */
public class FurnaceRefinePillListener implements Listener {

    private final TienGioiPre plugin;
    private final NamespacedKey halfMadePillKey;

    public FurnaceRefinePillListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.halfMadePillKey = new NamespacedKey(plugin, "half_made_pill");
    }

    /**
     * Sự kiện này được gọi khi nhiên liệu bắt đầu cháy để nung một vật phẩm.
     * Chúng ta dùng nó để đặt thời gian nung tùy chỉnh.
     */
    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack smeltingItem = event.getBlock().getState() instanceof org.bukkit.block.Furnace ? 
                                 ((org.bukkit.block.Furnace) event.getBlock().getState()).getInventory().getSmelting() : null;

        if (smeltingItem == null || !smeltingItem.hasItemMeta()) return;

        // Kiểm tra xem có phải là đan dược nửa mùa không bằng NBT
        if (smeltingItem.getItemMeta().getPersistentDataContainer().has(halfMadePillKey, PersistentDataType.BYTE)) {
            int cookTime = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20) * 20; // Chuyển sang tick
            event.setCookTime(cookTime);
        }
    }

    /**
     * Sự kiện này được gọi khi vật phẩm đã được nung xong.
     * Chúng ta can thiệp vào kết quả để tạo ra viên Đan Dược hoàn chỉnh.
     */
    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack sourceItem = event.getSource();
        if (sourceItem == null || !sourceItem.hasItemMeta()) return;

        ItemMeta sourceMeta = sourceItem.getItemMeta();
        if (sourceMeta == null || !sourceMeta.getPersistentDataContainer().has(halfMadePillKey, PersistentDataType.BYTE)) {
            return; // Không phải đan dược nửa mùa
        }
        
        // Lấy thông tin đã lưu từ viên đan dược nửa mùa
        String pillId = sourceMeta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        String pillTier = sourceMeta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_TIER_KEY, PersistentDataType.STRING);

        if (pillId == null || pillTier == null) return;
        
        // Tạo viên đan dược hoàn chỉnh
        ItemStack finalPill = createFinalPill(pillId, pillTier);
        if (finalPill != null) {
            event.setResult(finalPill);
        }
    }

    /**
     * Tạo ra ItemStack của viên Đan Dược hoàn chỉnh từ config.
     */
    private ItemStack createFinalPill(String pillId, String pillTier) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + pillTier);
        if (pillConfig == null) {
            plugin.getLogger().warning("Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + pillTier);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM); // Ngoại hình là Kem Dung Nham
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return pill;
        
        // Lấy thông tin từ config
        String displayName = format(pillConfig.getString("display-name", "Đan Dược Lỗi"));
        int bonus = pillConfig.getInt("success-chance-bonus", 0);

        // Thay thế placeholder trong lore
        List<String> lore = pillConfig.getStringList("lore").stream()
                .map(line -> format(line.replace("%bonus%", String.valueOf(bonus))))
                .collect(Collectors.toList());

        meta.setDisplayName(displayName);
        meta.setLore(lore);
        
        // Thêm hiệu ứng enchant
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Lưu NBT để nhận biết đây là đan dược hoàn chỉnh
        meta.getPersistentDataContainer().set(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING, pillId);
        meta.getPersistentDataContainer().set(plugin.getItemManager().ITEM_TIER_KEY, PersistentDataType.STRING, pillTier);

        pill.setItemMeta(meta);
        return pill;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}