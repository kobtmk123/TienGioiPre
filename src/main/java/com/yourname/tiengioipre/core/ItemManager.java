package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Quản lý việc tạo các ItemStack tùy chỉnh từ file config.yml.
 * Đây là nơi tập trung logic để đọc thông tin vật phẩm và xây dựng chúng.
 */
public class ItemManager {

    private final TienGioiPre plugin;
    public final NamespacedKey ITEM_ID_KEY;
    public final NamespacedKey ITEM_TIER_KEY;
    public final NamespacedKey HALF_MADE_PILL_KEY; // Key để đánh dấu đan dược nửa mùa

    public ItemManager(TienGioiPre plugin) {
        this.plugin = plugin;
        this.ITEM_ID_KEY = new NamespacedKey(plugin, "tiengioi_item_id");
        this.ITEM_TIER_KEY = new NamespacedKey(plugin, "tiengioi_item_tier");
        this.HALF_MADE_PILL_KEY = new NamespacedKey(plugin, "tiengioi_half_made_pill");
    }

    /**
     * Tạo một ItemStack từ cấu hình trong config.yml.
     * Hàm này đủ thông minh để xử lý cả item có nhiều tier và item không có tier.
     * @param id ID của nhóm vật phẩm (ví dụ: "cuonlinhkhi") hoặc ID của vật phẩm đơn lẻ (ví dụ: "phoi_thien_tinh").
     * @param tier Tier của vật phẩm (ví dụ: "ha", "trung", hoặc "default" cho item không có tier).
     * @return ItemStack đã được tạo, hoặc null nếu không tìm thấy.
     */
    public ItemStack createCultivationItem(String id, String tier) {
        // Đường dẫn đến mục cha của vật phẩm trong config
        String basePath = "items." + id;
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection(basePath);
        
        // Nếu không tìm thấy mục cha, thử tìm trong mục dược liệu 'alchemy.herbs'
        if (itemSection == null) {
            basePath = "alchemy.herbs." + id;
            itemSection = plugin.getConfig().getConfigurationSection(basePath);
        }
        
        if (itemSection == null) {
            plugin.getLogger().warning("Không tìm thấy cấu hình vật phẩm cho ID: " + id);
            return null;
        }

        // Kiểm tra xem item này có các tier con hay không (như cuonlinhkhi)
        // Nếu có tier con và tier được yêu cầu tồn tại, thì lấy section của tier con đó
        if (itemSection.isConfigurationSection(tier)) {
            itemSection = itemSection.getConfigurationSection(tier);
        }
        // Nếu không, itemSection vẫn là basePath, nó sẽ đọc trực tiếp các thuộc tính từ đó.
        // Điều này cho phép các vật phẩm như "phoi_thien_tinh" không cần có tầng "default:".
        
        if (itemSection == null) {
            plugin.getLogger().warning("Không tìm thấy tier '" + tier + "' cho vật phẩm có ID: " + id);
            return null;
        }

        // Tạo vật phẩm
        Material material = Material.matchMaterial(itemSection.getString("material", "PAPER").toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Vật liệu không hợp lệ: '" + itemSection.getString("material") + "' cho vật phẩm " + id);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set tên và lore, hỗ trợ cả mã màu RGB
        meta.setDisplayName(format(itemSection.getString("display-name", "Vật phẩm không tên")));
        List<String> lore = itemSection.getStringList("lore");
        meta.setLore(lore.stream().map(this::format).collect(Collectors.toList()));
        
        // Thêm hiệu ứng enchant nếu được cấu hình
        if (itemSection.getBoolean("enchanted", false)) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Lưu trữ NBT data để nhận biết vật phẩm trong các listener
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(ITEM_TIER_KEY, PersistentDataType.STRING, tier);

        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Định dạng màu cho tin nhắn, hỗ trợ cả mã màu & Và RGB (&#RRGGBB).
     */
    private String format(String message) {
        if (message == null) return "";
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color.substring(1)) + "");
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}