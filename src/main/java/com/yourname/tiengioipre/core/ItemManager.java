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

    public ItemStack createCultivationItem(String id, String tier) {
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("items." + id + "." + tier);
        if (itemSection == null) {
            return null;
        }

        String materialName = itemSection.getString("material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Vật liệu không hợp lệ '" + materialName + "' cho vật phẩm " + id + ":" + tier);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = itemSection.getString("display-name", "Vật phẩm không tên");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            List<String> lore = itemSection.getStringList("lore");
            meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));

            if (itemSection.getBoolean("enchanted", false)) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            // Lưu trữ dữ liệu tùy chỉnh vào vật phẩm (rất quan trọng)
            meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(ITEM_TIER_KEY, PersistentDataType.STRING, tier);

            item.setItemMeta(meta);
        }
        return item;
    }
}