package com.myname.tiengioipre.gui;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.core.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI {
    private final TienGioiPre plugin;
    public static final NamespacedKey SHOP_ITEM_TAG = new NamespacedKey(TienGioiPre.getInstance(), "shop_item_tag");
    public static final NamespacedKey SHOP_PRICE_TAG = new NamespacedKey(TienGioiPre.getInstance(), "shop_price_tag");


    public ShopGUI(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = format(plugin.getConfig().getString("shop.title", "&8Shop"));
        int size = plugin.getConfig().getInt("shop.size", 27);
        Inventory shopInv = Bukkit.createInventory(null, size, title);

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("shop.items");
        if (itemsSection == null) {
            player.sendMessage(format("&c[Lỗi] Shop chưa được cấu hình trong config.yml."));
            return;
        }

        ItemManager itemManager = plugin.getItemManager();

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection shopItemConfig = itemsSection.getConfigurationSection(key);
            if (shopItemConfig == null) continue;

            String itemId = shopItemConfig.getString("item-id");
            String itemTier = shopItemConfig.getString("item-tier");
            int slot = shopItemConfig.getInt("slot", 0);
            double price = shopItemConfig.getDouble("price", 0);

            ItemStack displayItem = itemManager.createCultivationItem(itemId, itemTier);
            if (displayItem == null) {
                plugin.getLogger().warning("Khong the tao vat pham cho shop: " + itemId + ":" + itemTier);
                continue;
            }

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                List<String> extraLore = shopItemConfig.getStringList("extra-lore");
                for (String line : extraLore) {
                    lore.add(format(line.replace("%price%", String.format("%,.0f", price))));
                }
                meta.setLore(lore);
                
                // Gắn dữ liệu ẩn vào item để Listener nhận biết
                // Đây là bước quan trọng nhất
                meta.getPersistentDataContainer().set(SHOP_ITEM_TAG, PersistentDataType.BYTE, (byte) 1);
                meta.getPersistentDataContainer().set(SHOP_PRICE_TAG, PersistentDataType.DOUBLE, price);
                
                // Gắn cả ID gốc để Listener có thể tạo lại item sau khi mua
                meta.getPersistentDataContainer().set(itemManager.ITEM_ID_KEY, PersistentDataType.STRING, itemId);
                meta.getPersistentDataContainer().set(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING, itemTier);
                
                displayItem.setItemMeta(meta);
            }
            shopInv.setItem(slot, displayItem);
        }
        player.openInventory(shopInv);
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}