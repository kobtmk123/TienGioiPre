package com.yourname.tiengioipre.commands; // <-- Đảm bảo package này đúng

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import com.yourname.tiengioipre.utils.DebugLogger; // <-- IMPORT MỚI
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey; // <-- IMPORT MỚI
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher; // <-- IMPORT MỚI
import java.util.regex.Pattern; // <-- IMPORT MỚI

public class DotPhaCommand implements CommandExecutor {
    private final TienGioiPre plugin;
    private final Set<UUID> playersInTribulation = new HashSet<>();
    private final Random random = new Random();
    private final NamespacedKey pillBonusKey;

    public DotPhaCommand(TienGioiPre plugin) {
        this.plugin = plugin;
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;

        if (playersInTribulation.contains(player.getUniqueId())) {
            player.sendMessage(format("&cBạn đang trong quá trình độ kiếp, không thể thực hiện!"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage(format("&c[Lỗi] Không thể tải dữ liệu của bạn. Vui lòng thử lại."));
            return true;
        }

        RealmManager.TierData currentTier = plugin.getRealmManager().getTierData(data.getRealmId(), data.getTierId());

        if (currentTier == null) {
            player.sendMessage(format("&c[Lỗi] Cảnh giới hiện tại của bạn không hợp lệ. Vui lòng liên hệ Admin."));
            return true;
        }

        // Kiểm tra xem đã đủ linh khí chưa
        if (data.getLinhKhi() >= currentTier.maxLinhKhi()) {
            
            boolean chanceEnabled = plugin.getConfig().getBoolean("settings.breakthrough-chance.enabled", false);
            int successRate = plugin.getConfig().getInt("settings.breakthrough-chance.success-rate", 45);
            
            // Lấy bonus từ đan dược và tự động sử dụng nó
            int pillBonus = getPillBonusAndConsume(player, currentTier);
            int finalSuccessRate = successRate + pillBonus;

            DebugLogger.log("DotPhaCommand", player.getName() + " is attempting breakthrough. Base Success: " + successRate + "%, Pill Bonus: " + pillBonus + "%, Final Rate: " + finalSuccessRate + "%");

            // Nếu hệ thống tỷ lệ được bật VÀ random thất bại
            if (chanceEnabled && random.nextInt(100) >= finalSuccessRate) {
                // ĐỘT PHÁ THẤT BẠI
                player.sendMessage(format("&c&lĐáng Tiếc! &7Lần đột phá này đã thất bại, linh khí hỗn loạn. Hãy thử lại lần sau!"));
                data.setLinhKhi(data.getLinhKhi() * 0.75); // Trừ 25% linh khí khi thất bại
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                DebugLogger.log("DotPhaCommand", player.getName() + " FAILED breakthrough. Remaining Linh Khi: " + data.getLinhKhi());
            } else {
                // ĐỘT PHÁ THÀNH CÔNG (hoặc khi hệ thống tỷ lệ bị tắt)
                // Bắt đầu quá trình Lôi Kiếp
                handleBreakthrough(player, data, currentTier);
                DebugLogger.log("DotPhaCommand", player.getName() + " PASSED breakthrough chance. Starting Tribulation.");
            }
        } else {
            String message = "&cBạn chưa đủ linh khí để đột phá! Cần &e%required%&c, bạn đang có &e%current%&c.";
            message = message.replace("%required%", String.format("%,.0f", currentTier.maxLinhKhi()));
            message = message.replace("%current%", String.format("%,.0f", data.getLinhKhi()));
            player.sendMessage(format(message));
            DebugLogger.log("DotPhaCommand", player.getName() + " does not have enough Linh Khi. Current: " + data.getLinhKhi() + ", Required: " + currentTier.maxLinhKhi());
        }

        return true;
    }
    
    /**
     * Bắt đầu quá trình Lôi Kiếp sau khi đã xác định là đột phá thành công.
     */
    private void handleBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        RealmManager realmManager = plugin.getRealmManager();
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());

        if (nextProgression == null) {
            player.sendMessage(format("&eBạn đã đạt tới cảnh giới cao nhất, không thể đột phá thêm!"));
            DebugLogger.log("DotPhaCommand", player.getName() + " is already at max realm.");
            return;
        }
        
        ConfigurationSection breakthroughConfig = currentTier.breakthroughConfig();
        if (breakthroughConfig == null) {
             player.sendMessage(format("&c[Lỗi] Cấu hình đột phá cho cảnh giới này bị thiếu. Vui lòng báo Admin."));
             DebugLogger.warn("DotPhaCommand", "Breakthrough config missing for realm: " + data.getRealmId() + ", tier: " + data.getTierId());
             return;
        }
        
        String tribulationName = format(breakthroughConfig.getString("tribulation-name", "&c&lThiên Kiếp"));
        int lightningCount = breakthroughConfig.getInt("lightning-count", 1);
        double lightningDamage = breakthroughConfig.getDouble("lightning-damage", 2.0);
        long lightningDelay = breakthroughConfig.getLong("lightning-delay-ticks", 20L);
        
        // Nếu không có sét đánh, thăng cấp ngay lập tức
        if (lightningCount <= 0) {
            DebugLogger.log("DotPhaCommand", player.getName() + " has no lightning strikes. Completing immediately.");
            completeBreakthrough(player, data, currentTier);
            return;
        }
        
        // Bắt đầu quá trình độ kiếp
        playersInTribulation.add(player.getUniqueId());
        player.sendMessage(format("&4&lBầu trời đột nhiên u ám, " + tribulationName + " sắp giáng lâm!"));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        DebugLogger.log("DotPhaCommand", player.getName() + " started tribulation. Lightning count: " + lightningCount + ", Damage: " + lightningDamage + ", Delay: " + lightningDelay);

        new BukkitRunnable() {
            int strikes = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { // Người chơi offline
                    DebugLogger.log("DotPhaCommand", player.getName() + " went offline during tribulation. Cancelling.");
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                if (player.isDead()) { // Người chơi chết
                    player.sendMessage(format("&cĐộ kiếp thất bại, thân tử đạo tiêu!"));
                    DebugLogger.log("DotPhaCommand", player.getName() + " died during tribulation. Cancelling.");
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.damage(lightningDamage);
                strikes++;
                player.sendMessage(format(tribulationName + " - Đạo thứ &c" + strikes + "&4&l!"));
                DebugLogger.log("DotPhaCommand", player.getName() + " struck by lightning #" + strikes);
                
                if (strikes >= lightningCount) {
                    DebugLogger.log("DotPhaCommand", player.getName() + " survived tribulation. Completing.");
                    completeBreakthrough(player, data, currentTier);
                    playersInTribulation.remove(player.getUniqueId()); // Xóa khỏi set
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 40L, lightningDelay); // Chờ 2 giây rồi bắt đầu đánh, sau đó lặp lại theo độ trễ
    }

    /**
     * Hoàn tất việc thăng cấp sau khi sống sót qua lôi kiếp.
     */
    private void completeBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        RealmManager realmManager = plugin.getRealmManager();
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());
        
        if (nextProgression == null) {
            DebugLogger.warn("DotPhaCommand", "completeBreakthrough: nextProgression is null for max player " + player.getName());
            return;
        }
        
        String nextRealmId = nextProgression[0];
        String nextTierId = nextProgression[1];

        double remainingLinhKhi = data.getLinhKhi() - currentTier.maxLinhKhi();

        data.setRealmId(nextRealmId);
        data.setTierId(nextTierId);
        data.setLinhKhi(remainingLinhKhi);
        
        realmManager.applyAllStats(player); 
        
        String realmName = realmManager.getRealmDisplayName(nextRealmId);
        String tierName = realmManager.getTierDisplayName(nextRealmId, nextTierId);
        
        // Tin nhắn riêng cho người chơi
        String privateMsg = "&b&lChúc Mừng! &fBạn đã thành công độ kiếp, tiến vào &b%realm_name% - %tier_name%&f!";
        privateMsg = privateMsg.replace("%realm_name%", realmName).replace("%tier_name%", tierName);
        player.sendMessage(format(privateMsg));
        
        // Thông báo toàn server
        String broadcastMsg = "&6&l[Thông Báo Tiên Giới] &eChúc mừng đạo hữu &f%player_name% &eđã thành công vượt qua thiên kiếp, chính thức đột phá lên cảnh giới &b%realm_name% - %tier_name%&e!";
        broadcastMsg = broadcastMsg.replace("%player_name%", player.getName())
                                 .replace("%realm_name%", realmName)
                                 .replace("%tier_name%", tierName);
        Bukkit.broadcastMessage(format(broadcastMsg));
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        DebugLogger.log("DotPhaCommand", player.getName() + " successfully breakthrough to " + realmName + " - " + tierName);
    }
    
    /**
     * Tìm và sử dụng đan dược trong túi đồ người chơi, trả về bonus tỷ lệ.
     */
    private int getPillBonusAndConsume(Player player, RealmManager.TierData currentTier) {
        int totalBonus = 0;
        // Duyệt qua inventory của người chơi để tìm đan dược
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            // Kiểm tra NBT tag của đan dược
            if (container.has(pillBonusKey, PersistentDataType.INTEGER)) {
                int bonus = container.get(pillBonusKey, PersistentDataType.INTEGER);
                String pillDisplayName = (meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name());

                player.sendMessage(format("&aĐã sử dụng " + pillDisplayName + " &ađể tăng &e" + bonus + "% &atỷ lệ đột phá!"));
                totalBonus += bonus;
                
                // Trừ 1 viên đan dược
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(i, item);
                DebugLogger.log("DotPhaCommand", player.getName() + " consumed pill " + pillDisplayName + " for +" + bonus + "% bonus.");
                break; // Chỉ cho dùng 1 viên mỗi lần đột phá
            }
        }
        return totalBonus;
    }
    
    private String format(String message) {
        // Hàm format từ TongMonManager (đã được cấu hình để hỗ trợ RGB)
        return plugin.getTongMonManager().format(message);
    }
}