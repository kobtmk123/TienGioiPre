package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.Realm;
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
            player.sendMessage(ChatColor.RED + "Dữ liệu của bạn chưa được tải, vui lòng thử lại sau giây lát.");
            return true;
        }

        Realm currentRealm = plugin.getRealmManager().getRealm(data.getRealmId());
        if (currentRealm == null) {
            player.sendMessage(ChatColor.RED + "Lỗi: Không tìm thấy cảnh giới hiện tại của bạn.");
            return true;
        }

        if (currentRealm.nextRealmId().equalsIgnoreCase("none")) {
            player.sendMessage(ChatColor.YELLOW + "Bạn đã đạt tới cảnh giới cao nhất, không thể đột phá thêm.");
            return true;
        }

        if (data.getLinhKhi() >= currentRealm.maxLinhKhi()) {
            // Đột phá
            Realm nextRealm = plugin.getRealmManager().getRealm(currentRealm.nextRealmId());
            if (nextRealm == null) {
                player.sendMessage(ChatColor.RED + "Lỗi: Cảnh giới tiếp theo không được cấu hình đúng.");
                return true;
            }

            // Tạo hiệu ứng sét
            player.getWorld().strikeLightningEffect(player.getLocation());
            // Gây sát thương (nếu có)
            if (currentRealm.breakthroughLightningDamage() > 0) {
                player.damage(currentRealm.breakthroughLightningDamage());
                if (player.isDead()) {
                    // Người chơi đã chết, không cần làm gì thêm. Có thể thêm message ở PlayerDeathEvent
                    // Ví dụ: Độ kiếp thất bại!
                    return true;
                }
            }
            
            // Cập nhật dữ liệu
            data.setRealmId(nextRealm.id());
            data.setLinhKhi(0); // Reset linh khí về 0

            // Cập nhật stats
            plugin.getRealmManager().applyRealmStats(player, nextRealm.id());
            
            String message = "&b&lChúc mừng! &fBạn đã đột phá thành công lên cảnh giới %realm%&f!";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message.replace("%realm%", nextRealm.displayName())));

        } else {
            // Không đủ linh khí
            String message = "&cBạn chưa đủ linh khí để đột phá! Cần &e%required%&c, bạn đang có &e%current%&c.";
            message = message.replace("%required%", String.format("%.0f", currentRealm.maxLinhKhi()));
            message = message.replace("%current%", String.format("%.0f", data.getLinhKhi()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        return true;
    }
}