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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xử lý tất cả các lệnh liên quan đến Con Đường Tu Luyện.
 * Bao gồm /conduongtuluyen và các lệnh chọn con đường cụ thể.
 */
public class PathCommand implements CommandExecutor, TabCompleter {

    private final TienGioiPre plugin;
    private final String prefix = "&8[&bTiên Giới&8] &r";

    public PathCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "conduongtuluyen":
                handleMainCommand(sender, args, label);
                break;
            // Tất cả các lệnh chọn con đường đều đi vào đây
            case "kiemtu":
            case "matu":
            case "phattu":
            case "luyenkhisu":
                handlePathSelection(sender, cmdName);
                break;
        }
        return true;
    }

    /**
     * Xử lý lệnh chính /conduongtuluyen và lệnh /cdtl set của admin.
     */
    private void handleMainCommand(CommandSender sender, String[] args, String label) {
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("tiengioipre.admin")) {
                sender.sendMessage(format(prefix + "&cBạn không có quyền sử dụng lệnh này."));
                return;
            }
            setPlayerPathByAdmin(sender, args, label);
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới có thể xem danh sách con đường.");
            return;
        }
        
        List<String> messages = plugin.getConfig().getStringList("paths.settings.path-selection-message");
        for (String msg : messages) {
            sender.sendMessage(format(msg));
        }
    }

    /**
     * Xử lý khi người chơi gõ các lệnh chọn con đường như /kiemtu, /luyendansu, v.v.
     */
    private void handlePathSelection(CommandSender sender, String pathId) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(format("&cLệnh này chỉ dành cho người chơi."));
            return;
        }

        Player player = (Player) sender;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        if (data == null) {
            player.sendMessage(format(prefix + "&cKhông thể tải dữ liệu của bạn, vui lòng thử lại."));
            return;
        }

        boolean firstChoiceOnly = plugin.getConfig().getBoolean("paths.settings.first-choice-only", true);
        if (firstChoiceOnly && data.getCultivationPath() != null && !data.getCultivationPath().equals("none")) {
            player.sendMessage(format(prefix + "&cBạn đã chọn con đường tu luyện của mình, không thể thay đổi!"));
            return;
        }

        setPathForPlayer(player, pathId);
    }
    
    /**
     * Xử lý khi admin dùng lệnh /cdtl set <player> <path>.
     */
    private void setPlayerPathByAdmin(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " set <player> <path_id>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        
        String pathId = args[2].toLowerCase();
        if (!plugin.getConfig().contains("paths." + pathId)) {
            sender.sendMessage(format(prefix + "&cCon đường tu luyện '" + pathId + "' không tồn tại trong config.yml."));
            return;
        }
        
        setPathForPlayer(target, pathId);
        sender.sendMessage(format(prefix + "&aĐã đặt con đường tu luyện cho " + target.getName() + " thành: " + pathId));
    }
    
    /**
     * Hàm trung tâm để đặt con đường cho một người chơi và áp dụng thay đổi.
     */
    private void setPathForPlayer(Player player, String pathId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        data.setCultivationPath(pathId);
        plugin.getRealmManager().applyAllStats(player);
        
        String displayName = plugin.getConfig().getString("paths." + pathId + ".display-name", pathId);
        player.sendMessage(format(prefix + "&aBạn đã chính thức bước lên con đường của một " + displayName + "!"));
    }
    
    private String format(String msg) {
        return plugin.getTongMonManager().format(msg);
    }

    /**
     * Xử lý gợi ý lệnh (tab-complete) cho /conduongtuluyen.
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
                // Gợi ý các ID của con đường tu luyện từ config (trừ mục 'settings')
                List<String> pathIds = new ArrayList<>(plugin.getConfig().getConfigurationSection("paths").getKeys(false));
                pathIds.remove("settings");
                return filter(pathIds, args[2]);
            }
        }
        
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String start) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(start.toLowerCase()))
                .collect(Collectors.toList());
    }
}