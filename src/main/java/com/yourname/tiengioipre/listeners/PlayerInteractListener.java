package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerInteractListener implements Listener {

    private final TienGioiPre plugin;
    private final ItemManager itemManager;

    public PlayerInteractListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(itemManager.ITEM_ID_KEY, PersistentDataType.STRING)) {
            String itemId = container.get(itemManager.ITEM_ID_KEY, PersistentDataType.STRING);

            if ("cuonlinhkhi".equals(itemId)) {
                event.setCancelled(true);
                String tier = container.get(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING);
                double linhKhiAmount = plugin.getConfig().getDouble("items.cuonlinhkhi." + tier + ".linh-khi-amount", 0);

                Player player = event.getPlayer();
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                if (data != null) {
                    data.addLinhKhi(linhKhiAmount);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bTienGioi&8] &aBạn đã hấp thụ &f" + String.format("%,.0f", linhKhiAmount) + "&a linh khí."));
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
}