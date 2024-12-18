package org.example.customcleanner.util;

import java.io.File;
import java.text.DecimalFormat;

public class FileUtils {
    public static String formatFileSize(double size) {
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }

    public static boolean isJunkFile(File file, String type) {
        String name = file.getName().toLowerCase();

        switch (type) {
            case "TEMP":
                return isTempFile(name);
            case "PREFETCH":
                return isPrefetchFile(name);
            case "DOWNLOADS":
                return isDownloadFile(name);
            case "RECYCLE":
                return isRecycleBinFile(name);
            default:
                return false;
        }
    }

    private static boolean isTempFile(String name) {
        String[] tempExtensions = {".tmp", ".temp", ".log", ".old", ".bak", ".chk"};
        for (String ext : tempExtensions) {
            if (name.endsWith(ext)) return true;
        }
        return name.startsWith("~$") || name.matches("^[~].*");
    }

    private static boolean isPrefetchFile(String name) {
        return name.endsWith(".pf") || name.matches(".*[.][0-9]+$");
    }

    private static boolean isDownloadFile(String name) {
        return name.endsWith(".part") || name.endsWith(".crdownload") ||
                name.endsWith(".download") || name.matches("^[0-9a-f]{8}$");
    }

    private static boolean isRecycleBinFile(String name) {
        return name.startsWith("$R") || name.startsWith("$I");
    }
}