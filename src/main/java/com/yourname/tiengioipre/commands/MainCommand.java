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
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final TienGioiPre plugin;
    private final String prefix = "&8[&bTienGioi&8] &r";
    private final boolean debugMode = true; // Bật/tắt chế độ debug tại đây

    public MainCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    private void debug(CommandSender sender, String message) {
        if (debugMode) {
            sender.sendMessage(format("&e[DEBUG] " + message));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tiengioipre.admin")) {
            sender.sendMessage(format(prefix + "&cBạn không có quyền."));
            return true;
        }

        debug(sender, "Lệnh nhận được: " + command.getName() + " với " + args.length + " đối số.");

        if (args.length == 0) {
            sendHelpMessage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        debug(sender, "Lệnh con: " + subCommand);

        switch (subCommand) {
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
                debug(sender, "Đang gọi hàm handleSetLinhKhi...");
                handleSetLinhKhi(sender, args, label);
                break;
            default:
                sendHelpMessage(sender, label);
                break;
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        debug(sender, "Bắt đầu reload...");
        plugin.reloadConfig();
        plugin.getRealmManager().loadRealms();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getRealmManager().applyAllStats(player);
        }
        sender.sendMessage(format(prefix + "&aĐã tải lại cấu hình và áp dụng cho người chơi online."));
        debug(sender, "Reload hoàn tất.");
    }

    private void handleGive(CommandSender sender, String[] args, String label) {
        debug(sender, "Đang xử lý lệnh 'give'.");
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

        debug(sender, "Give: player=" + target.getName() + ", type=" + itemType + ", id=" + itemId + ", amount=" + amount);

        ItemStack itemToGive;
        ItemManager itemManager = plugin.getItemManager();

        if (itemType.equals("cuonlinhkhi")) {
            itemToGive = itemManager.createCultivationItem(itemType, itemId);
        } else if (itemType.equals("phoi")) {
            itemToGive = itemManager.createCultivationItem(itemId, "default"); 
        } else {
            sender.sendMessage(format(prefix + "&cLoại vật phẩm không hợp lệ: 'cuonlinhkhi' hoặc 'phoi'."));
            return;
        }

        if (itemToGive == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy vật phẩm với ID '" + itemId + "' hoặc tier không hợp lệ."));
            return;
        }

        itemToGive.setAmount(amount);
        target.getInventory().addItem(itemToGive);
        sender.sendMessage(format(prefix + "&aĐã trao " + amount + "x " + itemToGive.getItemMeta().getDisplayName() + " &acho " + target.getName()));
        debug(sender, "Lệnh 'give' hoàn tất.");
    }
    
    private void handleSetRealm(CommandSender sender, String[] args, String label) {
        debug(sender, "Đang xử lý lệnh 'setrealm'.");
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
        debug(sender, "SetRealm: player=" + target.getName() + ", realmId=" + realmId + ", tierId=" + tierId);

        RealmManager.TierData tierData = plugin.getRealmManager().getTierData(realmId, tierId);
        if (tierData == null) {
            sender.sendMessage(format(prefix + "&cTu Vi ID hoặc Bậc ID không tồn tại."));
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
        plugin.getRealmManager().applyAllStats(target);
        String realmName = plugin.getRealmManager().getRealmDisplayName(realmId);
        String tierName = plugin.getRealmManager().getTierDisplayName(realmId, tierId);
        sender.sendMessage(format(prefix + "&aĐã đặt cảnh giới của " + target.getName() + " thành " + realmName + " " + tierName));
        target.sendMessage(format(prefix + "&aCảnh giới của bạn đã được admin thay đổi."));
        debug(sender, "Lệnh 'setrealm' hoàn tất.");
    }
    
    private void handleSetLinhKhi(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(format(prefix + "&cUsage: /" + label + " setlinhkhi <player> <amount>"));
            return;
        }
        debug(sender, "Bước 1: Cú pháp lệnh hợp lệ.");

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy người chơi " + args[1]));
            return;
        }
        debug(sender, "Bước 2: Đã tìm thấy người chơi '" + target.getName() + "'.");
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(format(prefix + "&cSố lượng linh khí không hợp lệ."));
            return;
        }
        debug(sender, "Bước 3: Số lượng linh khí hợp lệ: " + amount);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(format(prefix + "&cKhông tìm thấy dữ liệu của người chơi '" + target.getName() + "'."));
            return;
        }
        debug(sender, "Bước 4: Đã tìm thấy dữ liệu của người chơi.");

        data.setLinhKhi(amount);
        debug(sender, "Bước 5: Đã set linh khí thành công trong đối tượng PlayerData. Giá trị mới: " + data.getLinhKhi());
        
        sender.sendMessage(format(prefix + "&aĐã đặt linh khí của " + target.getName() + " thành &e" + String.format("%,.0f", amount)));
        target.sendMessage(format(prefix + "&aLinh khí của bạn đã được admin thay đổi."));
        debug(sender, "Bước 6: Gửi tin nhắn thành công. Lệnh 'setlinhkhi' hoàn tất.");
    }
    
    private void sendHelpMessage(CommandSender sender, String label) {
        sender.sendMessage(format("&6--- Lệnh Admin TienGioiPre ---"));
        sender.sendMessage(format("&e/" + label + " reload &7- Tải lại config."));
        sender.sendMessage(format("&e/" + label + " give <player> <type> <id> [amount] &7- Trao vật phẩm."));
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
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 3) {
                return filter(Arrays.asList("cuonlinhkhi", "phoi"), args[2]);
            }
            if (args.length == 4) {
                String itemType = args[2].toLowerCase();
                if (itemType.equals("cuonlinhkhi")) {
                    ConfigurationSection section = plugin.getConfig().getConfigurationSection("items.cuonlinhkhi");
                    return section != null ? filter(new ArrayList<>(section.getKeys(false)), args[3]) : Collections.emptyList();
                }
                if (itemType.equals("phoi")) {
                    List<String> phoiKeys = new ArrayList<>();
                    ConfigurationSection section = plugin.getConfig().getConfigurationSection("items");
                    if (section != null) {
                        section.getKeys(false).forEach(key -> { if (key.startsWith("phoi_")) phoiKeys.add(key); });
                    }
                    return filter(phoiKeys, args[3]);
                }
            }
        }
        if (args[0].equalsIgnoreCase("setrealm")) {
            if (args.length == 3) {
                ConfigurationSection section = plugin.getConfig().getConfigurationSection("realms");
                return section != null ? filter(new ArrayList<>(section.getKeys(false)), args[2]) : Collections.emptyList();
            }
            if (args.length == 4) {
                String realmId = args[2];
                ConfigurationSection section = plugin.getConfig().getConfigurationSection("realms." + realmId + ".tiers");
                return section != null ? filter(new ArrayList<>(section.getKeys(false)), args[3]) : Collections.emptyList();
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