package com.myname.tiengioipre.utils;

import com.myname.tiengioipre.TienGioiPre;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugLogger {
    private static boolean DEBUG_MODE = false; // Đặt là true để bật debug
    private static String PLUGIN_NAME = "TienGioiPre";

    public static void setDebugMode(boolean mode) {
        DEBUG_MODE = mode;
    }

    public static void log(String tag, String message) {
        if (DEBUG_MODE) {
            JavaPlugin.getPlugin(TienGioiPre.class).getLogger().info("[" + tag + "] " + message);
        }
    }

    public static void warn(String tag, String message) {
        if (DEBUG_MODE) {
            JavaPlugin.getPlugin(TienGioiPre.class).getLogger().warning("[" + tag + "] " + message);
        }
    }
}