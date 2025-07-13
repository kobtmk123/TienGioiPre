package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // Import EventPriority
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable; // Import BukkitRunnable

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

    // Ưu tiên cao nhất để xử lý trước các plugin khác
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPillSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (source == null || !source.hasItemMeta()) {
            plugin.getLogger().info("[Debug-FSmelt] Source item is null or has no meta. -> No Action");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            plugin.getLogger().info("[Debug-FSmelt] Source item is NOT a semi-finished pill (no NBT). -> No Action");
            return;
        }
        plugin.getLogger().info("[Debug-FSmelt] Detected SEMI-FINISHED PILL in smelt slot!");

        String pillData = container.get(semiPillKey, PersistentDataType.STRING);
        plugin.getLogger().info("[Debug-FSmelt] Pill NBT data: " + pillData);

        String[] parts = pillData.split(":");
        if (parts.length < 2) {
            plugin.getLogger().warning("[Debug-FSmelt] Invalid pill data format: " + pillData);
            return;
        }

        String pillId = parts[0];
        String tierId = parts[1];
        plugin.getLogger().info("[Debug-FSmelt] Processing pill: ID=" + pillId + ", Tier=" + tierId);

        ItemStack finalPill = createFinalPill(pillId, tierId);
        
        if (finalPill != null) {
            event.setResult(finalPill);
            plugin.getLogger().info("[Debug-FSmelt] Successfully set final pill result: " + finalPill.getItemMeta().getDisplayName());
        } else {
            plugin.getLogger().warning("[Debug-FSmelt] createFinalPill returned null for " + pillId + ":" + tierId);
        }
    }

    // Ưu tiên cao nhất để xử lý trước các plugin khác
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        // Lấy item trong ô nguyên liệu của lò (ô trên cùng)
        // Cần kiểm tra xem block có phải là Furnace không để tránh lỗi
        if (!(event.getBlock().getState() instanceof Furnace)) {
            plugin.getLogger().info("[Debug-FBurn] Block is not a furnace. -> No Action");
            return;
        }
        
        Furnace furnace = (Furnace) event.getBlock().getState();
        ItemStack source = furnace.getInventory().getSmelting();

        if (source == null || !source.hasItemMeta()) {
            plugin.getLogger().info("[Debug-FBurn] Smelting item is null or has no meta. -> No Action");
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        if (!container.has(semiPillKey, PersistentDataType.STRING)) {
            plugin.getLogger().info("[Debug-FBurn] Smelting item is NOT a semi-finished pill (no NBT). -> No Action");
            return;
        }
        plugin.getLogger().info("[Debug-FBurn] Detected SEMI-FINISHED PILL in furnace! Setting custom burn time.");

        int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 20);
        event.setBurnTime(cookTimeInSeconds * 20); // 20 ticks = 1 giây
        
        // Đặt event.setBurning(true) ở đây không phải lúc nào cũng đủ.
        // Chúng ta sẽ ép buộc lò nung hoạt động bằng cách đặt cook time và fuel time trực tiếp
        // (Nếu vẫn không được, cần dùng một task lặp để kiểm tra và ép)

        // Cần một task nhỏ để ép lò nung nếu nó không cháy ngay lập tức
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(furnace.getInventory().getSmelting() != null && furnace.getInventory().getSmelting().hasItemMeta() && furnace.getInventory().getSmelting().getItemMeta().getPersistentDataContainer().has(semiPillKey, PersistentDataType.STRING))) {
                    this.cancel(); // Item đã bị rút ra hoặc đã nung xong
                    return;
                }

                furnace.setCookTime( (short) (furnace.getCookTime() + 1)); // Tăng cook time mỗi tick
                furnace.setBurnTime( (short) (furnace.getBurnTime() + 1)); // Tăng burn time mỗi tick
                furnace.update(true); // Cập nhật trạng thái lò
                
                plugin.getLogger().info("[Debug-FBurn-Force] Forcing furnace at " + furnace.getLocation().toVector() + ". CookTime: " + furnace.getCookTime());

                if (furnace.getCookTime() >= cookTimeInSeconds * 20) {
                    this.cancel(); // Đã nung xong
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Chạy task mỗi tick sau 1 tick
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