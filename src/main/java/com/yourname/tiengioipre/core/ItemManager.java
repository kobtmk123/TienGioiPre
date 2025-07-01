package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class ItemManager {

    private final TienGioiPre plugin;
    public final NamespacedKey ITEM_ID_KEY;
    public final NamespacedKey ITEM_TIER_KEY;

    public ItemManager(TienGioiPre plugin) {
        this.plugin = plugin;
        this.ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
        this.ITEM_TIER_KEY = new NamespacedKey(plugin, "item_tier");
    }

    /**
     * Tạo một ItemStack từ cấu hình trong config.yml.
     * @param id ID của nhóm vật phẩm (ví dụ: "cuonlinhkhi") hoặc ID của vật phẩm đơn lẻ (ví dụ: "phoi_thien_tinh").
     * @param tier Tier của vật phẩm (ví dụ: "ha", "trung", hoặc "default" cho item không có tier).
     * @return ItemStack đã được tạo, hoặc null nếu không tìm thấy.
     */
    public ItemStack createCultivationItem(String id, String tier) {
        // Đường dẫn đến mục cha của vật phẩm trong config
        String basePath = "items." + id;
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection(basePath);
        if (itemSection == null) return null;

        // Kiểm tra xem item này có các tier con hay không (như cuonlinhkhi)
        // Nếu có tier con và tier được yêu cầu tồn tại, thì lấy section của tier con đó
        if (itemSection.isConfigurationSection(tier)) {
            itemSection = itemSection.getConfigurationSection(tier);
        }
        // Nếu không (ví dụ: phoi_thien_tinh, tier="default"), thì itemSection vẫn là basePath,
        // nó sẽ đọc trực tiếp các thuộc tính display-name, lore, material từ đó.
        
        if (itemSection == null) return null;

        // Tạo vật phẩm
        Material material = Material.matchMaterial(itemSection.getString("material", "PAPER").toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Vật liệu không hợp lệ: " + itemSection.getString("material") + " cho vật phẩm " + id);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(format(itemSection.getString("display-name", "Vật phẩm không tên")));
        
        List<String> lore = itemSection.getStringList("lore");
        meta.setLore(lore.stream().map(this::format).collect(Collectors.toList()));
        
        if (itemSection.getBoolean("enchanted", false)) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Lưu trữ NBT data để nhận biết vật phẩm
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(ITEM_TIER_KEY, PersistentDataType.STRING, tier);

        item.setItemMeta(meta);
        return item;
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}