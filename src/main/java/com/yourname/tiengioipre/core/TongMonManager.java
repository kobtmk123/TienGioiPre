package com.yourname.tiengioipre.core;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Quản lý tất cả các hoạt động liên quan đến Tông Môn.
 * Bao gồm việc đọc/ghi file tongmon.yml và các hàm logic.
 */
public class TongMonManager {
    private final TienGioiPre plugin;
    private File tongMonFile;
    private FileConfiguration tongMonConfig;

    public TongMonManager(TienGioiPre plugin) {
        this.plugin = plugin;
        loadTongMonData();
    }

    /**
     * Tải dữ liệu từ file tongmon.yml. Nếu file không tồn tại, tự động tạo mới.
     */
    public void loadTongMonData() {
        tongMonFile = new File(plugin.getDataFolder(), "tongmon.yml");
        if (!tongMonFile.exists()) {
            plugin.getLogger().info("Khong tim thay file tongmon.yml, dang tao file moi...");
            try {
                // Tạo file rỗng
                tongMonFile.createNewFile();
                // Tải cấu hình rỗng
                tongMonConfig = YamlConfiguration.loadConfiguration(tongMonFile);
                // Tạo mục gốc 'tong-mon' để tránh lỗi NullPointerException
                tongMonConfig.createSection("tong-mon");
                // Lưu lại file để tạo ra nó trên đĩa
                saveTongMonData();
            } catch (IOException e) {
                plugin.getLogger().severe("KHONG THE TAO FILE tongmon.yml!");
                e.printStackTrace();
            }
        }
        // Tải cấu hình từ file đã có hoặc vừa được tạo
        tongMonConfig = YamlConfiguration.loadConfiguration(tongMonFile);
    }

    /**
     * Lưu các thay đổi vào file tongmon.yml.
     */
    public void saveTongMonData() {
        try {
            tongMonConfig.save(tongMonFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu file tongmon.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Định dạng màu cho tin nhắn, hỗ trợ cả mã màu & và RGB (&#RRGGBB).
     */
    public String format(String message) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color.substring(1)) + "");
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Lấy tên hiển thị đã có màu của một tông môn.
     */
    public String getTenHienThi(String id) {
        return format(tongMonConfig.getString("tong-mon." + id + ".ten-hien-thi", id));
    }

    /**
     * Kiểm tra xem một ID tông môn đã tồn tại hay chưa.
     */
    public boolean isTenTongMonTonTai(String id) {
        return tongMonConfig.isConfigurationSection("tong-mon." + id);
    }

    /**
     * Lấy UUID của Trưởng Môn.
     */
    public UUID getChuTongMon(String id) {
        String uuidString = tongMonConfig.getString("tong-mon." + id + ".chu-tong-mon");
        return uuidString != null ? UUID.fromString(uuidString) : null;
    }

    /**
     * Kiểm tra xem một người chơi có phải là Trưởng Môn của tông môn đó không.
     */
    public boolean laChuTongMon(Player player, String tongMonId) {
        if (tongMonId == null || tongMonId.equalsIgnoreCase("none")) return false;
        UUID ownerUUID = getChuTongMon(tongMonId);
        return ownerUUID != null && player.getUniqueId().equals(ownerUUID);
    }

    /**
     * Tạo một tông môn mới và lưu vào file.
     */
    public void taoTongMon(Player chuTongMon, String id, String tenHienThi) {
        String path = "tong-mon." + id;
        tongMonConfig.set(path + ".ten-hien-thi", tenHienThi);
        tongMonConfig.set(path + ".chu-tong-mon", chuTongMon.getUniqueId().toString());
        tongMonConfig.set(path + ".thanh-vien", new ArrayList<String>()); // Khởi tạo danh sách thành viên rỗng
        saveTongMonData();
        
        plugin.getPlayerDataManager().getPlayerData(chuTongMon).setTongMonId(id);
    }

    /**
     * Xóa hoàn toàn một tông môn.
     */
    public void xoaTongMon(String id) {
        tongMonConfig.set("tong-mon." + id, null);
        saveTongMonData();
    }

    /**
     * Lấy danh sách UUID của tất cả thành viên trong tông môn.
     */
    public List<UUID> getThanhVien(String id) {
        List<String> uuidStrings = tongMonConfig.getStringList("tong-mon." + id + ".thanh-vien");
        return uuidStrings.stream().map(UUID::fromString).collect(Collectors.toList());
    }

    /**
     * Thêm một thành viên mới vào tông môn.
     */
    public void themThanhVien(String id, Player player) {
        List<String> thanhVien = tongMonConfig.getStringList("tong-mon." + id + ".thanh-vien");
        if (!thanhVien.contains(player.getUniqueId().toString())) {
            thanhVien.add(player.getUniqueId().toString());
            tongMonConfig.set("tong-mon." + id + ".thanh-vien", thanhVien);
            saveTongMonData();
        }
        plugin.getPlayerDataManager().getPlayerData(player).setTongMonId(id);
    }

    /**
     * Xóa một thành viên khỏi tông môn.
     */
    public void xoaThanhVien(String id, UUID playerUUID) {
        List<String> thanhVien = tongMonConfig.getStringList("tong-mon." + id + ".thanh-vien");
        thanhVien.remove(playerUUID.toString());
        tongMonConfig.set("tong-mon." + id + ".thanh-vien", thanhVien);
        saveTongMonData();
        
        // Cập nhật dữ liệu cho người chơi nếu họ đang online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            plugin.getPlayerDataManager().getPlayerData(player).setTongMonId("none");
        }
        // Nếu người chơi offline, dữ liệu sẽ được cập nhật khi họ vào lại server
    }

    /**
     * Lấy danh sách ID của tất cả các tông môn.
     */
    public Set<String> getTatCaTongMonIds() {
        ConfigurationSection section = tongMonConfig.getConfigurationSection("tong-mon");
        return section != null ? section.getKeys(false) : new HashSet<>();
    }
}