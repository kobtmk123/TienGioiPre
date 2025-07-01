package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors; // <-- IMPORT ĐÃ ĐƯỢC THÊM

public class MainCommand implements CommandExecutor, TabCompleter {

    private final TienGioiPre plugin;
    private final String prefix = "&8[&bTienGioi&8] &r";

    public MainCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tiengioipre.admin")) {
            sender.sendMessage(format(prefix + "&cBạn không có quyền."));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "give":
                handleGive(sender, args, label);
                break;
            case "setrealm":
                handleSetRealm(sender, args, label);
                break;
            case "setlinhkhi":
                handleSetLinhKhi(sender, args, label);
                break;
            default:
                sendHelpMessage(sender, label);
                break;
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getRealmManager().loadRealms();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getRealmManager().applyAllStats(player);
        }
        sender.sendMessage(format(prefix + "&aĐã tải lại cấu hình và áp dụng cho người chơi online."));
    }

    private void handleGive(CommandSender sender, String[] args, String label) {
        if (args.length < 4) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " give <player> <item_type> <item_id> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }

        String itemType = args[2].toLowerCase();
        String itemId = args[3].toLowerCase();
        int amount = 1;

        if (args.length > 4) {
             try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException e) { sender.sendMessage(format("&cSố lượng không hợp lệ.")); return; }
        }

        ItemStack itemToGive;
        ItemManager itemManager = plugin.getItemManager();

        if (itemType.equals("cuonlinhkhi")) {
            itemToGive = itemManager.createCultivationItem(itemType, itemId);
        } else if (itemType.equals("phoi")) {
            // Logic tạo Phôi
            // Giả sử các phôi được định nghĩa như các item bình thường trong mục 'items'
            // và không có tier (hoặc có tier là 'default')
            ConfigurationSection phoiSection = plugin.getConfig().getConfigurationSection("items." + itemId);
            if (phoiSection == null) {
                 sender.sendMessage(format(prefix + "&cKhông tìm thấy vật phẩm với ID '" + itemId + "'."));
                 return;
            }
            // Gọi hàm tạo item chung
            itemToGive = itemManager.createCultivationItem(itemId, "default");

        } else {
            sender.sendMessage(format(prefix + "&cLoại vật phẩm không hợp lệ: 'cuonlinhkhi' hoặc 'phoi'."));
            return;
        }

        if (itemToGive == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy vật phẩm với ID '" + itemId + "'."));
            return;
        }

        itemToGive.setAmount(amount);
        target.getInventory().addItem(itemToGive);
        sender.sendMessage(format(prefix + "&aĐã trao " + amount + "x " + itemToGive.getItemMeta().getDisplayName() + " &acho " + target.getName()));
    }
    
    private void handleSetRealm(CommandSender sender, String[] args, String label) {
        // ... (Giữ nguyên)
    }
    
    private void handleSetLinhKhi(CommandSender sender, String[] args, String label) {
        // ... (Giữ nguyên)
    }
    
    private void sendHelpMessage(CommandSender sender, String label) {
        // ... (Giữ nguyên)
    }
    
    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("reload", "give", "setrealm", "setlinhkhi"), args[0]);
        }
        // ... (phần còn lại giữ nguyên)
        return Collections.emptyList();
    }
    
    private List<String> filter(List<String> list, String start) {
        return list.stream()
                   .filter(s -> s.toLowerCase().startsWith(start.toLowerCase()))
                   .collect(Collectors.toList());
    }
}