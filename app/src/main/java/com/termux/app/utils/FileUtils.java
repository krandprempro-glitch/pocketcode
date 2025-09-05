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
    
    /**
     * 检查是否为图片文件
     */
    public static boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName.toLowerCase());
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") ||
               extension.equals("gif") || extension.equals("bmp") || extension.equals("webp") ||
               extension.equals("svg") || extension.equals("ico") || extension.equals("tiff");
    }
    
    /**
     * 检查是否为视频文件
     */
    public static boolean isVideoFile(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName.toLowerCase());
        return extension.equals("mp4") || extension.equals("avi") || extension.equals("mkv") ||
               extension.equals("mov") || extension.equals("wmv") || extension.equals("flv") ||
               extension.equals("webm") || extension.equals("m4v") || extension.equals("3gp");
    }
    
    /**
     * 检查是否为音频文件
     */
    public static boolean isAudioFile(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName.toLowerCase());
        return extension.equals("mp3") || extension.equals("wav") || extension.equals("flac") ||
               extension.equals("aac") || extension.equals("ogg") || extension.equals("wma") ||
               extension.equals("m4a") || extension.equals("opus") || extension.equals("aiff");
    }
    
    /**
     * 检查是否为压缩文件
     */
    public static boolean isArchiveFile(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName.toLowerCase());
        return extension.equals("zip") || extension.equals("rar") || extension.equals("7z") ||
               extension.equals("tar") || extension.equals("gz") || extension.equals("bz2") ||
               extension.equals("xz") || extension.equals("jar") || extension.equals("apk") ||
               extension.equals("deb") || extension.equals("rpm");
    }
    
    /**
     * 检查是否为文档文件
     */
    public static boolean isDocumentFile(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName.toLowerCase());
        return extension.equals("pdf") || extension.equals("doc") || extension.equals("docx") ||
               extension.equals("xls") || extension.equals("xlsx") || extension.equals("ppt") ||
               extension.equals("pptx") || extension.equals("odt") || extension.equals("ods") ||
               extension.equals("odp") || extension.equals("rtf");
    }
    
    /**
     * 获取MIME类型
     */
    public static String getMimeType(String fileName) {
        if (fileName == null) return "";
        
        String extension = getFileExtension(fileName.toLowerCase());
        
        // 图片类型
        if (extension.equals("jpg") || extension.equals("jpeg")) return "image/jpeg";
        if (extension.equals("png")) return "image/png";
        if (extension.equals("gif")) return "image/gif";
        if (extension.equals("webp")) return "image/webp";
        if (extension.equals("svg")) return "image/svg+xml";
        
        // 视频类型
        if (extension.equals("mp4")) return "video/mp4";
        if (extension.equals("avi")) return "video/x-msvideo";
        if (extension.equals("mkv")) return "video/x-matroska";
        if (extension.equals("webm")) return "video/webm";
        
        // 音频类型
        if (extension.equals("mp3")) return "audio/mpeg";
        if (extension.equals("wav")) return "audio/wav";
        if (extension.equals("ogg")) return "audio/ogg";
        if (extension.equals("m4a")) return "audio/mp4";
        
        // 文档类型
        if (extension.equals("pdf")) return "application/pdf";
        if (extension.equals("doc")) return "application/msword";
        if (extension.equals("docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (extension.equals("xls")) return "application/vnd.ms-excel";
        if (extension.equals("xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        
        // 压缩文件类型
        if (extension.equals("zip")) return "application/zip";
        if (extension.equals("rar")) return "application/vnd.rar";
        if (extension.equals("7z")) return "application/x-7z-compressed";
        
        // 代码文件类型
        if (extension.equals("html")) return "text/html";
        if (extension.equals("css")) return "text/css";
        if (extension.equals("js")) return "text/javascript";
        if (extension.equals("json")) return "application/json";
        if (extension.equals("xml")) return "text/xml";
        if (extension.equals("txt")) return "text/plain";
        
        return "application/octet-stream"; // 默认二进制类型
    }
}