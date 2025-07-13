package com.myname.tiengioipre.listeners;

import com.myname.tiengioipre.TienGioiPre;
import com.myname.tiengioipre.data.PlayerData;
import com.myname.tiengioipre.utils.DebugLogger; // <-- IMPORT MỚI
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item; // <-- IMPORT ĐÃ ĐƯỢC THÊM
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class CauldronAlchemyListener implements Listener {

    private final TienGioiPre plugin;
    
    private static class CauldronSession {
        Map<String, Integer> ingredients = new HashMap<>();
        BukkitTask craftTask;
    }
    
    private final Map<Location, BukkitRunnable> boilingCauldrons = new HashMap<>();
    private final Set<Location> readyCauldrons = new HashSet<>();
    private final Map<Location, CauldronSession> craftingCauldrons = new HashMap<>();
    private final Random random = new Random();

    public CauldronAlchemyListener(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCauldronInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Location cauldronLoc = clickedBlock.getLocation();

        // 1. Xử lý khi đổ nước vào vạc rỗng
        if (clickedBlock.getType() == Material.CAULDRON && itemInHand.getType() == Material.WATER_BUCKET) {
            DebugLogger.log("CauldronAlchemy", "Player " + player.getName() + " right-clicked empty cauldron with water bucket at " + cauldronLoc.toVector());
            // Chờ 1 tick để server cập nhật block thành WATER_CAULDRON
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (clickedBlock.getType() == Material.WATER_CAULDRON) { // Đảm bảo đã có nước
                    checkAndBoil(player, cauldronLoc);
                }
            }, 1L);
            return;
        }

        // 2. Xử lý khi bỏ dược liệu vào vạc ĐÃ SÔI
        // Chỉ xử lý nếu vạc đang là WATER_CAULDRON và đã ở trạng thái sẵn sàng (đã sôi)
        if (clickedBlock.getType() == Material.WATER_CAULDRON && readyCauldrons.contains(cauldronLoc)) {
            DebugLogger.log("CauldronAlchemy", "Player " + player.getName() + " right-clicked boiling cauldron at " + cauldronLoc.toVector());
            handleIngredientAdd(player, itemInHand, cauldronLoc, event);
        }
    }

    private void handleIngredientAdd(Player player, ItemStack ingredient, Location cauldronLoc, PlayerInteractEvent event) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) {
            player.sendMessage(format("&cChỉ Luyện Đan Sư mới có thể luyện đan."));
            DebugLogger.log("CauldronAlchemy", "Player " + player.getName() + " is not a Luyen Dan Su. Aborting.");
            return;
        }

        if (ingredient == null || ingredient.getType() == Material.AIR || !ingredient.hasItemMeta()) {
            DebugLogger.log("CauldronAlchemy", "Item in hand is not valid for alchemy.");
            return;
        }
        ItemMeta meta = ingredient.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING)) {
            DebugLogger.log("CauldronAlchemy", "Item in hand has no custom ID. Not an herb.");
            return;
        }
        
        String herbId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        // Kiểm tra xem item có phải là dược liệu không (tức là có tồn tại trong items và alchemy.herbs)
        if (!plugin.getConfig().contains("items." + herbId) || !plugin.getConfig().contains("alchemy.herbs." + herbId)) {
            DebugLogger.log("CauldronAlchemy", "Item ID '" + herbId + "' is not a defined herb in config.");
            return;
        }
        
        event.setCancelled(true); // Ngăn các hành động mặc định của item
        
        // Lấy session hiện tại hoặc tạo mới cho vạc này
        CauldronSession session = craftingCauldrons.computeIfAbsent(cauldronLoc, k -> new CauldronSession());
        session.ingredients.merge(herbId, 1, Integer::sum); // Thêm 1 đơn vị dược liệu vào session
        
        // Trừ 1 item trên tay người chơi
        ingredient.setAmount(ingredient.getAmount() - 1);
        
        player.sendMessage(format("&aĐã thêm 1x " + meta.getDisplayName() + " &avào vạc."));
        cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.5f);
        DebugLogger.log("CauldronAlchemy", "Added " + herbId + " to cauldron at " + cauldronLoc.toVector() + ". Current ingredients: " + session.ingredients);
        
        // Sau khi thêm, kiểm tra xem có khớp công thức nào không
        checkForRecipe(player, cauldronLoc, session);
    }
    
    /**
     * Kiểm tra block dưới vạc và bắt đầu quá trình đun nước.
     */
    private void checkAndBoil(Player player, Location loc) {
        Block block = loc.getBlock();
        if (block.getType() != Material.WATER_CAULDRON) return; // Đảm bảo vẫn là vạc nước

        Block blockBelow = block.getRelative(BlockFace.DOWN);
        // Kiểm tra nhiều loại block có lửa/nhiệt bên dưới
        if (blockBelow.getType() == Material.FIRE || blockBelow.getType() == Material.CAMPFIRE || 
            blockBelow.getType() == Material.SOUL_FIRE || blockBelow.getType() == Material.SOUL_CAMPFIRE || 
            blockBelow.getType() == Material.MAGMA_BLOCK) {
            
            // Nếu vạc đã hoặc đang được đun, bỏ qua
            if (boilingCauldrons.containsKey(loc) || readyCauldrons.contains(loc)) {
                DebugLogger.log("CauldronAlchemy", "Cauldron at " + loc.toVector() + " is already boiling or ready.");
                return;
            }
            
            long boilTime = plugin.getConfig().getLong("alchemy.settings.water-boil-time-seconds", 10); // Lấy thời gian từ config
            player.sendMessage(format("&bBạn bắt đầu đun nước trong vạc..."));
            DebugLogger.log("CauldronAlchemy", "Started boiling cauldron at " + loc.toVector() + " for " + boilTime + " seconds.");
            
            BukkitRunnable boilTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Kiểm tra lại trạng thái của vạc trước khi hoàn thành task
                    if (loc.getBlock().getType() != Material.WATER_CAULDRON) {
                        DebugLogger.log("CauldronAlchemy", "Cauldron at " + loc.toVector() + " is no longer WATER_CAULDRON. Cancelling boil task.");
                        boilingCauldrons.remove(loc);
                        return;
                    }
                    player.sendMessage(format("&aNước trong vạc đã sôi, có thể bắt đầu luyện đan!"));
                    loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0f, 1.0f);
                    loc.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 1, 0.5), 10, 0.2, 0.1, 0.2, 0.01);
                    
                    readyCauldrons.add(loc); // Đánh dấu vạc đã sôi
                    boilingCauldrons.remove(loc); // Xóa khỏi danh sách đang đun
                    DebugLogger.log("CauldronAlchemy", "Cauldron at " + loc.toVector() + " is READY for alchemy.");
                }
            };
            
            boilTask.runTaskLater(plugin, boilTime * 20L); // Chạy task sau N giây (20 ticks/giây)
            boilingCauldrons.put(loc, boilTask); // Lưu task lại
        } else {
            player.sendMessage(format("&cCần có lửa hoặc khối magma bên dưới vạc để đun nước."));
            DebugLogger.log("CauldronAlchemy", "No valid heat source below cauldron at " + loc.toVector());
        }
    }
    
    /**
     * Kiểm tra các dược liệu trong vạc có khớp với công thức nào không.
     */
    private void checkForRecipe(Player player, Location cauldronLoc, CauldronSession session) {
        ConfigurationSection recipesSection = plugin.getConfig().getConfigurationSection("alchemy.recipes");
        if (recipesSection == null) {
            DebugLogger.warn("CauldronAlchemy", "No alchemy recipes found in config.yml!");
            return;
        }
        
        String matchedRecipeId = null;
        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection ingredientsSection = recipesSection.getConfigurationSection(recipeId + ".ingredients");
            if (ingredientsSection == null) continue;

            Map<String, Object> requiredIngredients = ingredientsSection.getValues(false);
            
            if (ingredientsMatch(session.ingredients, requiredIngredients)) {
                matchedRecipeId = recipeId;
                break; // Tìm thấy công thức đầu tiên khớp
            }
        }

        if (matchedRecipeId != null) {
            String finalRecipeId = matchedRecipeId; // Cần final để dùng trong Anonymous class BukkitRunnable
            
            // Nếu đã có task luyện đan đang chạy cho vạc này, hủy nó trước
            if (session.craftTask != null && !session.craftTask.isCancelled()) {
                session.craftTask.cancel();
                DebugLogger.log("CauldronAlchemy", "Cancelled existing crafting task for cauldron at " + cauldronLoc.toVector());
            }

            player.sendMessage(format("&eDược liệu đã đủ, lò luyện bắt đầu khởi động..."));
            cauldronLoc.getWorld().playSound(cauldronLoc, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.8f);
            DebugLogger.log("CauldronAlchemy", "Starting crafting for recipe: " + finalRecipeId + " at " + cauldronLoc.toVector());

            long craftTime = plugin.getConfig().getLong("alchemy.settings.cauldron-craft-time-seconds", 30);

            session.craftTask = new BukkitRunnable() {
                @Override
                public void run() {
                    DebugLogger.log("CauldronAlchemy", "Crafting task completed for cauldron at " + cauldronLoc.toVector());
                    
                    // Xóa session và trạng thái của vạc
                    craftingCauldrons.remove(cauldronLoc);
                    readyCauldrons.remove(cauldronLoc); // Đánh dấu là không còn sẵn sàng

                    // Làm cạn nước trong vạc (chuyển về vạc rỗng)
                    Block cauldronBlock = cauldronLoc.getBlock();
                    if (cauldronBlock.getType() == Material.WATER_CAULDRON) {
                        cauldronBlock.setType(Material.CAULDRON);
                        DebugLogger.log("CauldronAlchemy", "Cauldron at " + cauldronLoc.toVector() + " emptied.");
                    } else {
                        DebugLogger.warn("CauldronAlchemy", "Cauldron at " + cauldronLoc.toVector() + " was not WATER_CAULDRON upon task completion.");
                    }
                    
                    // Random ra phẩm chất của đan dược
                    String tierId = getRandomPillTier(finalRecipeId);
                    
                    // Tạo item Đan Dược "Nửa Mùa"
                    ItemStack semiPill = createSemiFinishedPill(finalRecipeId, tierId);
                    
                    if (semiPill != null) {
                        // Thả đan dược ra thế giới với một lực đẩy nhẹ
                        Item droppedItem = cauldronLoc.getWorld().dropItem(cauldronLoc.clone().add(0.5, 1.2, 0.5), semiPill);
                        droppedItem.setVelocity(new Vector(0, 0.2, 0)); // Đẩy lên trên
                        
                        player.sendMessage(format("&6&lLuyện đan thành công! &fBạn nhận được một viên đan dược cần được tinh luyện."));
                        cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        DebugLogger.log("CauldronAlchemy", "Dropped semi-finished pill: " + semiPill.getItemMeta().getDisplayName());

                        // DEBUG: Kiểm tra NBT của semiPill sau khi tạo và drop
                        if (semiPill.hasItemMeta() && semiPill.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "semi_finished_pill"), PersistentDataType.STRING)) {
                            DebugLogger.log("CauldronAlchemy", "Semi-finished pill HAS NBT tag: " + semiPill.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "semi_finished_pill"), PersistentDataType.STRING));
                        } else {
                            DebugLogger.warn("CauldronAlchemy", "ERROR: Semi-finished pill DOES NOT HAVE NBT tag after creation!");
                        }

                    } else {
                        player.sendMessage(format("&cLỗi khi tạo đan dược, vui lòng báo admin."));
                        DebugLogger.warn("CauldronAlchemy", "createSemiFinishedPill returned null for " + finalRecipeId + ":" + tierId);
                    }
                }
            }.runTaskLater(plugin, craftTime * 20L); // Chạy task sau một khoảng thời gian
        } else {
            DebugLogger.log("CauldronAlchemy", "No matching recipe found for ingredients in cauldron at " + cauldronLoc.toVector() + ". Current ingredients: " + session.ingredients);
        }
    }

    /**
     * So sánh các nguyên liệu được cung cấp với nguyên liệu yêu cầu của công thức.
     */
    private boolean ingredientsMatch(Map<String, Integer> provided, Map<String, Object> required) {
        if (provided.size() != required.size()) {
            DebugLogger.log("CauldronAlchemy", "Ingredients mismatch: Provided size " + provided.size() + ", Required size " + required.size());
            return false;
        }
        for (Map.Entry<String, Object> entry : required.entrySet()) {
            String requiredHerbId = entry.getKey();
            int requiredAmount = (Integer) entry.getValue(); // Giá trị từ config là Integer
            
            if (!provided.containsKey(requiredHerbId) || provided.get(requiredHerbId) < requiredAmount) {
                DebugLogger.log("CauldronAlchemy", "Ingredients mismatch: Missing " + requiredHerbId + " or not enough. Provided: " + provided.getOrDefault(requiredHerbId, 0) + ", Required: " + requiredAmount);
                return false;
            }
        }
        DebugLogger.log("CauldronAlchemy", "Ingredients match! Provided: " + provided + ", Required: " + required);
        return true;
    }
    
    /**
     * Tìm một vạc đã sôi và sẵn sàng gần vị trí đã cho.
     */
    private Location findNearbyReadyCauldron(Location loc, double radius) {
        for (Location readyLoc : readyCauldrons) {
            // Kiểm tra cùng thế giới và trong bán kính
            if (Objects.equals(readyLoc.getWorld(), loc.getWorld()) && readyLoc.distanceSquared(loc) <= radius * radius) {
                return readyLoc;
            }
        }
        return null;
    }
    
    /**
     * Random ra một phẩm chất (tier) cho đan dược dựa trên tỷ lệ trong công thức.
     */
    private String getRandomPillTier(String recipeId) {
        ConfigurationSection chanceSection = plugin.getConfig().getConfigurationSection("alchemy.recipes." + recipeId + ".tier-chance");
        if (chanceSection == null) {
            DebugLogger.warn("CauldronAlchemy", "No tier-chance section for recipe " + recipeId + ". Defaulting to 'tam_pham'.");
            return "tam_pham";
        }

        int totalWeight = chanceSection.getKeys(false).stream().mapToInt(chanceSection::getInt).sum();
        if (totalWeight <= 0) {
            DebugLogger.warn("CauldronAlchemy", "Total weight for recipe " + recipeId + " is 0. Defaulting to 'tam_pham'.");
            return "tam_pham";
        }

        int randomNum = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (String key : chanceSection.getKeys(false)) {
            currentWeight += chanceSection.getInt(key);
            if (randomNum < currentWeight) {
                DebugLogger.log("CauldronAlchemy", "Random pill tier for recipe " + recipeId + ": " + key);
                return key;
            }
        }
        DebugLogger.warn("CauldronAlchemy", "Failed to randomly select pill tier. Defaulting to 'tam_pham'.");
        return "tam_pham"; // Dự phòng
    }

    /**
     * Tạo ra một ItemStack cho Đan Dược "Nửa Mùa" với NBT tag để nhận diện.
     */
    private ItemStack createSemiFinishedPill(String pillId, String tierId) {
        ItemStack pill = new ItemStack(Material.MAGMA_CREAM); // Ngoại hình là Kem Dung Nham
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = plugin.getConfig().getString("alchemy.pills." + pillId + "." + tierId + ".display-name", "Đan Dược Lỗi");
        meta.setDisplayName(format("&e" + displayName + " &7(Nửa Mùa)"));
        
        List<String> lore = new ArrayList<>();
        lore.add(format("&7Viên đan dược chưa được tinh luyện hoàn toàn."));
        lore.add(format("&7Hãy cho vào lò nung để hoàn tất."));
        meta.setLore(lore);

        // Lưu NBT tag vào item để listener lò nung nhận biết
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "semi_finished_pill"), PersistentDataType.STRING, pillId + ":" + tierId);
        
        pill.setItemMeta(meta);
        return pill;
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}