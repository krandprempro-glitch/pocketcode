package com.termux.app.utils;

import com.termux.R;
import com.termux.app.models.RemoteFileItem;

/**
 * 文件图标工具类
 * 根据文件类型返回相应的图标资源
 */
public class FileIconUtils {
    
    public static int getFileIcon(RemoteFileItem file) {
        if (file.isDirectory()) {
            return R.drawable.ic_folder_blue;
        }
        
        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);
        
        // 代码文件
        if (isCodeFile(extension)) {
            return R.drawable.ic_file_code;
        }
        
        // 文档文件
        if (isDocumentFile(extension)) {
            return R.drawable.ic_file_document;
        }
        
        // 图片文件
        if (isImageFile(extension)) {
            return R.drawable.ic_file_image;
        }
        
        // 压缩文件
        if (isArchiveFile(extension)) {
            return R.drawable.ic_file_archive;
        }
        
        // 文本文件
        if (isTextFile(extension)) {
            return R.drawable.ic_file_text;
        }
        
        // 默认文件图标
        return R.drawable.ic_file_unknown;
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    private static boolean isCodeFile(String extension) {
        return extension.equals("java") || extension.equals("kt") || extension.equals("js") ||
               extension.equals("ts") || extension.equals("py") || extension.equals("cpp") ||
               extension.equals("c") || extension.equals("h") || extension.equals("css") ||
               extension.equals("html") || extension.equals("xml") || extension.equals("json") ||
               extension.equals("gradle") || extension.equals("properties") || extension.equals("yml") ||
               extension.equals("yaml") || extension.equals("sh") || extension.equals("sql") ||
               extension.equals("php") || extension.equals("rb") || extension.equals("go") ||
               extension.equals("rs") || extension.equals("swift");
    }
    
    private static boolean isDocumentFile(String extension) {
        return extension.equals("pdf") || extension.equals("doc") || extension.equals("docx") ||
               extension.equals("xls") || extension.equals("xlsx") || extension.equals("ppt") ||
               extension.equals("pptx") || extension.equals("rtf");
    }
    
    private static boolean isImageFile(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") ||
               extension.equals("gif") || extension.equals("bmp") || extension.equals("svg") ||
               extension.equals("ico") || extension.equals("webp");
    }
    
    private static boolean isArchiveFile(String extension) {
        return extension.equals("zip") || extension.equals("rar") || extension.equals("7z") ||
               extension.equals("tar") || extension.equals("gz") || extension.equals("bz2") ||
               extension.equals("xz") || extension.equals("jar") || extension.equals("apk");
    }
    
    private static boolean isTextFile(String extension) {
        return extension.equals("txt") || extension.equals("log") || extension.equals("md") ||
               extension.equals("readme") || extension.equals("cfg") || extension.equals("conf") ||
               extension.equals("ini") || extension.equals("env");
    }
}