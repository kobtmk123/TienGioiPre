package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.RealmManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DotPhaCommand implements CommandExecutor {
    private final TienGioiPre plugin;
    private final Set<UUID> playersInTribulation = new HashSet<>();
    private final Random random = new Random();

    public DotPhaCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;

        if (playersInTribulation.contains(player.getUniqueId())) {
            player.sendMessage(format("&cBạn đang trong quá trình độ kiếp, không thể thực hiện!"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage(format("&c[Lỗi] Không thể tải dữ liệu của bạn."));
            return true;
        }

        RealmManager realmManager = plugin.getRealmManager();
        RealmManager.TierData currentTier = realmManager.getTierData(data.getRealmId(), data.getTierId());
        if (currentTier == null) {
            player.sendMessage(format("&c[Lỗi] Cảnh giới hiện tại không hợp lệ."));
            return true;
        }
        
        String[] nextProgression = realmManager.getNextProgression(data.getRealmId(), data.getTierId());
        if (nextProgression == null) {
            player.sendMessage(format("&eBạn đã đạt tới cảnh giới cao nhất!"));
            return true;
        }

        if (data.getLinhKhi() >= currentTier.maxLinhKhi()) {
            boolean chanceEnabled = plugin.getConfig().getBoolean("settings.breakthrough-chance.enabled", false);
            int successRate = plugin.getConfig().getInt("settings.breakthrough-chance.success-rate", 100);

            // === LOGIC MỚI: KIỂM TRA VÀ SỬ DỤNG ĐAN DƯỢC ===
            int pillBonus = consumeBreakthroughPill(player, nextProgression[0]); // nextProgression[0] là ID của Tu Vi sắp lên
            if (pillBonus > 0) {
                player.sendMessage(format("&dNhờ có đan dược phụ trợ, tỷ lệ đột phá của bạn đã tăng thêm &e" + pillBonus + "%&d!"));
            }
            int finalSuccessRate = successRate + pillBonus;
            
            // Nếu hệ thống tỷ lệ được bật VÀ random thất bại
            if (chanceEnabled && random.nextInt(100) >= finalSuccessRate) {
                player.sendMessage(format("&c&lĐáng Tiếc! &7Lần đột phá này đã thất bại, linh khí hỗn loạn."));
                data.setLinhKhi(data.getLinhKhi() * 0.75); 
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                handleBreakthrough(player, data, currentTier);
            }
        } else {
            String message = "&cBạn chưa đủ linh khí! Cần &e%req%&c, có &e%cur%&c.";
            message = message.replace("%req%", String.format("%,.0f", currentTier.maxLinhKhi()))
                             .replace("%cur%", String.format("%,.0f", data.getLinhKhi()));
            player.sendMessage(format(message));
        }
        return true;
    }

    /**
     * Tìm và sử dụng (xóa) viên đan dược đột phá phù hợp trong túi đồ của người chơi.
     * @param player Người chơi
     * @param nextRealmId ID của Tu Vi người chơi SẮP đột phá lên (ví dụ: "trucco")
     * @return Tỷ lệ bonus nhận được từ đan dược.
     */
    private int consumeBreakthroughPill(Player player, String nextRealmId) {
        String requiredPillId = nextRealmId + "_dan"; // Ví dụ: "trucco" -> "trucco_dan"
        ItemStack bestPill = null;
        int bestBonus = 0;
        int bestPillSlot = -1;

        // Duyệt qua túi đồ để tìm viên đan dược tốt nhất
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;
            
            ItemMeta meta = item.getItemMeta();
            // Giả sử chúng ta lưu ID của đan dược vào NBT để kiểm tra chính xác
            String pillId = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_ID_KEY, PersistentDataType.STRING);
            
            if (requiredPillId.equals(pillId)) {
                String pillTier = meta.getPersistentDataContainer().get(plugin.getItemManager().ITEM_TIER_KEY, PersistentDataType.STRING);
                int currentBonus = plugin.getConfig().getInt("alchemy.pills." + pillId + "." + pillTier + ".success-chance-bonus", 0);
                
                if (currentBonus > bestBonus) {
                    bestBonus = currentBonus;
                    bestPill = item;
                    bestPillSlot = i;
                }
            }
        }

        // Nếu tìm thấy một viên đan dược, sử dụng nó
        if (bestPill != null && bestPillSlot != -1) {
            bestPill.setAmount(bestPill.getAmount() - 1);
            player.getInventory().setItem(bestPillSlot, bestPill);
            return bestBonus;
        }

        return 0;
    }
    
    private void handleBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        // ... (Logic bắt đầu lôi kiếp giữ nguyên)
    }

    private void completeBreakthrough(Player player, PlayerData data, RealmManager.TierData currentTier) {
        // ... (Logic hoàn tất đột phá và gửi thông báo giữ nguyên)
    }
    
    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}