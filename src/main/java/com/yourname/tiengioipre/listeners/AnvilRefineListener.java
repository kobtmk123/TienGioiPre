package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AnvilRefineListener implements Listener {
    private final TienGioiPre plugin;
    private final Random random = new Random();

    public AnvilRefineListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        if (!(event.getView().getPlayer() instanceof Player)) return;

        Player player = (Player) event.getView().getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        // 1. Kiểm tra xem người chơi có phải là Luyện Khí Sư không
        if (data == null || !"luyenkhisu".equals(data.getCultivationPath())) {
            return;
        }

        ItemStack itemToUpgrade = inv.getItem(0);
        ItemStack catalyst = inv.getItem(1); // Phôi

        // 2. Kiểm tra xem có đủ item và phôi không
        if (itemToUpgrade == null || catalyst == null || !catalyst.hasItemMeta()) return;

        ItemMeta catalystMeta = catalyst.getItemMeta();
        if (catalystMeta == null) return;
        
        // 3. Kiểm tra xem vật phẩm thứ 2 có phải là Phôi hợp lệ không
        String catalystId = catalystMeta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        if (catalystId == null || !catalystId.startsWith("phoi_")) {
            return;
        }

        // 4. Kiểm tra xem loại vật phẩm có phù hợp với loại phôi không
        String requiredType = plugin.getConfig().getString("refining.catalysts." + catalystId + ".type");
        if (!isItemTypeMatch(itemToUpgrade.getType(), requiredType)) {
            return;
        }

        // 5. Random ra cấp bậc mới dựa trên nhóm tỷ lệ của phôi
        String chanceGroup = plugin.getConfig().getString("refining.catalysts." + catalystId + ".chance-group", "default");
        String chosenTierId = getRandomTier(chanceGroup);
        if (chosenTierId == null) return;

        // 6. Tạo ra vật phẩm kết quả
        ItemStack resultItem = createRefinedItem(itemToUpgrade, chosenTierId, player);
        if (resultItem == null) return;
        
        event.setResult(resultItem);
        
        // Dùng Bukkit Scheduler để set giá sửa chữa, tránh lỗi client/server không đồng bộ
        plugin.getServer().getScheduler().runTask(plugin, () -> inv.setRepairCost(5)); // Set giá 5 kinh nghiệm
    }

    /**
     * Random ra một cấp bậc dựa trên nhóm tỷ lệ được cung cấp.
     */
    private String getRandomTier(String chanceGroup) {
        ConfigurationSection chanceSection = plugin.getConfig().getConfigurationSection("refining.chances." + chanceGroup);
        if (chanceSection == null) return null;

        int totalWeight = chanceSection.getKeys(false).stream().mapToInt(chanceSection::getInt).sum();
        if (totalWeight <= 0) return null;

        int randomNum = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (String key : chanceSection.getKeys(false)) {
            currentWeight += chanceSection.getInt(key);
            if (randomNum < currentWeight) {
                return key;
            }
        }
        return null; // Trường hợp hiếm gặp
    }
    
    /**
     * Tạo ra ItemStack hoàn chỉnh sau khi đã rèn.
     */
    private ItemStack createRefinedItem(ItemStack originalItem, String tierId, Player crafter) {
        ItemStack newItem = originalItem.clone();
        ItemMeta meta = newItem.getItemMeta();
        if (meta == null) return null;

        ConfigurationSection tierConfig = plugin.getConfig().getConfigurationSection("refining.tiers." + tierId);
        if (tierConfig == null) return null;

        // Xóa các enchant và lore cũ nếu có để làm mới
        meta.getEnchants().keySet().forEach(meta::removeEnchant);
        
        // Set tên mới với màu sắc riêng cho từng cấp bậc
        String tierDisplayName = format(tierConfig.getString("display-name", ""));
        String nameColor = format(tierConfig.getString("name-color", "&f"));
        meta.setDisplayName(tierDisplayName + " " + nameColor + getCleanItemName(originalItem));
        
        // Tạo lore mới với các chỉ số ngẫu nhiên
        List<String> newLore = new ArrayList<>();
        String itemCategory = getItemCategory(originalItem.getType());
        ConfigurationSection attributesSection = plugin.getConfig().getConfigurationSection("refining.attributes." + itemCategory);
        
        if (attributesSection != null) {
            for (String attrKey : attributesSection.getKeys(false)) {
                ConfigurationSection attrConfig = attributesSection.getConfigurationSection(attrKey);
                // Chỉ thêm chỉ số nếu cấp bậc hiện tại có định nghĩa cho chỉ số đó
                if (attrConfig != null && attrConfig.contains(tierId)) {
                    double value = getRandomInRange(attrConfig.getString(tierId));
                    if (value > 0) {
                        String display = format(attrConfig.getString("display", "")).replace("%value%", String.format("%.1f", value));
                        newLore.add(display);
                    }
                }
            }
        }
        
        // Thêm tên người rèn vào cuối lore
        newLore.add("");
        newLore.add(format("&8Chế tác bởi: &7" + crafter.getName()));
        
        meta.setLore(newLore);
        
        // Thêm hiệu ứng phát sáng
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        
        // Lưu trữ NBT để plugin nhận biết đây là đồ đã rèn và cấp bậc của nó
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "refined_tier"), PersistentDataType.STRING, tierId);

        newItem.setItemMeta(meta);
        return newItem;
    }

    /**
     * Phân loại vật phẩm thành "weapon", "armor", "tool".
     */
    private String getItemCategory(Material material) {
        String name = material.name().toLowerCase();
        if (name.contains("sword") || name.contains("axe") || name.contains("bow") || name.contains("crossbow") || name.contains("trident")) return "weapon";
        if (name.contains("helmet") || name.contains("chestplate") || name.contains("leggings") || name.contains("boots")) return "armor";
        if (name.contains("pickaxe") || name.contains("shovel") || name.contains("hoe") || name.contains("shears") || name.contains("flint_and_steel")) return "tool";
        return "unknown";
    }

    /**
     * Kiểm tra xem loại vật phẩm có khớp với loại yêu cầu của phôi không.
     */
    private boolean isItemTypeMatch(Material material, String requiredType) {
        if (requiredType == null) return false;
        String itemCategory = getItemCategory(material);
        
        switch (requiredType) {
            case "armor_weapon": return itemCategory.equals("armor") || itemCategory.equals("weapon");
            case "tool": return itemCategory.equals("tool");
            default: return false;
        }
    }
    
    /**
     * Lấy một số ngẫu nhiên trong một khoảng cho trước (ví dụ: "1.0-1.5").
     */
    private double getRandomInRange(String range) {
        if (range == null || !range.contains("-")) return 0;
        try {
            String[] parts = range.split("-");
            double min = Double.parseDouble(parts[0]);
            double max = Double.parseDouble(parts[1]);
            return ThreadLocalRandom.current().nextDouble(min, max);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Lấy tên sạch của vật phẩm, loại bỏ các mã màu và cấp bậc cũ.
     */
    private String getCleanItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && !item.getItemMeta().getDisplayName().isEmpty()) {
             return ChatColor.stripColor(item.getItemMeta().getDisplayName()).replaceAll("\\[(.*?)\\]", "").trim();
        }
        return capitalize(item.getType().name().replace('_', ' ').toLowerCase());
    }
    
    /**
     * Viết hoa chữ cái đầu của mỗi từ.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Arrays.stream(str.split(" "))
                     .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                     .collect(Collectors.joining(" "));
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}