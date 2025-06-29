package com.yourname.tiengioipre.gui;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final ItemManager itemManager;

    public ShopGUI(TienGioiPre plugin) {
        this.plugin = plugin;
        this.itemManager = new ItemManager(plugin); // Hoặc lấy instance từ main class
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("shop.title", "&8Shop"));
        int size = plugin.getConfig().getInt("shop.size", 27);
        Inventory shopInv = Bukkit.createInventory(null, size, title);

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("shop.items");
        if (itemsSection == null) {
            player.sendMessage("§cShop chưa được cấu hình!");
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection shopItemSection = itemsSection.getConfigurationSection(key);
            if (shopItemSection == null) continue;

            String itemId = shopItemSection.getString("item-id");
            String itemTier = shopItemSection.getString("item-tier");
            int slot = shopItemSection.getInt("slot");
            double price = shopItemSection.getDouble("price");

            ItemStack item = itemManager.createCultivationItem(itemId, itemTier);
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                List<String> extraLore = shopItemSection.getStringList("extra-lore");
                for (String line : extraLore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%price%", String.valueOf(price))));
                }
                meta.setLore(lore);
                
                // Lưu giá vào item để ShopListener có thể đọc
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_price"), PersistentDataType.DOUBLE, price);
                
                item.setItemMeta(meta);
            }

            shopInv.setItem(slot, item);
        }

        player.openInventory(shopInv);
    }
}