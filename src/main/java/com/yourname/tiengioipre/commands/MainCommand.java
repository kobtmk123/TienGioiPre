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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final TienGioiPre plugin;
    private final String prefix = "&8[&bTienGioi&8] &r";

    public MainCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tiengioipre.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cBạn không có quyền."));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "setrealm":
                handleSetRealm(sender, args);
                break;
            case "setlinhkhi":
                handleSetLinhKhi(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getRealmManager().loadRealms();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aĐã tải lại cấu hình thành công."));
    }

    private void handleGive(CommandSender sender, String[] args) {
        // /tth give <player> <item_id> <tier> [amount]
        if (args.length < 4) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cUsage: /" + "tth" + " give <player> <item_id> <tier> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        
        String itemId = args[2];
        String itemTier = args[3];
        int amount = 1;
        if(args.length > 4) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cSố lượng không hợp lệ."));
                return;
            }
        }
        
        ItemStack item = plugin.getItemManager().createCultivationItem(itemId, itemTier);
        if(item == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cID hoặc cấp độ vật phẩm không tồn tại."));
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aĐã trao vật phẩm cho " + target.getName()));
    }

    private void handleSetRealm(CommandSender sender, String[] args) {
        // /tth setrealm <player> <realm_id>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cUsage: /" + "tth" + " setrealm <player> <realm_id>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        String realmId = args[2];
        if(plugin.getRealmManager().getRealm(realmId) == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cID cảnh giới không tồn tại."));
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        data.setRealmId(realmId);
        data.setLinhKhi(0);
        plugin.getRealmManager().applyRealmStats(target, realmId);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aĐã đặt cảnh giới của " + target.getName() + " thành " + realmId));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aCảnh giới của bạn đã được admin thay đổi."));
    }

    private void handleSetLinhKhi(CommandSender sender, String[] args) {
        // /tth setlinhkhi <player> <amount>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cUsage: /" + "tth" + " setlinhkhi <player> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cSố lượng linh khí không hợp lệ."));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        data.setLinhKhi(amount);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aĐã đặt linh khí của " + target.getName() + " thành " + amount));
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Lệnh Admin TienGioiPre ---");
        sender.sendMessage(ChatColor.YELLOW + "/tth reload " + ChatColor.GRAY + "- Tải lại config.");
        sender.sendMessage(ChatColor.YELLOW + "/tth give <player> <id> <tier> [amount] " + ChatColor.GRAY + "- Trao vật phẩm tu luyện.");
        sender.sendMessage(ChatColor.YELLOW + "/tth setrealm <player> <realm_id> " + ChatColor.GRAY + "- Đặt cảnh giới cho người chơi.");
        sender.sendMessage(ChatColor.YELLOW + "/tth setlinhkhi <player> <amount> " + ChatColor.GRAY + "- Đặt linh khí cho người chơi.");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("reload", "give", "setrealm", "setlinhkhi"), args[0]);
        }
        if (args.length > 1) {
            switch (args[0].toLowerCase()) {
                case "give":
                case "setrealm":
                case "setlinhkhi":
                    if (args.length == 2) {
                        return null; // Mặc định gợi ý tên người chơi
                    }
                    if (args.length == 3 && args[0].equalsIgnoreCase("setrealm")) {
                        // Cần thêm phương thức getAllRealmIds() vào RealmManager
                        // return filter(plugin.getRealmManager().getAllRealmIds(), args[2]);
                        return null;
                    }
                    if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                        // Cần thêm phương thức để lấy tất cả ID vật phẩm
                        return filter(plugin.getConfig().getConfigurationSection("items").getKeys(false), args[2]);
                    }
                    if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
                        // Cần thêm phương thức để lấy tất cả tier của vật phẩm
                        String itemId = args[2];
                        if(plugin.getConfig().contains("items." + itemId)) {
                            return filter(plugin.getConfig().getConfigurationSection("items." + itemId).getKeys(false), args[3]);
                        }
                    }
                    break;
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