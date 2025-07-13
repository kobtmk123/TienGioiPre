package com.myname.tiengioipre.listeners;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.core.ItemManager;
import com.myname.tiengioipre.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Xử lý việc rơi ra Dược Liệu khi người chơi là Luyện Đan Sư phá các loại cây cỏ.
 */
public class HerbDropListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();
    
    // Danh sách các loại cây có thể rơi ra dược liệu.
    // ĐÃ BỎ "Material.GRASS" (cỏ thấp) RA KHỎI DANH SÁCH.
    private final Set<Material> HERB_SOURCES = new HashSet<>(Arrays.asList(
            Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.VINE, Material.LILY_PAD, Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
            Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES
    ));
    
    // Danh sách các cây nông sản cần phải chín hoàn toàn mới có tỷ lệ rơi
    private final Set<Material> RIPE_CROP_SOURCES = new HashSet<>(Arrays.asList(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    ));

    public HerbDropListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 1. Bỏ qua nếu người chơi ở chế độ sáng tạo hoặc block bị phá không nằm trong danh sách nguồn
        if (player.getGameMode() == GameMode.CREATIVE || !HERB_SOURCES.contains(block.getType())) {
            return;
        }

        // 2. Kiểm tra xem người chơi có phải Luyện Đan Sư không
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) {
            return;
        }

        // 3. Nếu là cây nông sản, kiểm tra xem đã chín hoàn toàn chưa
        if (RIPE_CROP_SOURCES.contains(block.getType())) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    return; // Cây chưa chín, không rơi dược liệu
                }
            }
        }

        // 4. Lấy mục cấu hình của các dược liệu
        ConfigurationSection herbsSection = plugin.getConfig().getConfigurationSection("alchemy.herbs");
        if (herbsSection == null) {
            return;
        }
        
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        ItemManager itemManager = plugin.getItemManager();

        // 5. Duyệt qua từng loại dược liệu trong config và random tỷ lệ rơi
        for (String herbId : herbsSection.getKeys(false)) {
            ConfigurationSection herbConfig = herbsSection.getConfigurationSection(herbId);
            if (herbConfig == null) continue;

            double dropChance = herbConfig.getDouble("drop-chance-from-grass", 0.0);
            
            if (random.nextDouble() < dropChance) {
                // Giả sử các dược liệu không có tier, ta dùng "default" để gọi hàm tạo item
                ItemStack herbItemStack = itemManager.createCultivationItem(herbId, "default");
                
                if (herbItemStack != null) {
                    // Drop vật phẩm ra thế giới
                    block.getWorld().dropItemNaturally(dropLocation, herbItemStack);
                    
                    // Ngăn block drop item mặc định (ví dụ: wheat, seeds) để tránh rác
                    event.setDropItems(false);
                }
            }
        }
    }
}