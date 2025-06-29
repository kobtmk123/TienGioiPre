package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
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
            plugin.getPlayerDataManager().loadPlayerData(player);
        }
        sender.sendMessage(format(prefix + "&aĐã tải lại cấu hình và áp dụng cho người chơi online."));
    }

    private void handleGive(CommandSender sender, String[] args, String label) {
        if (args.length < 4) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " give <player> <item_id> <tier> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        String itemId = args[2];
        String itemTier = args[3];
        int amount = 1;
        if(args.length > 4) {
            try { amount = Integer.parseInt(args[4]); }
            catch (NumberFormatException e) {
                sender.sendMessage(format(prefix + "&cSố lượng không hợp lệ."));
                return;
            }
        }
        ItemStack item = plugin.getItemManager().createCultivationItem(itemId, itemTier);
        if(item == null) {
            sender.sendMessage(format(prefix + "&cID hoặc cấp độ vật phẩm không tồn tại."));
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(format(prefix + "&aĐã trao vật phẩm cho " + target.getName()));
        target.sendMessage(format(prefix + "&aBạn đã nhận được vật phẩm."));
    }

    private void handleSetRealm(CommandSender sender, String[] args, String label) {
        if (args.length < 4) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " setrealm <player> <tuvi_id> <bac_id>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        String realmId = args[2];
        String tierId = args[3];
        RealmManager.TierData tierData = plugin.getRealmManager().getTierData(realmId, tierId);
        if(tierData == null) {
            sender.sendMessage(format(prefix + "&cTu Vi ID hoặc Bậc ID không tồn tại trong config.yml."));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
             sender.sendMessage(format(prefix + "&cKhông tìm thấy dữ liệu của người chơi."));
             return;
        }
        data.setRealmId(realmId);
        data.setTierId(tierId);
        data.setLinhKhi(0);
        
        // ĐÃ SỬA LỖI: Gọi đúng hàm applyAllStats
        plugin.getRealmManager().applyAllStats(target);

        String realmName = plugin.getRealmManager().getRealmDisplayName(realmId);
        String tierName = plugin.getRealmManager().getTierDisplayName(realmId, tierId);
        sender.sendMessage(format(prefix + "&aĐã đặt cảnh giới của " + target.getName() + " thành " + realmName + " " + tierName));
        target.sendMessage(format(prefix + "&aCảnh giới của bạn đã được admin thay đổi."));
    }

    private void handleSetLinhKhi(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " setlinhkhi <player> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (NumberFormatException e) {
            sender.sendMessage(format(prefix + "&cSố lượng linh khí không hợp lệ."));
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
             sender.sendMessage(format(prefix + "&cKhông tìm thấy dữ liệu của người chơi."));
             return;
        }
        data.setLinhKhi(amount);
        sender.sendMessage(format(prefix + "&aĐã đặt linh khí của " + target.getName() + " thành &e" + String.format("%,.0f", amount)));
        target.sendMessage(format(prefix + "&aLinh khí của bạn đã được admin thay đổi."));
    }

    private void sendHelpMessage(CommandSender sender, String label) {
        sender.sendMessage(format("&6--- Lệnh Admin TienGioiPre ---"));
        sender.sendMessage(format("&e/" + label + " reload &7- Tải lại config."));
        sender.sendMessage(format("&e/" + label + " give <player> <id> <tier> [amount] &7- Trao vật phẩm."));
        sender.sendMessage(format("&e/" + label + " setrealm <player> <tuvi_id> <bac_id> &7- Đặt cảnh giới."));
        sender.sendMessage(format("&e/" + label + " setlinhkhi <player> <amount> &7- Đặt linh khí."));
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
        if (args.length == 2) {
            if (Arrays.asList("give", "setrealm", "setlinhkhi").contains(args[0].toLowerCase())) {
                return null;
            }
        }
        if (args[0].equalsIgnoreCase("setrealm")) {
            if (args.length == 3) {
                return filter(plugin.getRealmManager().getRealmsDataMap().keySet(), args[2]);
            }
            if (args.length == 4) {
                String realmId = args[2];
                RealmManager.RealmData realmData = plugin.getRealmManager().getRealmsDataMap().get(realmId);
                if (realmData != null) {
                    return filter(realmData.tiers().keySet(), args[3]);
                }
            }
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 3) {
                if(plugin.getConfig().getConfigurationSection("items") != null)
                    return filter(plugin.getConfig().getConfigurationSection("items").getKeys(false), args[2]);
            }
            if (args.length == 4) {
                String itemId = args[2];
                if(plugin.getConfig().contains("items." + itemId)) {
                    return filter(plugin.getConfig().getConfigurationSection("items." + itemId).getKeys(false), args[3]);
                }
            }
        }
        return Collections.emptyList();
    }
    
    private List<String> filter(Iterable<String> list, String start) {
        List<String> result = new ArrayList<>();
        if (list == null) return result;
        list.forEach(s -> {
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                result.add(s);
            }
        });
        return result;
    }
}