package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
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

    public PlayerInteractListener(TienGioiPre plugin) {
        this.plugin = plugin;
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
        ItemManager itemManager = new ItemManager(plugin); // Nên lấy instance từ main class

        if (container.has(itemManager.ITEM_ID_KEY, PersistentDataType.STRING)) {
            String itemId = container.get(itemManager.ITEM_ID_KEY, PersistentDataType.STRING);

            if ("cuonlinhkhi".equals(itemId)) {
                event.setCancelled(true);
                String tier = container.get(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING);
                double linhKhiAmount = plugin.getConfig().getDouble("items.cuonlinhkhi." + tier + ".linh-khi-amount", 0);

                PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
                if (data != null) {
                    data.addLinhKhi(linhKhiAmount);
                    event.getPlayer().sendMessage("§aBạn đã hấp thụ §f" + linhKhiAmount + "§a linh khí.");
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
}