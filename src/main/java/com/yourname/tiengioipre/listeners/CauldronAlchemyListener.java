package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CauldronAlchemyListener implements Listener {

    private final TienGioiPre plugin;
    
    // Class nhỏ để lưu trữ dữ liệu của vạc đang luyện đan
    private static class CauldronSession {
        Map<String, Integer> ingredients = new HashMap<>();
        BukkitRunnable craftTask;
    }
    
    // Map quản lý trạng thái của các vạc
    private final Map<Location, BukkitRunnable> boilingCauldrons = new HashMap<>();
    private final Set<Location> readyCauldrons = new HashSet<>();
    private final Map<Location, CauldronSession> craftingCauldrons = new HashMap<>();

    public CauldronAlchemyListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    // Sự kiện khi người chơi đổ nước vào vạc
    @EventHandler
    public void onCauldronFill(PlayerInteractEvent event) {
        // Chỉ xử lý khi người chơi chuột phải vào block
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.WATER_CAULDRON) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        // Đây là cách kiểm tra đổ nước vào vạc rỗng, vì PlayerBucketFillEvent không đáng tin cậy
        if (itemInHand.getType() == Material.WATER_BUCKET) {
            Levelled cauldronData = (Levelled) clickedBlock.getBlockData();
            // Nếu vạc chưa đầy nước
            if (cauldronData.getLevel() < cauldronData.getMaximumLevel()) {
                // Chỉ kiểm tra và bắt đầu đun khi đổ nước vào vạc chưa đầy
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    checkAndBoil(player, clickedBlock.getLocation());
                }, 1L); // Chờ 1 tick để server cập nhật trạng thái vạc
            }
        }
    }

    private void checkAndBoil(Player player, Location loc) {
        Block block = loc.getBlock();
        if (block.getType() != Material.WATER_CAULDRON) return;

        Block blockBelow = block.getRelative(BlockFace.DOWN);
        if (blockBelow.getType() == Material.FIRE || blockBelow.getType() == Material.MAGMA_BLOCK) {
            if (boilingCauldrons.containsKey(loc) || readyCauldrons.contains(loc)) return;
            
            long boilTime = plugin.getConfig().getLong("alchemy.settings.water-boil-time-seconds", 60);
            player.sendMessage(format("&bBạn bắt đầu đun nước trong vạc..."));
            
            BukkitRunnable boilTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Kiểm tra lại trạng thái của vạc trước khi hoàn thành
                    if (loc.getBlock().getType() != Material.WATER_CAULDRON) {
                        boilingCauldrons.remove(loc);
                        return;
                    }
                    player.sendMessage(format("&aNước trong vạc đã sôi, có thể bắt đầu luyện đan!"));
                    loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0f, 1.0f);
                    loc.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 1, 0.5), 10, 0.2, 0.1, 0.2, 0.01);
                    readyCauldrons.add(loc);
                    boilingCauldrons.remove(loc);
                }
            };
            
            boilTask.runTaskLater(plugin, boilTime * 20L);
            boilingCauldrons.put(loc, boilTask);
        }
    }
    
    // Sự kiện khi người chơi ném item vào vạc
    @EventHandler
    public void onItemDropInCauldron(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItemEntity = event.getItemDrop();
        ItemStack droppedItemStack = droppedItemEntity.getItemStack();
        Location dropLocation = droppedItemEntity.getLocation();

        // Tìm vạc đã sôi gần nhất
        Location cauldronLoc = findNearbyReadyCauldron(dropLocation, 2);
        if (cauldronLoc == null) return;

        // Kiểm tra xem có phải Luyện Đan Sư không
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) return;

        // Kiểm tra xem item có phải là dược liệu không (dựa vào NBT)
        ItemMeta meta = droppedItemStack.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING)) return;
        
        String herbId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        if (!plugin.getConfig().contains("alchemy.herbs." + herbId)) return;
        
        // Hủy việc drop item và xử lý logic
        event.setCancelled(true);
        droppedItemEntity.remove();
        
        // Thêm nguyên liệu vào session của vạc đó
        CauldronSession session = craftingCauldrons.computeIfAbsent(cauldronLoc, k -> new CauldronSession());
        session.ingredients.merge(herbId, droppedItemStack.getAmount(), Integer::sum);
        
        player.sendMessage(format("&aĐã thêm " + droppedItemStack.getAmount() + "x " + meta.getDisplayName() + " &avào vạc."));
        cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.5f);
        
        // Sau khi thêm, kiểm tra xem có khớp công thức nào không
        checkForRecipe(player, cauldronLoc, session);
    }
    
    private void checkForRecipe(Player player, Location cauldronLoc, CauldronSession session) {
        ConfigurationSection recipesSection = plugin.getConfig().getConfigurationSection("alchemy.recipes");
        if (recipesSection == null) return;
        
        for (String recipeId : recipesSection.getKeys(false)) {
            Map<String, Object> requiredIngredients = recipesSection.getConfigurationSection(recipeId + ".ingredients").getValues(false);
            
            if (ingredientsMatch(session.ingredients, requiredIngredients)) {
                // Hủy task cũ nếu có (trường hợp người chơi thêm nguyên liệu khác vào)
                if (session.craftTask != null) session.craftTask.cancel();

                player.sendMessage(format("&eDược liệu đã đủ, lò luyện bắt đầu khởi động..."));
                cauldronLoc.getWorld().playSound(cauldronLoc, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.8f);

                long craftTime = plugin.getConfig().getLong("alchemy.settings.cauldron-craft-time-seconds", 30);

                session.craftTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        craftingCauldrons.remove(cauldronLoc);
                        readyCauldrons.remove(cauldronLoc);
                        
                        Block cauldronBlock = cauldronLoc.getBlock();
                        if (cauldronBlock.getType() == Material.WATER_CAULDRON) {
                            cauldronBlock.setType(Material.CAULDRON);
                        }
                        
                        // Random ra phẩm chất của đan dược
                        String tierId = getRandomPillTier(recipeId);
                        
                        // Tạo item Đan Dược "Nửa Mùa"
                        ItemStack semiPill = createSemiFinishedPill(recipeId, tierId);
                        
                        if (semiPill != null) {
                            cauldronLoc.getWorld().dropItem(cauldronLoc.clone().add(0.5, 1.2, 0.5), semiPill).setVelocity(new Vector(0, 0.2, 0));
                            player.sendMessage(format("&6&lLuyện đan thành công! &fBạn nhận được một viên đan dược cần được tinh luyện."));
                            cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        } else {
                            player.sendMessage(format("&cLỗi khi tạo đan dược, vui lòng báo admin."));
                        }
                    }
                };
                session.craftTask.runTaskLater(plugin, craftTime * 20L);
                break; // Dừng lại sau khi tìm thấy công thức đầu tiên khớp
            }
        }
    }

    private boolean ingredientsMatch(Map<String, Integer> provided, Map<String, Object> required) {
        if (provided.size() != required.size()) return false;
        for (Map.Entry<String, Object> entry : required.entrySet()) {
            if (!provided.containsKey(entry.getKey()) || provided.get(entry.getKey()) < (Integer) entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    private Location findNearbyReadyCauldron(Location loc, double radius) {
        for (Location readyLoc : readyCauldrons) {
            if (Objects.equals(readyLoc.getWorld(), loc.getWorld()) && readyLoc.distanceSquared(loc) <= radius * radius) {
                return readyLoc;
            }
        }
        return null;
    }
    
    private String getRandomPillTier(String recipeId) {
        ConfigurationSection chanceSection = plugin.getConfig().getConfigurationSection("alchemy.recipes." + recipeId + ".tier-chance");
        if (chanceSection == null) return "tam_pham"; // Mặc định là tam phẩm

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

    private ItemStack createSemiFinishedPill(String pillId, String tierId) {
        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = plugin.getConfig().getString("alchemy.pills." + pillId + "." + tierId + ".display-name", "Đan Dược Lỗi");
        meta.setDisplayName(format("&e" + displayName + " &7(Nửa Mùa)"));
        
        List<String> lore = new ArrayList<>();
        lore.add(format("&7Viên đan dược chưa được tinh luyện hoàn toàn."));
        lore.add(format("&7Hãy cho vào lò nung để hoàn tất."));
        meta.setLore(lore);

        // Lưu NBT để listener lò nung nhận biết
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "semi_finished_pill"), PersistentDataType.STRING, pillId + ":" + tierId);
        
        pill.setItemMeta(meta);
        return pill;
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}