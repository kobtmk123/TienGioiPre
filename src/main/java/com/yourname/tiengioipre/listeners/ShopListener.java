package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
        if (!event.getView().getTitle().equals(shopTitle)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();
        NamespacedKey priceKey = new NamespacedKey(plugin, "shop_price");

        if (!container.has(priceKey, PersistentDataType.DOUBLE)) {
            return;
        }
        
        double pricePerItem = container.get(priceKey, PersistentDataType.DOUBLE);
        int amountToBuy = event.getClick().isLeftClick() ? 1 : 64;
        double totalPrice = pricePerItem * amountToBuy;

        if (TienGioiPre.getEconomy() == null) {
            player.sendMessage("§cChức năng kinh tế không hoạt động.");
            return;
        }

        EconomyResponse r = TienGioiPre.getEconomy().withdrawPlayer(player, totalPrice);
        if (r.transactionSuccess()) {
            player.sendMessage("§aBạn đã mua vật phẩm thành công với giá " + totalPrice + " Xu.");
            ItemStack itemToGive = clickedItem.clone();
            // Xóa các tag của shop khỏi vật phẩm trước khi đưa cho người chơi
            itemToGive.editMeta(meta -> meta.getPersistentDataContainer().remove(priceKey));
            itemToGive.setAmount(amountToBuy);
            
            HashMap<Integer, ItemStack> failedItems = player.getInventory().addItem(itemToGive);
            if (!failedItems.isEmpty()) {
                player.sendMessage("§cTúi đồ của bạn đã đầy, vật phẩm bị rơi ra đất!");
                failedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
            }
            
        } else {
            player.sendMessage("§cBạn không đủ tiền! Cần " + totalPrice + " Xu.");
        }
    }
}