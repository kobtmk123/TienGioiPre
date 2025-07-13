package com.myname.tiengioipre.commands;

import com.myname.tiengioipre.TienGioiPre;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TuLuyenCommand implements CommandExecutor {

    private final TienGioiPre plugin;

    public TuLuyenCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getCultivationManager().isCultivating(player)) {
            plugin.getCultivationManager().stopCultivating(player);
            // Thay thế bằng message từ messages.yml
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bTienGioi&8] &cBạn đã dừng tu luyện."));
        } else {
            plugin.getCultivationManager().startCultivating(player);
            // Thay thế bằng message từ messages.yml
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bTienGioi&8] &aBạn đã bắt đầu trạng thái tu luyện. Nhấn &eSHIFT&a để thoát."));
        }

        return true;
    }
}