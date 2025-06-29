package com.yourname.tiengioipre.commands;

import com.yourname.tiengioipre.TienGioiPre;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopTienGioiCommand implements CommandExecutor {

    private final TienGioiPre plugin;

    public ShopTienGioiCommand(TienGioiPre plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getShopGUI().open(player);
        return true;
    }
}