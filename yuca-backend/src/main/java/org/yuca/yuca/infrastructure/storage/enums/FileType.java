package org.yuca.yuca.infrastructure.storage.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 文件类型枚举
 */
@Getter
public enum FileType {

    /**
     * 图片
     */
    IMAGE("image", new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"}),

    /**
     * 视频
     */
    VIDEO("video", new String[]{".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv"}),

    /**
     * 文档
     */
    DOCUMENT("document", new String[]{".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt"}),

    /**
     * 压缩文件
     */
    ARCHIVE("archive", new String[]{".zip", ".rar", ".7z", ".tar", ".gz"}),

    /**
     * 其他
     */
    OTHER("other", new String[]{});

    /**
     * 文件分类
     */
    private final String category;

    /**
     * 文件扩展名
     */
    private final String[] extensions;

    FileType(String category, String[] extensions) {
        this.category = category;
        this.extensions = extensions;
    }

    /**
     * 根据文件名获取文件类型
     */
    public static FileType fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return OTHER;
        }

        String lowerFileName = fileName.toLowerCase();
        int lastDotIndex = lowerFileName.lastIndexOf(".");

        if (lastDotIndex == -1) {
            return OTHER;
        }

        String extension = lowerFileName.substring(lastDotIndex);

        for (FileType fileType : values()) {
            if (Arrays.stream(fileType.getExtensions())
                .anyMatch(ext -> ext.equals(extension))) {
                return fileType;
            }
        }

        return OTHER;
    }

    /**
     * 检查是否为图片
     */
    public boolean isImage() {
        return this == IMAGE;
    }

    /**
     * 检查扩展名是否匹配
     */
    public boolean matches(String extension) {
        return Arrays.stream(this.extensions)
            .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }
}
