package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Xử lý lệnh /dotpha của người chơi.
 */
public class DotPhaCommand implements CommandExecutor {
    private final TienGioiPre plugin;
    // Set để ngăn người chơi spam lệnh /dotpha khi đang trong quá trình độ kiếp
    private final Set<UUID> playersInTribulation = new HashSet<>();

    public DotPhaCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;

        // Ngăn spam lệnh
        if (playersInTribulation.contains(player.getUniqueId())) {
            player.sendMessage(format("&cBạn đang trong quá trình độ kiếp, không thể thực hiện!"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage(format("&c[Lỗi] Không thể tải dữ liệu của bạn. Vui lòng thử lại."));
            return true;
        }

        RealmManager realmManager = plugin.getRealmManager();
        RealmManager.TierData currentTier = realmManager.getTierData(data.getRealmId(), data.getTierId());

        if (currentTier == null) {
            player.sendMessage(format("&c[Lỗi] Cảnh giới hiện tại của bạn không hợp lệ. Vui lòng liên hệ Admin."));
            return true;
        }

        // Kiểm tra điều kiện có đủ linh khí để đột phá không
        if (data.getLinhKhi() >= currentTier.maxLinhKhi()) {
            handleBreakthrough(player, data, currentTier);
        } else {
            // Gửi tin nhắn không đủ linh khí
            String message = "&cBạn chưa đủ linh khí để đột phá! Cần &e%required%&c, bạn đang có &e%current%&c.";
            message = message.replace("%required%", String.format("%,.0f", currentTier.maxLinhKhi()));
            message = message.replace("%current%", String.format("%,.0f", data.getLinhKhi()));
            player.sendMessage(format(message));
        }

        return true;
    }

    /**
     * Bắt đầu quá trình đột phá và lôi kiếp.
     */
    private void handleBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        RealmManager realmManager = plugin.getRealmManager();
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());

        if (nextProgression == null) {
            player.sendMessage(format("&eBạn đã đạt tới cảnh giới cao nhất, không thể đột phá thêm!"));
            return;
        }
        
        // Đọc cấu hình lôi kiếp từ config.yml
        ConfigurationSection breakthroughConfig = plugin.getConfig().getConfigurationSection(
            "realms." + data.getRealmId() + ".tiers." + data.getTierId() + ".breakthrough"
        );
        
        if (breakthroughConfig == null) {
             player.sendMessage(format("&c[Lỗi] Cấu hình đột phá cho cảnh giới này bị thiếu. Vui lòng báo Admin."));
             return;
        }
        
        // Lấy các thông số của lôi kiếp
        String tribulationName = format(breakthroughConfig.getString("tribulation-name", "&c&lThiên Kiếp"));
        int lightningCount = breakthroughConfig.getInt("lightning-count", 1);
        double lightningDamage = breakthroughConfig.getDouble("lightning-damage", 2.0);
        long lightningDelay = breakthroughConfig.getLong("lightning-delay-ticks", 20L);
        
        // Nếu không có sét đánh, đột phá ngay lập tức
        if (lightningCount <= 0) {
            completeBreakthrough(player, data, currentTier);
            return;
        }
        
        // Nếu có sét, bắt đầu quá trình độ kiếp
        playersInTribulation.add(player.getUniqueId());
        player.sendMessage(format("&4&lBầu trời đột nhiên u ám, " + tribulationName + " sắp giáng lâm!"));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        // Tạo một task để đánh sét tuần tự
        new BukkitRunnable() {
            int strikes = 0;

            @Override
            public void run() {
                // Hủy task nếu người chơi đã thoát hoặc chết
                if (!player.isOnline() || player.isDead()) {
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Đánh một tia sét
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.damage(lightningDamage);
                strikes++;
                player.sendMessage(format(tribulationName + " - Đạo thứ &c" + strikes + "&4&l!"));
                
                // Kiểm tra xem đã hết lôi kiếp chưa
                if (strikes >= lightningCount) {
                    if (!player.isDead()) { // Nếu vẫn còn sống sau tia sét cuối cùng
                        completeBreakthrough(player, data, currentTier);
                    }
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 40L, lightningDelay); // Chờ 2 giây rồi bắt đầu đánh, sau đó lặp lại theo độ trễ
    }

    /**
     * Hoàn tất việc thăng cấp sau khi độ kiếp thành công.
     */
    private void completeBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        RealmManager realmManager = plugin.getRealmManager();
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());
        
        if (nextProgression == null) return; // Kiểm tra lại để chắc chắn
        
        String nextRealmId = nextProgression[0];
        String nextTierId = nextProgression[1];

        // Tính linh khí dư
        double remainingLinhKhi = data.getLinhKhi() - currentTier.maxLinhKhi();

        // Cập nhật dữ liệu người chơi
        data.setRealmId(nextRealmId);
        data.setTierId(nextTierId);
        data.setLinhKhi(remainingLinhKhi);
        
        // Áp dụng stats mới từ cả cảnh giới và con đường tu luyện
        realmManager.applyAllStats(player); 
        
        // Gửi tin nhắn và âm thanh chúc mừng
        String realmName = realmManager.getRealmDisplayName(nextRealmId);
        String tierName = realmManager.getTierDisplayName(nextRealmId, nextTierId);
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        player.sendMessage(format("&b&lChúc Mừng! &fBạn đã thành công độ kiếp, tiến vào &b" + realmName + " - " + tierName + "&f!"));
    }
    
    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}