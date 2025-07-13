package com.yourname.tiengioipre.tasks;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.utils.DebugLogger;
import org.bukkit.ChatColor; // Import này đã có nhưng vẫn giữ
import org.bukkit.Material;
import org.bukkit.NamespacedKey; // Import này đã có nhưng vẫn giữ
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection; // <-- IMPORT MỚI
import org.bukkit.enchantments.Enchantment; // <-- IMPORT MỚI
import org.bukkit.inventory.ItemFlag; // Import này đã có nhưng vẫn giữ
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList; // <-- IMPORT MỚI
import java.util.List;      // <-- IMPORT MỚI
import java.util.Objects;
import java.util.regex.Matcher; // Import này đã có nhưng vẫn giữ
import java.util.regex.Pattern; // Import này đã có nhưng vẫn giữ

public class AlchemyFurnaceTask extends BukkitRunnable {

    private final TienGioiPre plugin;
    private final NamespacedKey semiPillKey;
    private final NamespacedKey pillBonusKey;

    public AlchemyFurnaceTask(TienGioiPre plugin) {
        this.plugin = plugin;
        this.semiPillKey = new NamespacedKey(plugin, "semi_finished_pill");
        this.pillBonusKey = new NamespacedKey(plugin, "tiengioi_pill_bonus");
    }

    @Override
    public void run() {
        // Duyệt qua tất cả các chunk đang tải
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Furnace) {
                        Furnace furnace = (Furnace) blockState;
                        ItemStack smeltingItem = furnace.getInventory().getSmelting();

                        if (smeltingItem == null || !smeltingItem.hasItemMeta()) continue;

                        PersistentDataContainer container = smeltingItem.getItemMeta().getPersistentDataContainer();

                        if (container.has(semiPillKey, PersistentDataType.STRING)) {
                            DebugLogger.log("AlchemyFurnaceTask", "Detected semi-pill in furnace at " + furnace.getLocation().toVector());

                            int cookTimeInSeconds = plugin.getConfig().getInt("alchemy.settings.furnace-smelt-time-seconds", 5);
                            short targetCookTime = (short)(cookTimeInSeconds * 20);

                            if (furnace.getCookTime() < targetCookTime) {
                                DebugLogger.log("AlchemyFurnaceTask", "Forcing furnace cook: " + furnace.getCookTime() + "/" + targetCookTime);
                                furnace.setCookTime((short) (furnace.getCookTime() + 1));
                                furnace.setBurnTime((short) (furnace.getBurnTime() + 1));
                                furnace.update(true);
                            } else {
                                DebugLogger.log("AlchemyFurnaceTask", "Semi-pill cooked! Creating final pill.");
                                String pillData = container.get(semiPillKey, PersistentDataType.STRING);
                                String[] parts = pillData.split(":");
                                String pillId = parts[0];
                                String tierId = parts[1];

                                ItemStack finalPill = createFinalPill(pillId, tierId);

                                if (finalPill != null) {
                                    furnace.getInventory().setSmelting(null);
                                    furnace.getInventory().setResult(finalPill);
                                    furnace.setCookTime((short) 0);
                                    furnace.setBurnTime((short) 0);
                                    furnace.update(true);
                                    DebugLogger.log("AlchemyFurnaceTask", "Final pill created: " + finalPill.getItemMeta().getDisplayName());
                                } else {
                                    DebugLogger.warn("AlchemyFurnaceTask", "Failed to create final pill for " + pillId + ":" + tierId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private ItemStack createFinalPill(String pillId, String tierId) {
        ConfigurationSection pillConfig = plugin.getConfig().getConfigurationSection("alchemy.pills." + pillId + "." + tierId);
        if (pillConfig == null) {
            DebugLogger.warn("AlchemyFurnaceTask", "createFinalPill: Không tìm thấy cấu hình cho đan dược: " + pillId + " - " + tierId);
            return null;
        }

        ItemStack pill = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = pill.getItemMeta();
        if (meta == null) return null;

        String displayName = format(pillConfig.getString("display-name", "Đan Dược"));
        meta.setDisplayName(displayName);

        int bonus = pillConfig.getInt("success-chance-bonus", 0);
        List<String> lore = new ArrayList<>();
        for (String line : pillConfig.getStringList("lore")) {
            lore.add(format(line.replace("%bonus%", String.valueOf(bonus))));
        }
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(pillBonusKey, PersistentDataType.INTEGER, bonus);
        
        pill.setItemMeta(meta);
        DebugLogger.log("AlchemyFurnaceTask", "createFinalPill: Final pill created with NBT bonus: " + bonus);
        return pill;
    }
    
    private String format(String msg) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start(), matcher.end());
            msg = msg.replace(color, ChatColor.of(color.substring(1)) + "");
            matcher = pattern.matcher(msg);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}