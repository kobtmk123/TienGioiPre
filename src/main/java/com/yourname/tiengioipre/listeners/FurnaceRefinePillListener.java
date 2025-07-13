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

public class FurnaceRefinePillListener implements Listener {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;

    public FurnaceRefinePillListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    @EventHandler
    public void onPillSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (source == null || !source.hasItemMeta()) {
            plugin.getLogger().info("[Debug-FurnaceSmelt] Source item is null or has no meta.");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            plugin.getLogger().info("[Debug-FurnaceSmelt] Source item is NOT a semi-finished pill.");
            return;
        }
        plugin.getLogger().info("[Debug-FurnaceSmelt] Source item IS a semi-finished pill!");

        String pillData = container.get(semiPillKey, PersistentDataType.STRING);
        String[] parts = pillData.split(":");
        if (parts.length < 2) {
            plugin.getLogger().warning("[Debug-FurnaceSmelt] Invalid pill data: " + pillData);
            return;
        }

        String pillId = parts[0];
        String tierId = parts[1];
        plugin.getLogger().info("[Debug-FurnaceSmelt] Smelting pill: " + pillId + " Tier: " + tierId);

        ItemStack finalPill = createFinalPill(pillId, tierId);
        
        if (finalPill != null) {
            event.setResult(finalPill);
            plugin.getLogger().info("[Debug-FurnaceSmelt] Successfully set result for pill: " + finalPill.getItemMeta().getDisplayName());
        } else {
            plugin.getLogger().warning("[Debug-FurnaceSmelt] createFinalPill returned null for " + pillId + ":" + tierId);
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!(event.getBlock().getState() instanceof Furnace)) return;
        
        Furnace furnace = (Furnace) event.getBlock().getState();
        ItemStack source = furnace.getInventory().getSmelting(); // Vật phẩm trong ô nung

        if (source == null || !source.hasItemMeta()) {
            plugin.getLogger().info("[Debug-FurnaceBurn] Smelting item is null or has no meta. Not a pill.");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            plugin.getLogger().info("[Debug-FurnaceBurn] Smelting item is NOT a semi-finished pill. Not changing burn time.");
            return;
        }
        plugin.getLogger().info("[Debug-FurnaceBurn] Smelting item IS a semi-finished pill! Setting custom burn time.");

        int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20);
        event.setBurnTime(cookTimeInSeconds * 20); // 20 ticks = 1 giây
        event.setBurning(true); // Đảm bảo lò cháy
        plugin.getLogger().info("[Debug-FurnaceBurn] Custom burn time set to " + cookTimeInSeconds + " seconds.");
    }

    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            plugin.getLogger().warning("Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        return pill;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}