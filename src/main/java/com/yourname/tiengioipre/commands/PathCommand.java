package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xử lý tất cả các lệnh liên quan đến Con Đường Tu Luyện.
 */
public class PathCommand implements CommandExecutor, TabCompleter {

    private final TienGioiPre plugin;

    public PathCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Lấy tên lệnh được gõ (ví dụ: "conduongtuluyen", "kiemtu", ...)
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "conduongtuluyen":
                handleMainCommand(sender, args, label);
                break;
            // Các lệnh chọn con đường sẽ đi vào case này
            case "kiemtu":
            case "matu":
            case "phattu":
            case "luyenkhisu": // <-- ĐÃ THÊM LỆNH MỚI
                handlePathSelection(sender, cmdName);
                break;
        }
        return true;
    }

    /**
     * Xử lý lệnh chính /conduongtuluyen.
     */
    private void handleMainCommand(CommandSender sender, String[] args, String label) {
        // Lệnh set của Admin
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("tiengioipre.admin")) {
                sender.sendMessage(format("&cBạn không có quyền sử dụng lệnh này."));
                return;
            }
            setPlayerPathByAdmin(sender, args, label);
            return;
        }

        // Lệnh của người chơi
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới có thể xem danh sách con đường.");
            return;
        }
        
        // Hiển thị tin nhắn hướng dẫn chọn con đường
        List<String> messages = plugin.getConfig().getStringList("paths.settings.path-selection-message");
        for (String msg : messages) {
            sender.sendMessage(format(msg));
        }
    }

    /**
     * Xử lý khi người chơi gõ các lệnh chọn con đường như /kiemtu, /matu, v.v.
     */
    private void handlePathSelection(CommandSender sender, String pathId) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(format("&cLệnh này chỉ dành cho người chơi."));
            return;
        }

        Player player = (Player) sender;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        if (data == null) {
            player.sendMessage(format("&cKhông thể tải dữ liệu của bạn, vui lòng thử lại."));
            return;
        }

        // Kiểm tra xem người chơi có được chọn lại hay không
        boolean firstChoiceOnly = plugin.getConfig().getBoolean("paths.settings.first-choice-only", true);
        if (firstChoiceOnly && data.getCultivationPath() != null && !data.getCultivationPath().equals("none")) {
            player.sendMessage(format("&cBạn đã chọn con đường tu luyện của mình, không thể thay đổi!"));
            return;
        }

        // Thực hiện việc chọn con đường
        setPathForPlayer(player, pathId);
    }
    
    /**
     * Xử lý khi admin dùng lệnh /cdtl set <player> <path>.
     */
    private void setPlayerPathByAdmin(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(format("&cUsage: /" + label + " set <player> <path_id>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format("&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        
        String pathId = args[2].toLowerCase();
        if (!plugin.getConfig().contains("paths." + pathId)) {
            sender.sendMessage(format("&cCon đường tu luyện '" + pathId + "' không tồn tại trong config.yml."));
            return;
        }
        
        // Thực hiện việc đặt con đường và thông báo cho admin
        setPathForPlayer(target, pathId);
        sender.sendMessage(format("&aĐã đặt con đường tu luyện cho " + target.getName() + " thành: " + pathId));
    }
    
    /**
     * Hàm trung tâm để đặt con đường cho một người chơi và áp dụng thay đổi.
     */
    private void setPathForPlayer(Player player, String pathId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        data.setCultivationPath(pathId);
        
        // Áp dụng lại toàn bộ stats (từ cảnh giới + con đường mới)
        plugin.getRealmManager().applyAllStats(player);
        
        String displayName = plugin.getConfig().getString("paths." + pathId + ".display-name", pathId);
        player.sendMessage(format("&aBạn đã chính thức bước lên con đường của một " + displayName + "!"));
    }
    
    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Xử lý gợi ý lệnh (tab-complete).
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("conduongtuluyen")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            if (sender.hasPermission("tiengioipre.admin")) {
                return filter(Collections.singletonList("set"), args[0]);
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            if (sender.hasPermission("tiengioipre.admin")) {
                return null; // Gợi ý tên người chơi
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (sender.hasPermission("tiengioipre.admin")) {
                // Gợi ý các ID của con đường tu luyện từ config
                return filter(plugin.getConfig().getConfigurationSection("paths").getKeys(false), args[2]);
            }
        }
        
        return Collections.emptyList();
    }

    private List<String> filter(Iterable<String> list, String start) {
        return ((List<String>) list).stream()
                .filter(s -> s.toLowerCase().startsWith(start.toLowerCase()))
                .collect(Collectors.toList());
    }
}