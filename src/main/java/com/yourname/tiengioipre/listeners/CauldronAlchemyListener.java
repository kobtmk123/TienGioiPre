package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CauldronAlchemyListener implements Listener {

    private final TienGioiPre plugin;
    // Map để lưu trữ trạng thái của các vạc đang hoạt động
    private final Map<Location, CauldronData> activeCauldrons = new HashMap<>();
    private final Random random = new Random();

    public CauldronAlchemyListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    // Sự kiện khi người chơi dùng xô nước đổ vào vạc
    @EventHandler
    public void onPlayerFillCauldron(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null || event.getItem().getType() != Material.WATER_BUCKET) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAULDRON) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !"luyendansu".equals(data.getCultivationPath())) {
            return; // Chỉ Luyện Đan Sư mới có thể đun nước
        }

        Levelled cauldronData = (Levelled) block.getBlockData();
        if (cauldronData.getLevel() == cauldronData.getMaximumLevel()) {
            return; // Vạc đã đầy nước
        }

        // Kiểm tra xem có lửa bên dưới không
        Material blockBelow = block.getRelative(0, -1, 0).getType();
        if (blockBelow != Material.FIRE && blockBelow != Material.MAGMA_BLOCK) {
            return;
        }

        // Đánh dấu đây là sự kiện của plugin để tránh các plugin khác can thiệp
        event.setCancelled(true); 
        
        // Cập nhật bằng tay
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.getItem().setType(Material.BUCKET);
        }
        cauldronData.setLevel(cauldronData.getMaximumLevel());
        block.setBlockData(cauldronData);
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);

        // Bắt đầu đun nước
        startBoiling(player, block.getLocation());
    }

    private void startBoiling(Player player, Location loc) {
        if (activeCauldrons.containsKey(loc)) return;

        CauldronData cauldron = new CauldronData();
        activeCauldrons.put(loc, cauldron);
        
        long boilTime = plugin.getConfig().getLong("alchemy.settings.water-boil-time-seconds", 60) * 20L;
        player.sendMessage(format("&bNước trong vạc bắt đầu được đun nóng..."));

        new BukkitRunnable() {
            @Override
            public void run() {
                CauldronData currentData = activeCauldrons.get(loc);
                if (currentData == null) {
                    this.cancel();
                    return;
                }
                currentData.isBoiling = true;
                player.sendMessage(format("&dNước đã sôi, có thể bắt đầu luyện đan!"));
                player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0f, 1.0f);
            }
        }.runTaskLater(plugin, boilTime);
    }
    
    // Sự kiện khi người chơi ném item vào vạc
    @EventHandler
    public void onItemDropInCauldron(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItemEntity = event.getItemDrop();
        ItemStack droppedItemStack = droppedItemEntity.getItemStack();
        
        // Chờ một chút để item rơi xuống
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!droppedItemEntity.isValid()) {
                    this.cancel();
                    return;
                }
                
                Location itemLoc = droppedItemEntity.getLocation();
                Block blockAt = itemLoc.getBlock();
                
                // Kiểm tra xem item có nằm trong vạc đang sôi không
                if (blockAt.getType() == Material.WATER_CAULDRON && activeCauldrons.containsKey(blockAt.getLocation())) {
                    CauldronData cauldron = activeCauldrons.get(blockAt.getLocation());
                    if (cauldron != null && cauldron.isBoiling) {
                        handleItemAlchemy(player, cauldron, droppedItemStack, blockAt.getLocation());
                        droppedItemEntity.remove(); // Xóa item đã ném
                    }
                }
            }
        }.runTaskLater(plugin, 10L); // Chờ nửa giây
    }

    private void handleItemAlchemy(Player player, CauldronData cauldron, ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        // Lấy ID của dược liệu từ NBT
        String herbId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        if (herbId == null || !herbId.startsWith("linh_") && !herbId.startsWith("huyet_") && !herbId.startsWith("bach_")) {
            player.sendMessage(format("&cVật phẩm này không phải dược liệu!"));
            return;
        }
        
        // Thêm nguyên liệu vào vạc
        cauldron.addIngredient(herbId, item.getAmount());
        player.sendMessage(format("&aĐã thêm " + item.getAmount() + "x " + meta.getDisplayName() + " &avào vạc."));
        
        // Kiểm tra công thức
        String matchedRecipeId = findMatchingRecipe(cauldron.ingredients);
        if (matchedRecipeId != null) {
            player.sendMessage(format("&eNguyên liệu đã đủ, bắt đầu luyện đan!"));
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
            
            // Xóa vạc khỏi danh sách hoạt động để tránh người khác ném thêm đồ vào
            activeCauldrons.remove(loc); 
            
            long craftTime = plugin.getConfig().getLong("alchemy.settings.cauldron-craft-time-seconds", 30) * 20L;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Cạn nước
                    Block cauldronBlock = loc.getBlock();
                    if (cauldronBlock.getType() == Material.WATER_CAULDRON) {
                        cauldronBlock.setType(Material.CAULDRON);
                    }
                    
                    // Random phẩm chất
                    String pillTier = getRandomPillTier(matchedRecipeId);
                    if (pillTier == null) return;
                    
                    // Tạo viên đan dược nửa mùa
                    ItemStack halfMadePill = createHalfMadePill(matchedRecipeId, pillTier);
                    if (halfMadePill != null) {
                        loc.getWorld().dropItemNaturally(loc.add(0, 1, 0), halfMadePill);
                        player.sendMessage(format("&6Một viên đan dược đã được luyện thành!"));
                    }
                }
            }.runTaskLater(plugin, craftTime);
        }
    }

    private String findMatchingRecipe(Map<String, Integer> ingredients) {
        ConfigurationSection recipes = plugin.getConfig().getConfigurationSection("alchemy.recipes");
        if (recipes == null) return null;

        for (String recipeId : recipes.getKeys(false)) {
            ConfigurationSection recipeIngredients = recipes.getConfigurationSection(recipeId + ".ingredients");
            if (recipeIngredients == null) continue;

            Map<String, Integer> required = new HashMap<>();
            for (String key : recipeIngredients.getKeys(false)) {
                required.put(key, recipeIngredients.getInt(key));
            }
            if (ingredients.equals(required)) {
                return recipeId;
            }
        }
        return null;
    }
    
    private String getRandomPillTier(String recipeId) {
        // ... (Logic random tier tương tự như trong AnvilRefineListener)
    }
    
    private ItemStack createHalfMadePill(String pillId, String pillTier) {
        // Tạo một item mới, có tên và lore đặc biệt để nhận biết
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    // Class nội bộ để lưu dữ liệu của mỗi vạc
    private static class CauldronData {
        boolean isBoiling = false;
        Map<String, Integer> ingredients = new HashMap<>();

        void addIngredient(String id, int amount) {
            ingredients.merge(id, amount, Integer::sum);
        }
    }
}