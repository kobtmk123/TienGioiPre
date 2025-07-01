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

import java.util.ArrayList;
import java.util.Arrays;      // <-- IMPORT ĐÃ ĐƯỢC THÊM
import java.util.List;
import java.util.Random;
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

        if (data == null || !"luyenkhisu".equals(data.getCultivationPath())) {
            return;
        }

        ItemStack itemToUpgrade = inv.getItem(0);
        ItemStack catalyst = inv.getItem(1);

        if (itemToUpgrade == null || catalyst == null || !catalyst.hasItemMeta()) return;

        ItemMeta catalystMeta = catalyst.getItemMeta();
        if (catalystMeta == null) return;
        
        String catalystId = catalystMeta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);

        if (catalystId == null || !catalystId.startsWith("phoi_")) {
            return;
        }

        String requiredType = plugin.getConfig().getString("refining.catalysts." + catalystId);
        if (!isItemTypeMatch(itemToUpgrade.getType(), requiredType)) {
            return;
        }

        String chosenTierId = getRandomTier(catalystId);
        if (chosenTierId == null) return;

        ItemStack resultItem = createRefinedItem(itemToUpgrade, chosenTierId);
        if (resultItem == null) return;
        
        event.setResult(resultItem);
        plugin.getServer().getScheduler().runTask(plugin, () -> inv.setRepairCost(5));
    }

    private String getRandomTier(String catalystId) {
        ConfigurationSection chanceSection = plugin.getConfig().getConfigurationSection("refining.chances." + catalystId);
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
        return null;
    }
    
    private ItemStack createRefinedItem(ItemStack originalItem, String tierId) {
        ItemStack newItem = originalItem.clone();
        ItemMeta meta = newItem.getItemMeta();
        if (meta == null) return null;

        ConfigurationSection tierConfig = plugin.getConfig().getConfigurationSection("refining.tiers." + tierId);
        if (tierConfig == null) return null;

        String tierDisplayName = format(tierConfig.getString("display-name", ""));
        String tierColor = format(tierConfig.getString("color", "&f"));
        meta.setDisplayName(tierDisplayName + " " + tierColor + getCleanItemName(originalItem));

        List<String> newLore = new ArrayList<>();
        double damageBonus = getRandomInRange(tierConfig.getString("damage-range"));
        double healthBonus = getRandomInRange(tierConfig.getString("health-range"));

        if (damageBonus > 0) newLore.add(format("&a Sát thương: &f+" + String.format("%.1f", damageBonus)));
        if (healthBonus > 0) newLore.add(format("&c Máu: &f+" + String.format("%.1f", healthBonus)));
        
        meta.setLore(newLore);
        
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "refined_tier"), PersistentDataType.STRING, tierId);

        newItem.setItemMeta(meta);
        return newItem;
    }

    private boolean isItemTypeMatch(Material material, String requiredType) {
        if (requiredType == null) return false;
        String matName = material.name().toLowerCase();
        
        boolean isArmor = matName.contains("helmet") || matName.contains("chestplate") || matName.contains("leggings") || matName.contains("boots");
        boolean isWeapon = matName.contains("sword") || matName.contains("axe") || matName.contains("bow") || matName.contains("crossbow") || matName.contains("trident");
        boolean isTool = matName.contains("pickaxe") || matName.contains("shovel") || matName.contains("hoe") || matName.contains("shears") || matName.contains("flint_and_steel");

        switch (requiredType) {
            case "armor_weapon": return isArmor || isWeapon;
            case "tool": return isTool;
            default: return false;
        }
    }
    
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
    
    private String getCleanItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && !item.getItemMeta().getDisplayName().isEmpty()) {
             return ChatColor.stripColor(item.getItemMeta().getDisplayName()).replaceAll("\\[(.*?)\\]", "").trim();
        }
        return capitalize(item.getType().name().replace('_', ' ').toLowerCase());
    }
    
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