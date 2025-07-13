package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.gui.ShopGUI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class ShopListener implements Listener {

    private final TienGioiPre plugin;
    private final String shopTitle;

    public ShopListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.shopTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("shop.title", "&8Shop"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Chỉ xử lý nếu GUI đang mở là của shop
        if (!event.getView().getTitle().equals(shopTitle)) {
            return;
        }

        // HỦY BỎ SỰ KIỆN CLICK -> NGƯỜI CHƠI KHÔNG THỂ LẤY ITEM
        event.setCancelled(true);

        // Đảm bảo người click là Player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // Bỏ qua nếu click vào ô trống hoặc item không có dữ liệu
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Chỉ xử lý nếu đây là vật phẩm của shop (có tag)
        if (!container.has(ShopGUI.SHOP_ITEM_TAG, PersistentDataType.BYTE)) {
            return;
        }

        // Lấy thông tin từ item đã được gắn trong ShopGUI
        double pricePerItem = container.getOrDefault(ShopGUI.SHOP_PRICE_TAG, PersistentDataType.DOUBLE, 0.0);
        if (pricePerItem <= 0) return;

        // Xác định số lượng mua dựa trên loại click
        int amountToBuy = (event.getClick() == ClickType.RIGHT) ? 64 : 1;
        double totalPrice = pricePerItem * amountToBuy;

        // Kiểm tra kết nối với Vault
        Economy economy = TienGioiPre.getEconomy();
        if (economy == null) {
            player.sendMessage(format("&c[Lỗi] Hệ thống kinh tế không hoạt động. Vui lòng báo Admin."));
            return;
        }

        // Kiểm tra số dư của người chơi
        if (economy.getBalance(player) < totalPrice) {
            player.sendMessage(format("&cBạn không đủ tiền! Cần &e" + String.format("%,.0f", totalPrice) + " Xu&c."));
            player.closeInventory();
            return;
        }

        // Thực hiện giao dịch
        EconomyResponse response = economy.withdrawPlayer(player, totalPrice);
        if (response.transactionSuccess()) {
            // Lấy ID và TIER gốc từ item để tạo lại một item "sạch" cho người chơi
            ItemManager itemManager = plugin.getItemManager();
            String itemId = container.get(itemManager.ITEM_ID_KEY, PersistentDataType.STRING);
            String itemTier = container.get(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING);
            
            ItemStack itemToGive = itemManager.createCultivationItem(itemId, itemTier);
            if (itemToGive == null) {
                player.sendMessage(format("&c[Lỗi] Không thể tạo vật phẩm. Giao dịch đã được hoàn lại."));
                economy.depositPlayer(player, totalPrice); // Hoàn tiền nếu có lỗi
                return;
            }
            itemToGive.setAmount(amountToBuy);

            // Thêm vật phẩm vào kho đồ của người chơi
            HashMap<Integer, ItemStack> failedItems = player.getInventory().addItem(itemToGive);
            if (!failedItems.isEmpty()) {
                player.sendMessage(format("&cTúi đồ của bạn đã đầy, vật phẩm bị rơi ra đất!"));
                failedItems.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }

            player.sendMessage(format("&aBạn đã mua thành công &f" + amountToBuy + "x " + meta.getDisplayName() + " &avới giá &e" + String.format("%,.0f", totalPrice) + " Xu&a."));
        } else {
            // Thông báo nếu giao dịch thất bại vì lý do khác
            player.sendMessage(format("&cGiao dịch thất bại: " + response.errorMessage));
        }
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}