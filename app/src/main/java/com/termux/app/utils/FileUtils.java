package com.termux.app.utils;

/**
 * 文件工具类
 */
public class FileUtils {
    
    /**
     * 格式化文件大小显示
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 检查是否为代码文件
     */
    public static boolean isCodeFile(String fileName) {
        if (fileName == null) return false;
        
        String extension = getFileExtension(fileName.toLowerCase());
        return isCodeExtension(extension);
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    private static boolean isCodeExtension(String extension) {
        return extension.equals("java") || extension.equals("kt") || extension.equals("js") ||
               extension.equals("ts") || extension.equals("py") || extension.equals("cpp") ||
               extension.equals("c") || extension.equals("h") || extension.equals("css") ||
               extension.equals("html") || extension.equals("xml") || extension.equals("json") ||
               extension.equals("gradle") || extension.equals("properties") || extension.equals("yml") ||
               extension.equals("yaml") || extension.equals("sh") || extension.equals("sql") ||
               extension.equals("php") || extension.equals("rb") || extension.equals("go") ||
               extension.equals("rs") || extension.equals("swift") || extension.equals("md") ||
               extension.equals("txt") || extension.equals("log");
    }
}