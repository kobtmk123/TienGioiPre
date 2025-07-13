package com.yourname.tiengioipre.tasks;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.utils.DebugLogger;
import net.md_5.bungee.api.ChatColor; // Đã sửa import cho ChatColor (hỗ trợ RGB)
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

import java.util.ArrayList;
import java.util.HashMap; // Đã thêm import HashMap
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task này chịu trách nhiệm tự động xử lý quá trình nung Đan Dược Nửa Mùa trong lò.
 * Nó chủ động kiểm tra các lò đang được theo dõi và ép lò nung.
 */
public class AlchemyFurnaceTask extends BukkitRunnable {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;
    
    // Map để theo dõi các lò nung chứa đan dược nửa mùa.
    // Key: Location của lò nung
    // Value: Item đan dược nửa mùa đang được nung (được lưu trữ để xử lý NBT)
    private final Map<Location, ItemStack> furnacesToTrack = new HashMap<>(); 

    public AlchemyFurnaceTask(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    /**
     * Thêm một lò nung vào danh sách theo dõi của task.
     * Phương thức này được gọi từ PlayerInteractListener khi người chơi đặt đan dược vào lò.
     * @param loc Vị trí của lò nung.
     * @param semiPillItem Viên đan dược nửa mùa được đặt vào lò.
     */
    public void addFurnaceToTrack(Location loc, ItemStack semiPillItem) {
        // Chỉ thêm nếu block tại vị trí đó thực sự là một lò nung
        if (loc.getBlock().getState() instanceof Furnace) {
            furnacesToTrack.put(loc, semiPillItem); // Lưu item vào map để truy cập NBT
            DebugLogger.log("AlchemyFurnaceTask", "Added furnace at " + loc.toVector() + " to tracking for semi-pill: " + semiPillItem.getItemMeta().getDisplayName());
        } else {
            DebugLogger.warn("AlchemyFurnaceTask", "Attempted to add non-furnace block to tracking: " + loc.toVector());
        }
    }

    /**
     * Hàm chính của task, chạy định kỳ để kiểm tra và ép lò nung.
     */
    @Override
    public void run() {
        // Duyệt qua một bản sao của keySet để tránh ConcurrentModificationException
        // khi xóa các phần tử trong khi duyệt.
        for (Location furnaceLoc : new ArrayList<>(furnacesToTrack.keySet())) {
            BlockState blockState = furnaceLoc.getBlock().getState();

            // 1. Kiểm tra trạng thái của lò nung
            if (!(blockState instanceof Furnace) || !furnaceLoc.getChunk().isLoaded()) {
                // Nếu block không còn là lò nung hoặc chunk đã bị unload, xóa khỏi map theo dõi.
                furnacesToTrack.remove(furnaceLoc);
                DebugLogger.log("AlchemyFurnaceTask", "Removed unloaded/invalid furnace from tracking: " + furnaceLoc.toVector());
                continue; // Chuyển sang lò tiếp theo
            }

            Furnace furnace = (Furnace) blockState;
            ItemStack smeltingItem = furnace.getInventory().getSmelting(); // Lấy vật phẩm đang được nung trong lò

            // 2. Kiểm tra xem item trong lò còn là đan dược nửa mùa không
            if (smeltingItem == null || !smeltingItem.hasItemMeta() || !smeltingItem.getItemMeta().getPersistentDataContainer().has(semiPillKey, PersistentDataType.STRING)) {
                // Nếu item đã bị rút ra, đã nung xong (và bị thay thế bằng kết quả), hoặc không còn là đan dược nửa mùa
                furnacesToTrack.remove(furnaceLoc);
                DebugLogger.log("AlchemyFurnaceTask", "Removed cooked/removed item from tracking: " + furnaceLoc.toVector());
                continue; // Chuyển sang lò tiếp theo
            }

            // Lấy thời gian nấu cần thiết từ config
            int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 5);
            short targetCookTime = (short)(cookTimeInSeconds * 20); // Chuyển giây sang ticks

            // Lấy tiến độ hiện tại của lò nung (cookTime)
            short currentCookTime = furnace.getCookTime(); 
            currentCookTime += 20; // Tăng 20 ticks (tương đương 1 giây, vì task chạy mỗi 20 ticks)

            furnace.setCookTime(currentCookTime); // Cập nhật cook time của lò

            // Để đảm bảo lò có vẻ "cháy", chúng ta tăng burnTime.
            // Nếu lò không có nhiên liệu, nó sẽ không cháy thật sự, nhưng cookTime vẫn tăng.
            // Mục tiêu là Đan dược ra, không phải lò cháy đẹp.
            furnace.setBurnTime((short) (furnace.getBurnTime() + 20)); // Tăng burn time (để lò có lửa)
            furnace.update(true); // Cập nhật trạng thái lò ngay lập tức

            DebugLogger.log("AlchemyFurnaceTask", "Cooking " + smeltingItem.getItemMeta().getDisplayName() + " at " + furnaceLoc.toVector() + ". Progress: " + furnace.getCookTime() + "/" + targetCookTime);

            // 3. Nếu đã nấu xong
            if (furnace.getCookTime() >= targetCookTime) {
                DebugLogger.log("AlchemyFurnaceTask", "Semi-pill cooked! Creating final pill.");
                String pillData = smeltingItem.getItemMeta().getPersistentDataContainer().get(semiPillKey, PersistentDataType.STRING);
                String[] parts = pillData.split(":");
                String pillId = parts[0];
                String tierId = parts[1];

                ItemStack finalPill = createFinalPill(pillId, tierId); // Tạo viên đan dược hoàn chỉnh

                if (finalPill != null) {
                    furnace.getInventory().setSmelting(null); // Xóa item nửa mùa khỏi ô nung
                    furnace.getInventory().setResult(finalPill); // Đặt đan dược hoàn chỉnh vào ô kết quả
                    
                    // Reset trạng thái lò nung về ban đầu
                    furnace.setCookTime((short) 0);
                    furnace.setBurnTime((short) 0);
                    furnace.update(true); // Cập nhật trạng thái lò
                    
                    DebugLogger.log("AlchemyFurnaceTask", "Final pill created: " + finalPill.getItemMeta().getDisplayName());
                } else {
                    DebugLogger.warn("AlchemyFurnaceTask", "Failed to create final pill for " + pillId + ":" + tierId);
                }
                furnacesToTrack.remove(furnaceLoc); // Xóa khỏi danh sách theo dõi
            }
        }
    }

    /**
     * Tạo ra một ItemStack Đan Dược hoàn chỉnh dựa trên ID và phẩm chất.
     * Logic này được lấy từ FurnaceRefinePillListener để tránh lặp code.
     * @param pillId ID của loại đan, ví dụ: "luyen_khi_dan"
     * @param tierId ID của phẩm chất, ví dụ: "nhat_pham"
     * @return ItemStack Đan Dược hoàn chỉnh, hoặc null nếu có lỗi.
     */
    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            DebugLogger.warn("AlchemyFurnaceTask", "createFinalPill: Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        // Sử dụng hàm format từ TongMonManager để đảm bảo hỗ trợ màu RGB
        String displayName = plugin.getTongMonManager().format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(plugin.getTongMonManager().format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        DebugLogger.log("AlchemyFurnaceTask", "createFinalPill: Final pill created with NBT bonus: " + bonus);
        return pill;
    }
    
    /**
     * Định dạng mã màu cho chuỗi (hỗ trợ cả & và RGB).
     * Hàm này được đặt ở đây để không cần phải truy cập TongMonManager mỗi lần.
     */
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