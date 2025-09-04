package com.termux.app.models;

/**
 * 文件类型枚举
 * 用于识别和分类不同类型的文件
 */
public enum FileType {
    DIRECTORY("文件夹"),
    TEXT_FILE("文本文件"),
    CODE_FILE("代码文件"),
    IMAGE_FILE("图片文件"),
    ARCHIVE_FILE("压缩文件"),
    EXECUTABLE_FILE("可执行文件"),
    CONFIG_FILE("配置文件"),
    LOG_FILE("日志文件"),
    DOCUMENT_FILE("文档文件"),
    MEDIA_FILE("媒体文件"),
    UNKNOWN_FILE("未知文件");
    
    private final String description;
    
    FileType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为文本文件类型
     */
    public boolean isTextFile() {
        return this == TEXT_FILE || this == CODE_FILE || this == CONFIG_FILE || this == LOG_FILE;
    }
    
    /**
     * 判断是否为可预览的图片类型
     */
    public boolean isPreviewableImage() {
        return this == IMAGE_FILE;
    }
    
    /**
     * 判断是否为媒体文件类型
     */
    public boolean isMediaFile() {
        return this == MEDIA_FILE || this == IMAGE_FILE;
    }
}