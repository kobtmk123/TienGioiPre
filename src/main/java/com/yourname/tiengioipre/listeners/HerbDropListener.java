package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
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

public class HerbDropListener implements Listener {

    private final TienGioiPre plugin;
    private final Random random = new Random();
    
    // ĐÃ BỎ "Material.GRASS" RA KHỎI DANH SÁCH
    private final Set<Material> HERB_SOURCES = new HashSet<>(Arrays.asList(
            Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.VINE, Material.LILY_PAD, Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
            Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES
    ));
    
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

        if (player.getGameMode() == GameMode.CREATIVE || !HERB_SOURCES.contains(block.getType())) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) {
            return;
        }

        if (RIPE_CROP_SOURCES.contains(block.getType())) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    return;
                }
            }
        }

        ConfigurationSection herbsSection = plugin.getConfig().getConfigurationSection("alchemy.herbs");
        if (herbsSection == null) return;
        
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        ItemManager itemManager = plugin.getItemManager();

        for (String herbId : herbsSection.getKeys(false)) {
            ConfigurationSection herbConfig = herbsSection.getConfigurationSection(herbId);
            if (herbConfig == null) continue;

            double dropChance = herbConfig.getDouble("drop-chance-from-grass", 0.0);
            
            if (random.nextDouble() < dropChance) {
                ItemStack herbItemStack = itemManager.createCultivationItem(herbId, "default");
                
                if (herbItemStack != null) {
                    block.getWorld().dropItemNaturally(dropLocation, herbItemStack);
                    event.setDropItems(false);
                }
            }
        }
    }
}