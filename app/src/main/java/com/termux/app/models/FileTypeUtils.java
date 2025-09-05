package com.termux.app.models;

import com.termux.R;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件类型识别工具类
 * 根据文件扩展名识别文件类型
 */
public class FileTypeUtils {
    
    private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();
    
    static {
        // 代码文件
        EXTENSION_MAP.put("java", FileType.CODE_FILE);
        EXTENSION_MAP.put("js", FileType.CODE_FILE);
        EXTENSION_MAP.put("ts", FileType.CODE_FILE);
        EXTENSION_MAP.put("py", FileType.CODE_FILE);
        EXTENSION_MAP.put("cpp", FileType.CODE_FILE);
        EXTENSION_MAP.put("c", FileType.CODE_FILE);
        EXTENSION_MAP.put("h", FileType.CODE_FILE);
        EXTENSION_MAP.put("hpp", FileType.CODE_FILE);
        EXTENSION_MAP.put("cs", FileType.CODE_FILE);
        EXTENSION_MAP.put("go", FileType.CODE_FILE);
        EXTENSION_MAP.put("rs", FileType.CODE_FILE);
        EXTENSION_MAP.put("php", FileType.CODE_FILE);
        EXTENSION_MAP.put("rb", FileType.CODE_FILE);
        EXTENSION_MAP.put("swift", FileType.CODE_FILE);
        EXTENSION_MAP.put("kt", FileType.CODE_FILE);
        EXTENSION_MAP.put("scala", FileType.CODE_FILE);
        EXTENSION_MAP.put("pl", FileType.CODE_FILE);
        EXTENSION_MAP.put("sh", FileType.CODE_FILE);
        EXTENSION_MAP.put("bash", FileType.CODE_FILE);
        EXTENSION_MAP.put("zsh", FileType.CODE_FILE);
        EXTENSION_MAP.put("fish", FileType.CODE_FILE);
        EXTENSION_MAP.put("ps1", FileType.CODE_FILE);
        EXTENSION_MAP.put("bat", FileType.CODE_FILE);
        EXTENSION_MAP.put("cmd", FileType.CODE_FILE);
        EXTENSION_MAP.put("html", FileType.CODE_FILE);
        EXTENSION_MAP.put("htm", FileType.CODE_FILE);
        EXTENSION_MAP.put("css", FileType.CODE_FILE);
        EXTENSION_MAP.put("scss", FileType.CODE_FILE);
        EXTENSION_MAP.put("sass", FileType.CODE_FILE);
        EXTENSION_MAP.put("less", FileType.CODE_FILE);
        EXTENSION_MAP.put("jsx", FileType.CODE_FILE);
        EXTENSION_MAP.put("tsx", FileType.CODE_FILE);
        EXTENSION_MAP.put("vue", FileType.CODE_FILE);
        EXTENSION_MAP.put("dart", FileType.CODE_FILE);
        EXTENSION_MAP.put("r", FileType.CODE_FILE);
        EXTENSION_MAP.put("m", FileType.CODE_FILE);
        EXTENSION_MAP.put("mm", FileType.CODE_FILE);
        
        // 文本文件
        EXTENSION_MAP.put("txt", FileType.TEXT_FILE);
        EXTENSION_MAP.put("md", FileType.TEXT_FILE);
        EXTENSION_MAP.put("markdown", FileType.TEXT_FILE);
        EXTENSION_MAP.put("rst", FileType.TEXT_FILE);
        EXTENSION_MAP.put("asciidoc", FileType.TEXT_FILE);
        EXTENSION_MAP.put("adoc", FileType.TEXT_FILE);
        EXTENSION_MAP.put("org", FileType.TEXT_FILE);
        EXTENSION_MAP.put("tex", FileType.TEXT_FILE);
        
        // 配置文件
        EXTENSION_MAP.put("json", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("xml", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("yaml", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("yml", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("toml", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("ini", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("conf", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("config", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("properties", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("gradle", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("pom", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("dockerfile", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("dockerignore", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("gitignore", FileType.CONFIG_FILE);
        EXTENSION_MAP.put("gitconfig", FileType.CONFIG_FILE);
        
        // 日志文件
        EXTENSION_MAP.put("log", FileType.LOG_FILE);
        EXTENSION_MAP.put("logs", FileType.LOG_FILE);
        EXTENSION_MAP.put("out", FileType.LOG_FILE);
        
        // 图片文件
        EXTENSION_MAP.put("jpg", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("jpeg", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("png", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("gif", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("bmp", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("webp", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("svg", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("ico", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("tiff", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("tif", FileType.IMAGE_FILE);
        
        // 压缩文件
        EXTENSION_MAP.put("zip", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("rar", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("7z", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("tar", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("gz", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("bz2", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("xz", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("lz", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("lzma", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("tgz", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("tbz2", FileType.ARCHIVE_FILE);
        EXTENSION_MAP.put("txz", FileType.ARCHIVE_FILE);
        
        // 文档文件
        EXTENSION_MAP.put("pdf", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("doc", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("docx", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("xls", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("xlsx", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("ppt", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("pptx", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("odt", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("ods", FileType.DOCUMENT_FILE);
        EXTENSION_MAP.put("odp", FileType.DOCUMENT_FILE);
        
        // 媒体文件
        EXTENSION_MAP.put("mp4", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("avi", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("mkv", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("mov", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("wmv", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("flv", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("webm", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("mp3", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("wav", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("flac", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("aac", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("ogg", FileType.MEDIA_FILE);
        EXTENSION_MAP.put("wma", FileType.MEDIA_FILE);
        
        // 可执行文件
        EXTENSION_MAP.put("exe", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("msi", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("app", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("deb", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("rpm", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("apk", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("dmg", FileType.EXECUTABLE_FILE);
        EXTENSION_MAP.put("pkg", FileType.EXECUTABLE_FILE);
    }
    
    /**
     * 根据文件名获取文件类型
     */
    public static FileType getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return FileType.UNKNOWN_FILE;
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        return EXTENSION_MAP.getOrDefault(extension, FileType.UNKNOWN_FILE);
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        // 处理特殊文件名
        if (fileName.equals("Dockerfile") || fileName.equals("dockerfile")) {
            return "dockerfile";
        }
        if (fileName.equals(".gitignore")) {
            return "gitignore";
        }
        if (fileName.equals(".gitconfig")) {
            return "gitconfig";
        }
        if (fileName.equals(".dockerignore")) {
            return "dockerignore";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 获取文件类型对应的图标资源ID
     */
    public static int getFileTypeIcon(FileType fileType) {
        switch (fileType) {
            case DIRECTORY:
                return R.drawable.ic_folder_blue;
            case CODE_FILE:
                return android.R.drawable.ic_menu_edit;
            case TEXT_FILE:
                return R.drawable.ic_file_document;
            case CONFIG_FILE:
                return android.R.drawable.ic_menu_preferences;
            case LOG_FILE:
                return android.R.drawable.ic_menu_info_details;
            case IMAGE_FILE:
                return R.drawable.ic_file_image;
            case ARCHIVE_FILE:
                return R.drawable.ic_file_archive;
            case DOCUMENT_FILE:
                return R.drawable.ic_file_document;
            case MEDIA_FILE:
                return android.R.drawable.ic_media_play;
            case EXECUTABLE_FILE:
                return android.R.drawable.ic_menu_send;
            default:
                return R.drawable.ic_file_document;
        }
    }
    
    /**
     * 判断文件是否可以预览
     */
    public static boolean isPreviewable(FileType fileType) {
        return fileType.isTextFile() || fileType.isPreviewableImage();
    }
    
    /**
     * 判断文件是否可以编辑
     */
    public static boolean isEditable(FileType fileType) {
        return fileType.isTextFile();
    }
}