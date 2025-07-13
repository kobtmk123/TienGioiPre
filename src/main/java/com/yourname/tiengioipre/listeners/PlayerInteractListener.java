package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.core.ItemManager;
import com.yourname.tiengioipre.data.PlayerData;
import com.yourname.tiengioipre.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * Lắng nghe các sự kiện tương tác của người chơi.
 * Bao gồm sử dụng cuộn linh khí và đặt đan dược nửa mùa vào lò.
 */
public class PlayerInteractListener implements Listener {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey; // Key để nhận diện đan dược nửa mùa

    public PlayerInteractListener(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Chỉ xử lý khi người chơi chuột phải bằng tay chính
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // 1. Xử lý khi người chơi sử dụng Cuộn Linh Khí
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta()) return;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            ItemManager itemManager = plugin.getItemManager();

            if (container.has(itemManager.ITEM_ID_KEY, PersistentDataType.STRING)) {
                String itemId = container.get(itemManager.ITEM_ID_KEY, PersistentDataType.STRING);

                if ("cuonlinhkhi".equals(itemId)) {
                    event.setCancelled(true);
                    String tier = container.get(itemManager.ITEM_TIER_KEY, PersistentDataType.STRING);
                    double linhKhiAmount = plugin.getConfig().getDouble("items.cuonlinhkhi." + tier + ".linh-khi-amount", 0);

                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
                    if (data != null) {
                        data.addLinhKhi(linhKhiAmount);
                        event.getPlayer().sendMessage(plugin.getTongMonManager().format("&aBạn đã hấp thụ &f" + linhKhiAmount + "&a linh khí."));
                        item.setAmount(item.getAmount() - 1);
                    }
                    return; // Đã xử lý sự kiện, không cần kiểm tra tiếp
                }
            }
        }

        // 2. Xử lý khi người chơi đặt Đan Dược Nửa Mùa vào lò nung
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null &&
            (event.getClickedBlock().getType() == Material.FURNACE || event.getClickedBlock().getType() == Material.BLAST_FURNACE || event.getClickedBlock().getType() == Material.SMOKER)) {
            
            ItemStack itemInHand = event.getItem();
            if (itemInHand == null || !itemInHand.hasItemMeta()) {
                DebugLogger.log("PlayerInteract", "Item in hand is null or has no meta when right-clicking furnace.");
                return;
            }

            // Kiểm tra xem item trên tay có phải là đan dược nửa mùa không
            if (itemInHand.getItemMeta().getPersistentDataContainer().has(semiPillKey, PersistentDataType.STRING)) {
                DebugLogger.log("PlayerInteract", "Detected semi-finished pill in hand.");
                // Nếu block là một lò nung
                if (event.getClickedBlock().getState() instanceof Furnace) {
                    Furnace furnace = (Furnace) event.getClickedBlock().getState();
                    // Nếu ô nung đang trống
                    if (furnace.getInventory().getSmelting() == null || furnace.getInventory().getSmelting().getType() == Material.AIR) {
                        DebugLogger.log("PlayerInteract", "Furnace is empty. Adding to tracking.");
                        // Thêm lò nung vào danh sách theo dõi của AlchemyFurnaceTask
                        plugin.getAlchemyFurnaceTask().addFurnaceToTrack(furnace.getLocation(), itemInHand);
                        
                        // Ngăn sự kiện mặc định và trừ item trên tay người chơi
                        event.setCancelled(true);
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                        DebugLogger.log("PlayerInteract", "Semi-pill successfully added to furnace tracking. Remaining in hand: " + itemInHand.getAmount());
                    } else {
                        event.getPlayer().sendMessage(plugin.getTongMonManager().format("&cLò nung đang bận, vui lòng chờ lò trống."));
                        DebugLogger.log("PlayerInteract", "Furnace is busy. Not adding semi-pill to tracking.");
                    }
                } else {
                    DebugLogger.log("PlayerInteract", "Clicked block is not a valid furnace type.");
                }
            }
        }
    }
}