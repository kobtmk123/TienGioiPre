package com.myname.tiengioipre.listeners;

import com.myname.tiengioipre.TienGioiPre;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.block.Block;
import java.util.Random;

public class OreSpawnListener implements Listener {
    private final TienGioiPre plugin;
    private final Random random = new Random();

    public OreSpawnListener(TienGioiPre plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChunkGenerate(ChunkPopulateEvent event) {
        // Lấy spawn-chance từ config
        double armorOreChance = plugin.getConfig().getDouble("refining.ore-spawning.armor_weapon_ore.spawn-chance");

        // Logic duyệt qua các block trong chunk mới tạo
        // Nếu random.nextDouble() < armorOreChance, thay thế block đó bằng COPPER_ORE
        // Đồng thời, có thể lưu lại tọa độ block này để tạo hiệu ứng hạt sau
    }
    
    // Cần một task riêng để chạy và tạo hiệu ứng hạt cho các quặng đã spawn
}