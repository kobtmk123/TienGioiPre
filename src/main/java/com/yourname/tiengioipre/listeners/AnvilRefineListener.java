package com.yourname.tiengioipre.listeners;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// ... các import khác cho NBT, random, v.v. ...

public class AnvilRefineListener implements Listener {
    private final TienGioiPre plugin;
    public AnvilRefineListener(TienGioiPre plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        Player player = (Player) event.getView().getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        // Chỉ Luyện Khí Sư mới dùng được tính năng này
        if (data == null || !"luyenkhisu".equals(data.getCultivationPath())) {
            return;
        }

        ItemStack itemToUpgrade = inv.getItem(0);
        ItemStack catalyst = inv.getItem(1); // Phôi

        if (itemToUpgrade == null || catalyst == null) return;
        
        // Logic kiểm tra xem Phôi có hợp lệ không
        // Logic kiểm tra xem item có phù hợp với phôi không (armor_weapon vs tool)
        // Logic tính toán tỷ lệ ra cấp bậc
        // Logic tính toán random chỉ số dựa trên cấp bậc và damage-range/health-range
        // Logic tạo ra item kết quả (resultItem)

        // Ví dụ tạo item kết quả
        ItemStack resultItem = itemToUpgrade.clone();
        ItemMeta meta = resultItem.getItemMeta();
        
        // Tên mới: [Tuyệt] Kiếm Kim Cương
        String tierDisplayName = plugin.getConfig().getString("refining.tiers.tuyet.display-name");
        meta.setDisplayName(tierDisplayName + " " + itemToUpgrade.getType().name());
        
        // Thêm lore chỉ số
        List<String> lore = new ArrayList<>();
        lore.add("&aSát thương: &f+5.5");
        lore.add("&cMáu: &f+4.0");
        meta.setLore(lore);
        
        // Thêm enchant và NBT để đánh dấu là đồ đã rèn
        // ...
        
        resultItem.setItemMeta(meta);
        event.setResult(resultItem);
        // Set giá kinh nghiệm để có thể lấy ra
        inv.setRepairCost(5); 
    }
}