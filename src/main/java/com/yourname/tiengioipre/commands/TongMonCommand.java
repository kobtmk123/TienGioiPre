package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.TongMonManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // <-- IMPORT MỚI
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable; // <-- IMPORT MỚI

import java.util.*;
import java.util.stream.Collectors;

// SỬA LỖI: Thêm "implements TabCompleter"
public class TongMonCommand implements CommandExecutor, TabCompleter {
    private final TienGioiPre plugin;
    private final TongMonManager tmManager;
    private final Map<UUID, String> invites = new HashMap<>();
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
        
        if (!(sender instanceof Player)) {
            if (subCmd.equals("set")) handleAdminSet(sender, args, label);
            else sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
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
            default: sendHelpMessage(player, label); break;
        }
        return true;
    }
    
    // --- CÁC HÀM XỬ LÝ LỆNH ---

    private void handleCreate(Player player, String[] args, String label) {
        if (args.length < 3) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " create <ID> <Tên Hiển Thị>"));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (!"luyenhu".equals(data.getRealmId()) || !"vienman".equals(data.getTierId())) {
            player.sendMessage(format(prefix + "&cBạn cần đạt Luyện Hư - Viên Mãn để lập tông."));
            return;
        }
        if (!"none".equals(data.getTongMonId())) {
            player.sendMessage(format(prefix + "&cBạn đã ở trong một tông môn."));
            return;
        }
        String id = args[1].toLowerCase();
        if (tmManager.isTenTongMonTonTai(id)) {
            player.sendMessage(format(prefix + "&cID Tông Môn này đã tồn tại."));
            return;
        }
        String tenHienThi = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        tmManager.taoTongMon(player, id, tenHienThi);
        Bukkit.broadcastMessage(format(prefix + "&fChúc mừng đạo hữu &e" + player.getName() + " &fđã khai sơn lập phái, sáng lập ra " + tmManager.getTenHienThi(id) + "&f!"));
    }

    private void handleInvite(Player player, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " moi <player>"));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        String tongMonId = data.getTongMonId();
        if ("none".equals(tongMonId) || !tmManager.laChuTongMon(player, tongMonId)) {
            player.sendMessage(format(prefix + "&cChỉ Trưởng Môn mới có quyền mời."));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(format(prefix + "&cNgười chơi không online."));
            return;
        }
        invites.put(target.getUniqueId(), tongMonId);
        player.sendMessage(format(prefix + "&aĐã gửi lời mời tới " + target.getName()));
        target.sendMessage(format(prefix + "&fBạn nhận được lời mời gia nhập " + tmManager.getTenHienThi(tongMonId) + " &ftừ &e" + player.getName()));
        target.sendMessage(format(prefix + "&fGõ &a/tongmon accept " + tongMonId + " &fđể đồng ý."));
    }

    private void handleAccept(Player player, String[] args, String label) {
        if (args.length < 2) {
            player.sendMessage(format(prefix + "&cUsage: /" + label + " accept <ID_Tông_Môn>"));
            return;
        }
        String tongMonId = args[1].toLowerCase();
        if (!invites.getOrDefault(player.getUniqueId(), "").equals(tongMonId)) {
            player.sendMessage(format(prefix + "&cBạn không có lời mời từ tông môn này."));
            return;
        }
        invites.remove(player.getUniqueId());
        tmManager.themThanhVien(tongMonId, player);
        Bukkit.broadcastMessage(format(prefix + "&fChúc mừng &e" + player.getName() + " &fđã trở thành thành viên của " + tmManager.getTenHienThi(tongMonId) + "&f!"));
    }

    private void handleKick(Player player, String[] args, String label) {
        // ... (Logic tương tự)
    }

    private void handleDisband(Player player, String label) {
        // ... (Logic tương tự)
    }

    private void handleAdminSet(CommandSender sender, String[] args, String label) {
        // ... (Logic tương tự)
    }

    private void sendHelpMessage(CommandSender sender, String label) {
        // ... (Logic tương tự)
    }

    private String format(String msg) {
        return tmManager.format(msg);
    }
    
    // SỬA LỖI: Thêm phương thức onTabComplete
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "moi", "accept", "kick", "xoa", "set"), args[0]);
        }
        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (Arrays.asList("moi", "kick", "set").contains(subCmd)) {
                return null; // Gợi ý tên người chơi online
            }
            if (subCmd.equals("accept")) {
                // Gợi ý tên tông môn đã mời người chơi (nếu có)
                String invitedTo = invites.get(((Player) sender).getUniqueId());
                if (invitedTo != null) {
                    return Collections.singletonList(invitedTo);
                }
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