package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.TongMonManager;
import com.yourname.tiengioipre.data.PlayerData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TongMonCommand implements CommandExecutor, TabCompleter {
    private final TienGioiPre plugin;
    private final TongMonManager tmManager;
    private final Map<UUID, String> invites = new HashMap<>(); // <UUID người được mời, ID tông môn mời>
    private final String prefix = "&8[&eTông Môn&8] &r";

    public TongMonCommand(TienGioiPre plugin) {
        this.plugin = plugin;
        this.tmManager = plugin.getTongMonManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender, label);
            return true;
        }

        String subCmd = args[0].toLowerCase();
        
        // Lệnh có thể dùng bởi Console (chỉ có 'set')
        if (!(sender instanceof Player)) {
            if (subCmd.equals("set")) {
                handleAdminSet(sender, args, label);
            } else {
                sender.sendMessage("Lệnh con này chỉ dành cho người chơi.");
            }
            return true;
        }

        Player player = (Player) sender;
        switch (subCmd) {
            case "create": handleCreate(player, args, label); break;
            case "moi": handleInvite(player, args, label); break;
            case "accept": handleAccept(player, args, label); break;
            case "kick": handleKick(player, args, label); break;
            case "xoa": case "giaitan": handleDisband(player, label); break;
            case "set": handleAdminSet(sender, args, label); break;
            default: sendHelpMessage(sender, label); break;
        }
        return true;
    }

    private void handleCreate(Player player, String[] args, String label) {
        // Cú pháp: /tm create <id> <tên_hiển_thị>
        if (args.length < 3) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " create <ID_Tông_Môn> <Tên Hiển Thị>"));
            player.sendMessage(format(prefix + "&7ID không được có dấu cách và màu. Tên hiển thị có thể có màu."));
            player.sendMessage(format(prefix + "&7Ví dụ: /" + label + " create HacKiemMon &#FF0000Hắc Kiếm Môn"));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        // Kiểm tra điều kiện cảnh giới (ví dụ: Luyện Hư - Viên Mãn)
        if (!"luyenhu".equals(data.getRealmId()) || !"vienman".equals(data.getTierId())) {
            player.sendMessage(format(prefix + "&cBạn chưa đủ cảnh giới để sáng lập tông môn. Cần đạt Luyện Hư - Viên Mãn."));
            return;
        }
        if (!"none".equals(data.getTongMonId())) {
            player.sendMessage(format(prefix + "&cBạn đã ở trong một tông môn, không thể tạo mới."));
            return;
        }
        
        String id = args[1].toLowerCase();
        if (tmManager.isTenTongMonTonTai(id)) {
            player.sendMessage(format(prefix + "&cID Tông Môn này đã tồn tại, vui lòng chọn ID khác."));
            return;
        }
        if (id.contains(" ")) {
            player.sendMessage(format(prefix + "&cID Tông Môn không được chứa dấu cách."));
            return;
        }

        // Gộp các phần còn lại của tên hiển thị
        String tenHienThi = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        tmManager.taoTongMon(player, id, tenHienThi);
        Bukkit.broadcastMessage(format(prefix + "&fChúc mừng đạo hữu &e" + player.getName() + " &fđã khai sơn lập phái, sáng lập ra " + tmManager.getTenHienThi(id) + "&f!"));
    }

    private void handleInvite(Player player, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " moi <tên_người_chơi>"));
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        String tongMonId = data.getTongMonId();
        if ("none".equals(tongMonId) || !tmManager.laChuTongMon(player, tongMonId)) {
            player.sendMessage(format(prefix + "&cChỉ Trưởng Môn mới có quyền mời thành viên."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(format(prefix + "&cKhông tìm thấy người chơi '" + args[1] + "' hoặc họ đang offline."));
            return;
        }

        invites.put(target.getUniqueId(), tongMonId);
        player.sendMessage(format(prefix + "&aĐã gửi lời mời gia nhập tông môn tới " + target.getName()));
        
        String tenHienThi = tmManager.getTenHienThi(tongMonId);
        target.sendMessage(format(prefix + "&fBạn nhận được lời mời gia nhập " + tenHienThi + "&f từ Trưởng Môn &e" + player.getName()));
        target.sendMessage(format(prefix + "&fGõ &a/tongmon accept " + tongMonId + " &fđể đồng ý."));
    }

    private void handleAccept(Player player, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " accept <ID_Tông_Môn>"));
            return;
        }
        String tongMonId = args[1].toLowerCase();
        if (!invites.containsKey(player.getUniqueId()) || !invites.get(player.getUniqueId()).equals(tongMonId)) {
            player.sendMessage(format(prefix + "&cBạn không có lời mời nào từ tông môn này."));
            return;
        }
        
        invites.remove(player.getUniqueId());
        tmManager.themThanhVien(tongMonId, player);
        
        String tenHienThi = tmManager.getTenHienThi(tongMonId);
        Bukkit.broadcastMessage(format(prefix + "&fChúc mừng &e" + player.getName() + " &fđã chính thức trở thành thành viên của " + tenHienThi + "&f!"));
    }

    private void handleKick(Player player, String[] args, String label) {
        // ...
    }

    private void handleDisband(Player player, String label) {
        // ...
    }

    private void handleAdminSet(CommandSender sender, String[] args, String label) {
        // ...
    }

    private void sendHelpMessage(CommandSender sender, String label) {
        // ...
    }

    private String format(String msg) {
        return tmManager.format(msg); // Sử dụng hàm format từ TongMonManager để hỗ trợ RGB
    }
    
    // ... Tab Completer ...
}