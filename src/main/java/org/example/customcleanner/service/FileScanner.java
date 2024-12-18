package org.example.customcleanner.service;

import org.example.customcleanner.model.FileItem;
import org.example.customcleanner.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {
    public List<FileItem> scanFiles(String type) {
        List<FileItem> junkFiles = new ArrayList<>();
        String[] locations = getLocationsForType(type);

        for (String location : locations) {
            File directory = new File(location);
            if (directory.exists()) {
                searchJunkFilesInDirectory(directory, junkFiles, type);
            }
        }

        return junkFiles;
    }

    private String[] getLocationsForType(String type) {
        switch (type) {
            case "TEMP":
                return new String[]{
                        System.getProperty("java.io.tmpdir"),
                        System.getProperty("user.home") + "/AppData/Local/Temp",
                        "C:/Windows/Temp"
                };
            case "PREFETCH":
                return new String[]{"C:/Windows/Prefetch"};
            case "DOWNLOADS":
                return new String[]{System.getProperty("user.home") + "/Downloads"};
            case "RECYCLE":
                return new String[]{"C:/$Recycle.Bin"};
            default:
                return new String[]{};
        }
    }

    private void searchJunkFilesInDirectory(File directory, List<FileItem> junkFiles, String type) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && FileUtils.isJunkFile(file, type)) {
                    junkFiles.add(new FileItem(
                            file.getName(),
                            file.getAbsolutePath(),
                            FileUtils.formatFileSize(file.length()),
                            type,
                            true
                    ));
                }
            }
        }
    }
}