package com.yourname.tiengioipre.tasks;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlchemyFurnaceTask extends BukkitRunnable {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;
    
    // Map mới để theo dõi tiến độ của từng viên đan dược trong lò
    // Key: Location của lò nung
    // Value: Current progress (ticks)
    private final Map<Location, Short> furnaceProgress = new HashMap<>();

    public AlchemyFurnaceTask(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    @Override
    public void run() {
        // Duyệt qua tất cả các lò nung đang được theo dõi
        // Sử dụng một bản sao của KeySet để tránh ConcurrentModificationException
        for (Location furnaceLoc : new ArrayList<>(furnaceProgress.keySet())) {
            BlockState blockState = furnaceLoc.getBlock().getState();

            // Nếu block không còn là lò nung hoặc chunk đã bị unload, xóa khỏi map
            if (!(blockState instanceof Furnace) || !furnaceLoc.getChunk().isLoaded()) {
                furnaceProgress.remove(furnaceLoc);
                DebugLogger.log("AlchemyFurnaceTask", "Removed unloaded/invalid furnace from tracking: " + furnaceLoc.toVector());
                continue;
            }

            Furnace furnace = (Furnace) blockState;
            ItemStack smeltingItem = furnace.getInventory().getSmelting();

            // Nếu item trong lò không còn là đan dược nửa mùa, xóa khỏi tracking
            if (smeltingItem == null || !smeltingItem.hasItemMeta() || !smeltingItem.getItemMeta().getPersistentDataContainer().has(semiPillKey, PersistentDataType.STRING)) {
                furnaceProgress.remove(furnaceLoc);
                DebugLogger.log("AlchemyFurnaceTask", "Removed cooked/removed item from tracking: " + furnaceLoc.toVector());
                continue;
            }

            // Lấy thời gian nấu cần thiết từ config
            int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 5);
            short targetCookTime = (short)(cookTimeInSeconds * 20); // Chuyển giây sang ticks

            // Lấy tiến độ hiện tại
            short currentCookTime = furnaceProgress.getOrDefault(furnaceLoc, (short)0);
            currentCookTime += 20; // Tăng 20 ticks mỗi 1 giây (task chạy mỗi 20 ticks)

            furnaceProgress.put(furnaceLoc, currentCookTime); // Cập nhật tiến độ

            // DEBUGGING: In tiến độ
            DebugLogger.log("AlchemyFurnaceTask", "Cooking " + smeltingItem.getItemMeta().getDisplayName() + " at " + furnaceLoc.toVector() + ". Progress: " + currentCookTime + "/" + targetCookTime);

            // Nếu đã nấu xong
            if (currentCookTime >= targetCookTime) {
                DebugLogger.log("AlchemyFurnaceTask", "Semi-pill cooked! Creating final pill.");
                String pillData = smeltingItem.getItemMeta().getPersistentDataContainer().get(semiPillKey, PersistentDataType.STRING);
                String[] parts = pillData.split(":");
                String pillId = parts[0];
                String tierId = parts[1];

                ItemStack finalPill = createFinalPill(pillId, tierId);

                if (finalPill != null) {
                    // Đặt kết quả vào lò nung
                    furnace.getInventory().setSmelting(null); // Xóa item nửa mùa
                    furnace.getInventory().setResult(finalPill); // Đặt đan dược hoàn chỉnh
                    furnace.setCookTime((short) 0); // Reset cook time
                    furnace.setBurnTime((short) 0); // Tắt lửa
                    furnace.update(true); // Cập nhật trạng thái lò
                    
                    DebugLogger.log("AlchemyFurnaceTask", "Final pill created: " + finalPill.getItemMeta().getDisplayName());
                } else {
                    DebugLogger.warn("AlchemyFurnaceTask", "Failed to create final pill for " + pillId + ":" + tierId);
                }
                furnaceProgress.remove(furnaceLoc); // Xóa khỏi tracking
            }
            // Không cần phải quản lý burnTime của lò thực sự nếu chúng ta tự quản lý cookTime
            // Tuy nhiên, để lò có vẻ "cháy" thì cần có nhiên liệu.
            // Có thể đặt một ít nhiên liệu ảo nếu không có (như một cục than) để lò vẫn có lửa.
            // Nhưng nếu lửa nháy thì không quan trọng lắm, miễn là đan dược ra.
        }
    }

    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            DebugLogger.warn("AlchemyFurnaceTask", "createFinalPill: Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
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
        DebugLogger.log("AlchemyFurnaceTask", "createFinalPill: Final pill created with NBT bonus: " + bonus);
        return pill;
    }
    
    private String format(String msg) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start(), matcher.end());
            msg = msg.replace(color, ChatColor.of(color.substring(1)) + "");
            matcher = pattern.matcher(msg);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}