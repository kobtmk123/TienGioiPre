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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CauldronAlchemyListener implements Listener {

    private final TienGioiPre plugin;
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
            return;
        }
        
        if (!(block.getBlockData() instanceof Levelled)) return;
        Levelled cauldronData = (Levelled) block.getBlockData();
        if (cauldronData.getLevel() != 0) return;

        Material blockBelow = block.getRelative(0, -1, 0).getType();
        if (blockBelow != Material.FIRE && blockBelow != Material.MAGMA_BLOCK) {
            return;
        }

        event.setCancelled(true);
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.getItem().setType(Material.BUCKET);
        }
        
        block.setType(Material.WATER_CAULDRON);
        Levelled waterCauldronData = (Levelled) block.getBlockData();
        waterCauldronData.setLevel(waterCauldronData.getMaximumLevel());
        block.setBlockData(waterCauldronData);
        
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
        startBoiling(player, block.getLocation());
    }

    private void startBoiling(Player player, Location loc) {
        if (activeCauldrons.containsKey(loc)) return;

        activeCauldrons.put(loc, new CauldronData());
        long boilTime = plugin.getConfig().getLong("alchemy.settings.water-boil-time-seconds", 60) * 20L;
        player.sendMessage(format("&bNước trong vạc bắt đầu được đun nóng..."));

        new BukkitRunnable() {
            @Override
            public void run() {
                CauldronData currentData = activeCauldrons.get(loc);
                if (currentData == null || loc.getBlock().getType() != Material.WATER_CAULDRON) {
                    activeCauldrons.remove(loc);
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
        
        new BukkitRunnable() {
            int ticksLived = 0;
            @Override
            public void run() {
                if (!droppedItemEntity.isValid() || ticksLived++ > 40) { // Hủy sau 2 giây
                    this.cancel();
                    return;
                }
                
                Location itemLoc = droppedItemEntity.getLocation();
                Block blockAt = itemLoc.getBlock();
                
                if (blockAt.getType() == Material.WATER_CAULDRON && activeCauldrons.containsKey(blockAt.getLocation())) {
                    CauldronData cauldron = activeCauldrons.get(blockAt.getLocation());
                    if (cauldron != null && cauldron.isBoiling) {
                        handleItemAlchemy(player, cauldron, droppedItemEntity.getItemStack(), blockAt.getLocation());
                        droppedItemEntity.remove();
                        this.cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L); // Kiểm tra mỗi 1/4 giây
    }

    private void handleItemAlchemy(Player player, CauldronData cauldron, ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String herbId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        if (herbId == null) {
            player.sendMessage(format("&cVật phẩm này không phải dược liệu hợp lệ!"));
            return;
        }
        
        cauldron.addIngredient(herbId, item.getAmount());
        player.sendMessage(format("&aĐã thêm " + item.getAmount() + "x " + meta.getDisplayName() + " &avào vạc."));
        
        String matchedRecipeId = findMatchingRecipe(cauldron.ingredients);
        if (matchedRecipeId != null) {
            player.sendMessage(format("&eNguyên liệu đã đủ, bắt đầu luyện đan!"));
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
            
            activeCauldrons.remove(loc); 
            
            long craftTime = plugin.getConfig().getLong("alchemy.settings.cauldron-craft-time-seconds", 30) * 20L;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    Block cauldronBlock = loc.getBlock();
                    if (cauldronBlock.getType() == Material.WATER_CAULDRON) {
                        cauldronBlock.setType(Material.CAULDRON);
                    }
                    
                    String pillTier = getRandomPillTier(matchedRecipeId);
                    if (pillTier == null) return;
                    
                    ItemStack halfMadePill = createHalfMadePill(matchedRecipeId, pillTier);
                    if (halfMadePill != null) {
                        loc.getWorld().dropItemNaturally(loc.clone().add(0, 1, 0), halfMadePill);
                        player.sendMessage(format("&6Một viên đan dược đã được luyện thành! Hãy nung nó để hoàn thiện."));
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
        ConfigurationSection chanceSection = plugin.getConfig().getConfigurationSection("alchemy.recipes." + recipeId + ".tier-chance");
        if (chanceSection == null) return "tam_pham"; // Mặc định là phẩm chất thấp nhất
        
        int totalWeight = chanceSection.getKeys(false).stream().mapToInt(chanceSection::getInt).sum();
        if (totalWeight <= 0) return "tam_pham";

        int randomNum = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (String key : chanceSection.getKeys(false)) {
            currentWeight += chanceSection.getInt(key);
            if (randomNum < currentWeight) {
                return key;
            }
        }
        return "tam_pham";
    }
    
    private ItemStack createHalfMadePill(String pillId, String pillTier) {
        ItemStack item = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.setDisplayName(format("&7Đan Dược Nửa Mùa"));
        meta.setLore(Collections.singletonList(format("&fBỏ vào lò nung để hoàn thiện.")));
        
        ItemManager itemManager = plugin.getItemManager();
        meta.getPersistentDataContainer().set(itemManager.HALF_MADE_PILL_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(itemManager.ITEM_ID_KEY, PersistentDataType.STRING, pillId);
        meta.getPersistentDataContainer().set(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING, pillTier);
        
        item.setItemMeta(meta);
        return item;
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    private static class CauldronData {
        boolean isBoiling = false;
        Map<String, Integer> ingredients = new HashMap<>();
        void addIngredient(String id, int amount) { ingredients.merge(id, amount, Integer::sum); }
    }
}