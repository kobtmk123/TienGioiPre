package com.yourname.tiengioipre.api;

import com.yourname.tiengioipre.TienGioiPre;
import com.yourname.tiengioipre.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Lớp API chính thức cho plugin TienGioiPre.
 * Cung cấp các phương thức an toàn để các plugin khác có thể lấy dữ liệu
 * mà không cần truy cập trực tiếp vào mã nguồn bên trong.
 *
 * Cách sử dụng trong plugin khác:
 * TienGioiAPI api = TienGioiPre.getAPI();
 * if (api != null) {
 *     String realm = api.getPlayerRealmName(player);
 * }
 */
public class TienGioiAPI {

    private final TienGioiPre plugin;

    public TienGioiAPI(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    /**
     * Lấy tên hiển thị cảnh giới hiện tại của người chơi.
     * @param player Người chơi cần kiểm tra.
     * @return Tên hiển thị của cảnh giới (đã định dạng màu), hoặc "Không rõ" nếu không tìm thấy.
     */
    public String getPlayerRealmName(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return "Không rõ";
        }
        return plugin.getRealmManager().getRealmDisplayName(data.getRealmId());
    }

    /**
     * Lấy ID cảnh giới hiện tại của người chơi (ví dụ: "trucco_soky_tang1").
     * @param player Người chơi cần kiểm tra.
     * @return Chuỗi ID của cảnh giới, hoặc null nếu không tìm thấy người chơi.
     */
    public String getPlayerRealmId(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return (data != null) ? data.getRealmId() : null;
    }

    /**
     * Lấy số linh khí hiện tại của người chơi.
     * @param player Người chơi cần kiểm tra.
     * @return Số linh khí hiện tại, hoặc -1 nếu không tìm thấy người chơi.
     */
    public double getPlayerLinhKhi(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return (data != null) ? data.getLinhKhi() : -1;
    }

    /**
     * Lấy số linh khí tối đa cho cảnh giới hiện tại của người chơi.
     * @param player Người chơi cần kiểm tra.
     * @return Số linh khí tối đa, hoặc -1 nếu không tìm thấy người chơi.
     */
    public double getPlayerMaxLinhKhi(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return -1;
        }
        return plugin.getRealmManager().getMaxLinhKhi(data.getRealmId());
    }

    /**
     * Thêm một lượng linh khí cho người chơi.
     * @param player Người chơi nhận linh khí.
     * @param amount Lượng linh khí cần thêm.
     */
    public void addLinhKhi(Player player, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.addLinhKhi(amount);
        }
    }

    /**
     * Đặt một lượng linh khí cụ thể cho người chơi.
     * @param player Người chơi cần đặt linh khí.
     * @param amount Lượng linh khí muốn đặt.
     */
    public void setLinhKhi(Player player, double amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.setLinhKhi(amount);
        }
    }
}