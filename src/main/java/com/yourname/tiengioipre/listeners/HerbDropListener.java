package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material; // <-- IMPORT QUAN TRỌNG
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

public class HerbDropListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();
    
    // Đã sửa lỗi: Đảm bảo tất cả các Material đều có "Material." đứng trước
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

        if (player.getGameMode() == GameMode.CREATIVE || !FARMABLE_BLOCKS.contains(blockType)) {
            return;
        }

        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            if (ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !"luyendansu".equals(data.getCultivationPath())) {
            return;
        }
        
        ConfigurationSection herbsSection = plugin.getConfig().getConfigurationSection("alchemy.herbs");
        if (herbsSection == null) {
            return;
        }

        Set<String> herbIds = herbsSection.getKeys(false);
        boolean didDropHerb = false;

        for (String herbId : herbIds) {
            double dropChance = herbsSection.getDouble(herbId + ".drop-chance-from-grass", 0.0);
            
            if (random.nextDouble() < dropChance) {
                ItemManager itemManager = plugin.getItemManager();
                ItemStack herbStack = itemManager.createCultivationItem(herbId, "default"); 
                
                if (herbStack != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), herbStack);
                    didDropHerb = true;
                }
            }
        }

        if (didDropHerb) {
            event.setDropItems(false);
        }
    }
}