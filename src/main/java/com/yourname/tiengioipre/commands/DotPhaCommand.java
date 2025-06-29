package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DotPhaCommand implements CommandExecutor {
    private final TienGioiPre plugin;

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

        // Kiểm tra điều kiện đột phá
        if (data.getLinhKhi() >= currentTier.maxLinhKhi()) {
            
            String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());

            if (nextProgression == null) {
                player.sendMessage(format("&eBạn đã đạt tới cảnh giới cao nhất, không thể đột phá thêm!"));
                return true;
            }

            String nextRealmId = nextProgression[0];
            String nextTierId = nextProgression[1];
            
            // Gây hiệu ứng thiên kiếp
            player.getWorld().strikeLightningEffect(player.getLocation());
            if (currentTier.breakthroughLightningDamage() > 0) {
                player.damage(currentTier.breakthroughLightningDamage());
                if (player.isDead()) {
                    // Plugin không cần gửi tin nhắn, Minecraft đã có tin nhắn chết
                    return true;
                }
            }

            // Tính toán linh khí dư
            double remainingLinhKhi = data.getLinhKhi() - currentTier.maxLinhKhi();

            // Cập nhật dữ liệu người chơi
            data.setRealmId(nextRealmId);
            data.setTierId(nextTierId);
            data.setLinhKhi(remainingLinhKhi);
            
            // Áp dụng stats mới (ĐÃ SỬA LỖI)
            realmManager.applyAllStats(player);
            
            // Gửi tin nhắn chúc mừng
            String realmName = realmManager.getRealmDisplayName(nextRealmId);
            String tierName = realmManager.getTierDisplayName(nextRealmId, nextTierId);
            player.sendMessage(format("&b&lChúc Mừng! &fBạn đã đột phá thành công lên &b" + realmName + " - " + tierName + "&f!"));

        } else {
            // Không đủ linh khí
            String message = "&cBạn chưa đủ linh khí để đột phá! Cần &e%required%&c, bạn đang có &e%current%&c.";
            message = message.replace("%required%", String.format("%,.0f", currentTier.maxLinhKhi()));
            message = message.replace("%current%", String.format("%,.0f", data.getLinhKhi()));
            player.sendMessage(format(message));
        }

        return true;
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}