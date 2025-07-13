package com.yourname.tiengioipre.tasks;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class AlchemyFurnaceTask extends BukkitRunnable {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey; // Cần dùng để tạo final pill

    public AlchemyFurnaceTask(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus"); // Khởi tạo key này
    }

    @Override
    public void run() {
        // Duyệt qua tất cả các chunk đang tải (loaded chunks)
        // Đây là phương pháp hiệu suất kém, chỉ dùng để debug/kiểm tra.
        // Cần một cách tối ưu hơn để theo dõi các lò nung cụ thể.
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Furnace) {
                        Furnace furnace = (Furnace) blockState;
                        ItemStack smeltingItem = furnace.getInventory().getSmelting();

                        if (smeltingItem == null || !smeltingItem.hasItemMeta()) continue;

                        PersistentDataContainer container = smeltingItem.getItemMeta().getPersistentDataContainer();

                        if (container.has(semiPillKey, PersistentDataType.STRING)) {
                            // Đã tìm thấy Đan dược nửa mùa trong lò nung
                            DebugLogger.log("AlchemyFurnaceTask", "Detected semi-pill in furnace at " + furnace.getLocation().toVector());

                            int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 5);
                            short targetCookTime = (short)(cookTimeInSeconds * 20);

                            if (furnace.getCookTime() < targetCookTime) {
                                DebugLogger.log("AlchemyFurnaceTask", "Forcing furnace cook: " + furnace.getCookTime() + "/" + targetCookTime);
                                furnace.setCookTime((short) (furnace.getCookTime() + 1)); // Tăng cook time
                                furnace.setBurnTime((short) (furnace.getBurnTime() + 1)); // Tăng burn time (để lò cháy)
                                furnace.update(true); // Cập nhật trạng thái lò
                            } else {
                                // Đã nung xong, tạo kết quả
                                DebugLogger.log("AlchemyFurnaceTask", "Semi-pill cooked! Creating final pill.");
                                String pillData = container.get(semiPillKey, PersistentDataType.STRING);
                                String[] parts = pillData.split(":");
                                String pillId = parts[0];
                                String tierId = parts[1];

                                // Tạo viên đan dược hoàn chỉnh (cần tạo hàm này trong Listener hoặc ở đây)
                                // Tạm thời tạo dummy item
                                ItemStack finalPill = createFinalPill(pillId, tierId); // Sẽ lấy logic từ FurnaceRefinePillListener

                                if (finalPill != null) {
                                    furnace.getInventory().setSmelting(null); // Xóa item nửa mùa
                                    furnace.getInventory().setResult(finalPill); // Đặt kết quả
                                    furnace.setCookTime((short) 0);
                                    furnace.setBurnTime((short) 0);
                                    furnace.update(true);
                                    DebugLogger.log("AlchemyFurnaceTask", "Final pill created: " + finalPill.getItemMeta().getDisplayName());
                                } else {
                                    DebugLogger.warn("AlchemyFurnaceTask", "Failed to create final pill for " + pillId + ":" + tierId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Hàm tạo final pill (đã lấy từ FurnaceRefinePillListener)
    private ItemStack createFinalPill(String pillId, String tierId) {
        // Lấy lại logic từ FurnaceRefinePillListener.java::createFinalPill
        // Để không lặp code, bạn có thể tạo một ItemFactory trong core và gọi nó ở đây
        // Nhưng tạm thời, chúng ta sẽ đặt lại code vào đây để nó chạy độc lập
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            DebugLogger.warn("FurnaceRefine", "createFinalPill: Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = plugin.getTongMonManager().format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(plugin.getTongMonManager().format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        DebugLogger.log("FurnaceRefine", "createFinalPill: Final pill created with NBT bonus: " + bonus);
        return pill;
    }
}