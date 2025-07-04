package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DotPhaCommand implements CommandExecutor {
    private final TienGioiPre plugin;
    private final Set<UUID> playersInTribulation = new HashSet<>();
    private final Random random = new Random();

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

        // Kiểm tra xem đã đủ linh khí chưa
        if (data.getLinhKhi() >= currentTier.maxLinhKhi()) {
            
            // Lấy cài đặt tỷ lệ từ config
            boolean chanceEnabled = plugin.getConfig().getBoolean("settings.breakthrough-chance.enabled", false);
            int successRate = plugin.getConfig().getInt("settings.breakthrough-chance.success-rate", 100);

            // Nếu hệ thống tỷ lệ được bật VÀ random thất bại
            if (chanceEnabled && random.nextInt(100) >= successRate) {
                // ĐỘT PHÁ THẤT BẠI
                player.sendMessage(format("&c&lĐáng Tiếc! &7Lần đột phá này đã thất bại, linh khí hỗn loạn. Hãy thử lại lần sau!"));
                // Trừ 25% linh khí khi thất bại, có thể tùy chỉnh
                data.setLinhKhi(data.getLinhKhi() * 0.75); 
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                // ĐỘT PHÁ THÀNH CÔNG (hoặc khi hệ thống tỷ lệ bị tắt)
                // Bắt đầu quá trình Lôi Kiếp
                handleBreakthrough(player, data, currentTier);
            }
        } else {
            // Không đủ linh khí
            String message = "&cBạn chưa đủ linh khí để đột phá! Cần &e%required%&c, bạn đang có &e%current%&c.";
            message = message.replace("%required%", String.format("%,.0f", currentTier.maxLinhKhi()));
            message = message.replace("%current%", String.format("%,.0f", data.getLinhKhi()));
            player.sendMessage(format(message));
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
            return;
        }
        
        ConfigurationSection breakthroughConfig = currentTier.breakthroughConfig();
        
        if (breakthroughConfig == null) {
             player.sendMessage(format("&c[Lỗi] Cấu hình đột phá cho cảnh giới này bị thiếu. Vui lòng báo Admin."));
             return;
        }
        
        String tribulationName = format(breakthroughConfig.getString("tribulation-name", "&c&lThiên Kiếp"));
        int lightningCount = breakthroughConfig.getInt("lightning-count", 1);
        double lightningDamage = breakthroughConfig.getDouble("lightning-damage", 2.0);
        long lightningDelay = breakthroughConfig.getLong("lightning-delay-ticks", 20L);
        
        // Nếu không có sét đánh, thăng cấp ngay lập tức
        if (lightningCount <= 0) {
            completeBreakthrough(player, data, currentTier);
            return;
        }
        
        // Bắt đầu quá trình độ kiếp
        playersInTribulation.add(player.getUniqueId());
        player.sendMessage(format("&4&lBầu trời đột nhiên u ám, " + tribulationName + " sắp giáng lâm!"));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        new BukkitRunnable() {
            int strikes = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    if (player.isDead()) {
                         player.sendMessage(format("&cĐộ kiếp thất bại, thân tử đạo tiêu!"));
                    }
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.damage(lightningDamage);
                strikes++;
                player.sendMessage(format(tribulationName + " - Đạo thứ &c" + strikes + "&4&l!"));
                
                if (strikes >= lightningCount) {
                    if (!player.isDead()) {
                        completeBreakthrough(player, data, currentTier);
                    }
                    playersInTribulation.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 40L, lightningDelay);
    }

    /**
     * Hoàn tất việc thăng cấp sau khi sống sót qua lôi kiếp.
     */
    private void completeBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        RealmManager realmManager = plugin.getRealmManager();
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());
        
        if (nextProgression == null) return;
        
        String nextRealmId = nextProgression[0];
        String nextTierId = nextProgression[1];

        double remainingLinhKhi = data.getLinhKhi() - currentTier.maxLinhKhi();

        data.setRealmId(nextRealmId);
        data.setTierId(nextTierId);
        data.setLinhKhi(remainingLinhKhi);
        
        realmManager.applyAllStats(player);
        
        String realmName = realmManager.getRealmDisplayName(nextRealmId);
        String tierName = realmManager.getTierDisplayName(nextRealmId, nextTierId);
        
        // Tin nhắn riêng
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
    }
    
    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}