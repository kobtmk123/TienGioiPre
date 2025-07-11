package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.GameMode;
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
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Lắng nghe sự kiện phá block để xử lý việc rơi Dược Liệu.
 */
public class HerbDropListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();
    
    // Danh sách các loại block có thể rơi ra dược liệu
    private final List<Material> FARMABLE_BLOCKS = Arrays.asList(
            Material.GRASS, Material.TALL_GRASS, Material.FERN,
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );

    public HerbDropListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // 1. Kiểm tra các điều kiện ban đầu
        if (player.getGameMode() == GameMode.CREATIVE || !FARMABLE_BLOCKS.contains(blockType)) {
            return;
        }

        // Đối với cây nông sản, chỉ drop khi đã lớn hoàn toàn
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            if (ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
        }
        
        // 2. Kiểm tra xem người chơi có phải là Luyện Đan Sư không
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !"luyendansu".equals(data.getCultivationPath())) {
            return;
        }
        
        // 3. Lấy section chứa tất cả các loại dược liệu từ config
        ConfigurationSection herbsSection = plugin.getConfig().getConfigurationSection("alchemy.herbs");
        if (herbsSection == null) {
            return;
        }

        Set<String> herbIds = herbsSection.getKeys(false);
        boolean didDropHerb = false;

        // 4. Duyệt qua từng loại dược liệu để kiểm tra tỷ lệ rơi
        for (String herbId : herbIds) {
            double dropChance = herbsSection.getDouble(herbId + ".drop-chance-from-grass", 0.0);
            
            if (random.nextDouble() < dropChance) {
                ItemManager itemManager = plugin.getItemManager();
                // Dược liệu không có tier, nên dùng "default"
                ItemStack herbStack = itemManager.createCultivationItem(herbId, "default"); 
                
                if (herbStack != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), herbStack);
                    didDropHerb = true;
                }
            }
        }

        // 5. Nếu đã drop ra dược liệu, hủy các drop mặc định (như hạt giống, lúa mì,...)
        if (didDropHerb) {
            event.setDropItems(false);
        }
    }
}