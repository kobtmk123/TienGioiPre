package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PathCommand implements CommandExecutor {

    private final TienGioiPre plugin;

    public PathCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "conduongtuluyen":
                handleMainCommand(sender, args);
                break;
            case "kiemtu":
            case "matu":
            case "phattu":
                handlePathSelection(sender, cmd);
                break;
        }
        return true;
    }

    private void handleMainCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            // Xử lý lệnh admin từ console
            if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
                setPlayerPath(sender, args);
            } else {
                 sender.sendMessage("Usage: /conduongtuluyen set <player> <path>");
            }
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("set") && sender.hasPermission("tiengioipre.admin")) {
            setPlayerPath(sender, args);
        } else {
            // Hiển thị tin nhắn chọn con đường cho người chơi
            List<String> messages = plugin.getConfig().getStringList("paths.settings.path-selection-message");
            messages.forEach(msg -> sender.sendMessage(format(msg)));
        }
    }

    private void handlePathSelection(CommandSender sender, String pathId) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return;
        }
        Player player = (Player) sender;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        boolean firstChoiceOnly = plugin.getConfig().getBoolean("paths.settings.first-choice-only", true);
        if (firstChoiceOnly && data.getCultivationPath() != null && !data.getCultivationPath().equals("none")) {
            player.sendMessage(format("&cBạn đã chọn con đường tu luyện của mình, không thể thay đổi!"));
            return;
        }

        setPath(player, pathId);
    }
    
    private void setPlayerPath(CommandSender sender, String[] args) {
        // /cdtl set <player> <path>
        if (args.length < 3) {
            sender.sendMessage(format("&cUsage: /conduongtuluyen set <player> <path>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format("&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        String pathId = args[2].toLowerCase();
        if (!plugin.getConfig().contains("paths." + pathId)) {
            sender.sendMessage(format("&cCon đường tu luyện '" + pathId + "' không tồn tại."));
            return;
        }
        setPath(target, pathId);
        sender.sendMessage(format("&aĐã đặt con đường tu luyện cho " + target.getName() + " thành " + pathId));
    }
    
    private void setPath(Player player, String pathId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        data.setCultivationPath(pathId);
        plugin.getRealmManager().applyAllStats(player); // Áp dụng lại stats
        String displayName = plugin.getConfig().getString("paths." + pathId + ".display-name", pathId);
        player.sendMessage(format("&aBạn đã chọn con đường của một " + displayName + "!"));
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}