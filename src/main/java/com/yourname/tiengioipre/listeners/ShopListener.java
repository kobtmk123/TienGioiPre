package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
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
    private final NamespacedKey shopItemKey;
    private final NamespacedKey shopPriceKey;

    public ShopListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.shopTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("shop.title", "&8Shop"));
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.shopPriceKey = new NamespacedKey(plugin, "shop_price");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(shopTitle)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Chỉ xử lý nếu đây là vật phẩm của shop
        if (!container.has(shopItemKey, PersistentDataType.BYTE)) {
            return;
        }

        double pricePerItem = container.getOrDefault(shopPriceKey, PersistentDataType.DOUBLE, 0.0);
        if (pricePerItem <= 0) return;

        int amountToBuy = (event.getClick() == ClickType.RIGHT) ? 64 : 1;
        double totalPrice = pricePerItem * amountToBuy;

        Economy economy = TienGioiPre.getEconomy();
        if (economy == null) {
            player.sendMessage(format("&cLỗi: Không tìm thấy plugin quản lý kinh tế (Vault)."));
            return;
        }

        if (economy.getBalance(player) < totalPrice) {
            player.sendMessage(format("&cBạn không đủ tiền! Cần &e" + String.format("%,.0f", totalPrice) + " Xu&c."));
            player.closeInventory();
            return;
        }

        EconomyResponse response = economy.withdrawPlayer(player, totalPrice);
        if (response.transactionSuccess()) {
            ItemManager itemManager = plugin.getItemManager();
            String itemId = container.get(itemManager.ITEM_ID_KEY, PersistentDataType.STRING);
            String itemTier = container.get(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING);
            
            ItemStack itemToGive = itemManager.createCultivationItem(itemId, itemTier);
            if (itemToGive == null) {
                player.sendMessage(format("&cLỗi: Không thể tạo vật phẩm để trao."));
                economy.depositPlayer(player, totalPrice); // Hoàn tiền
                return;
            }
            itemToGive.setAmount(amountToBuy);

            HashMap<Integer, ItemStack> failedItems = player.getInventory().addItem(itemToGive);
            if (!failedItems.isEmpty()) {
                player.sendMessage(format("&cTúi đồ của bạn đã đầy, vật phẩm bị rơi ra đất!"));
                failedItems.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }

            player.sendMessage(format("&aBạn đã mua thành công &f" + amountToBuy + "x " + meta.getDisplayName() + " &avới giá &e" + String.format("%,.0f", totalPrice) + " Xu&a."));
        } else {
            player.sendMessage(format("&cGiao dịch thất bại: " + response.errorMessage));
        }
    }

    private String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}