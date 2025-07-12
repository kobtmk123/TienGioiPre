package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class HerbDropListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();
    
    // Danh sách các loại cây cỏ có thể rơi ra dược liệu
    private final Set<Material> HERB_SOURCES = new HashSet<>(Arrays.asList(
            Material.GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.VINE,
            Material.LILY_PAD
    ));

    public HerbDropListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Bỏ qua nếu người chơi ở chế độ sáng tạo hoặc không phải là các loại cây cỏ trong danh sách
        if (player.getGameMode() == GameMode.CREATIVE || !HERB_SOURCES.contains(block.getType())) {
            return;
        }

        // Kiểm tra xem người chơi có phải Luyện Đan Sư không
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) {
            return;
        }

        // Lấy mục cấu hình của các dược liệu
        ConfigurationSection herbsSection = plugin.getConfig().getConfigurationSection("alchemy.herbs");
        if (herbsSection == null) {
            return;
        }
        
        // Vị trí để drop item
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        ItemManager itemManager = plugin.getItemManager();

        // Duyệt qua từng loại dược liệu trong config
        for (String herbId : herbsSection.getKeys(false)) {
            ConfigurationSection herbConfig = herbsSection.getConfigurationSection(herbId);
            if (herbConfig == null) continue;

            double dropChance = herbConfig.getDouble("drop-chance-from-grass", 0.0);
            
            // Random tỷ lệ
            if (random.nextDouble() < dropChance) {
                // Nếu trúng, tạo vật phẩm dược liệu
                // Giả sử các dược liệu không có tier, ta dùng "default"
                ItemStack herbItemStack = itemManager.createCultivationItem(herbId, "default");
                
                if (herbItemStack != null) {
                    // Drop vật phẩm ra thế giới
                    block.getWorld().dropItemNaturally(dropLocation, herbItemStack);
                    
                    // Ngăn block drop item mặc định (ví dụ: wheat, seeds)
                    event.setDropItems(false);
                }
            }
        }
    }
}