package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (clickedBlock.getType() == Material.WATER_CAULDRON) {
                    checkAndBoil(player, cauldronLoc);
                }
            }, 1L);
            return;
        }

        // 2. Xử lý khi bỏ dược liệu vào vạc ĐÃ SÔI
        if (clickedBlock.getType() == Material.WATER_CAULDRON && readyCauldrons.contains(cauldronLoc)) {
            handleIngredientAdd(player, itemInHand, cauldronLoc, event);
        }
    }

    private void handleIngredientAdd(Player player, ItemStack ingredient, Location cauldronLoc, PlayerInteractEvent event) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null || !"luyendansu".equals(playerData.getCultivationPath())) {
            player.sendMessage(format("&cChỉ Luyện Đan Sư mới có thể luyện đan."));
            return;
        }

        if (ingredient == null || ingredient.getType() == Material.AIR || !ingredient.hasItemMeta()) return;
        ItemMeta meta = ingredient.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING)) return;
        
        String herbId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
        
        if (!plugin.getConfig().contains("items." + herbId) || !plugin.getConfig().contains("alchemy.herbs." + herbId)) return;
        
        event.setCancelled(true);

        CauldronSession session = craftingCauldrons.computeIfAbsent(cauldronLoc, k -> new CauldronSession());
        session.ingredients.merge(herbId, 1, Integer::sum);
        
        ingredient.setAmount(ingredient.getAmount() - 1);
        
        player.sendMessage(format("&aĐã thêm 1x " + meta.getDisplayName() + " &avào vạc."));
        cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.5f);
        
        checkForRecipe(player, cauldronLoc, session);
    }
    
    private void checkAndBoil(Player player, Location loc) {
        Block block = loc.getBlock();
        if (block.getType() != Material.WATER_CAULDRON) return;

        Block blockBelow = block.getRelative(BlockFace.DOWN);
        if (blockBelow.getType() == Material.FIRE || blockBelow.getType() == Material.CAMPFIRE || blockBelow.getType() == Material.SOUL_FIRE || blockBelow.getType() == Material.SOUL_CAMPFIRE || blockBelow.getType() == Material.MAGMA_BLOCK) {
            if (boilingCauldrons.containsKey(loc) || readyCauldrons.contains(loc)) return;
            
            long boilTime = plugin.getConfig().getLong("alchemy.settings.water-boil-time-seconds", 60);
            player.sendMessage(format("&bBạn bắt đầu đun nước trong vạc..."));
            
            BukkitRunnable boilTask = new BukkitRunnable() {
                @Override
                public void run() {
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
    
    private void checkForRecipe(Player player, Location cauldronLoc, CauldronSession session) {
        ConfigurationSection recipesSection = plugin.getConfig().getConfigurationSection("alchemy.recipes");
        if (recipesSection == null) return;
        
        String matchedRecipeId = null;
        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection ingredientsSection = recipesSection.getConfigurationSection(recipeId + ".ingredients");
            if (ingredientsSection == null) continue;

            Map<String, Object> requiredIngredients = ingredientsSection.getValues(false);
            
            if (ingredientsMatch(session.ingredients, requiredIngredients)) {
                matchedRecipeId = recipeId;
                break;
            }
        }

        if (matchedRecipeId != null) {
            String finalRecipeId = matchedRecipeId;
            
            if (session.craftTask != null && !session.craftTask.isCancelled()) session.craftTask.cancel();

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
                    
                    String tierId = getRandomPillTier(finalRecipeId);
                    ItemStack semiPill = createSemiFinishedPill(finalRecipeId, tierId);
                    
                    if (semiPill != null) {
                        cauldronLoc.getWorld().dropItem(cauldronLoc.clone().add(0.5, 1.2, 0.5), semiPill).setVelocity(new Vector(0, 0.2, 0));
                        player.sendMessage(format("&6&lLuyện đan thành công! &fBạn nhận được một viên đan dược cần được tinh luyện."));
                        cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    } else {
                        player.sendMessage(format("&cLỗi khi tạo đan dược, vui lòng báo admin."));
                    }
                }
            }.runTaskLater(plugin, craftTime * 20L);
            return;
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
        if (chanceSection == null) return "tam_pham";

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

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "semi_finished_pill"), PersistentDataType.STRING, pillId + ":" + tierId);
        
        pill.setItemMeta(meta);
        return pill;
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}