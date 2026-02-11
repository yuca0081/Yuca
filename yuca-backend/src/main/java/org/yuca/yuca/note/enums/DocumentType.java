package org.yuca.yuca.note.enums;

import lombok.Getter;

/**
 * 文档内容类型枚举
 */
@Getter
public enum DocumentType {

    /**
     * Markdown格式
     */
    MARKDOWN("MARKDOWN", "Markdown"),

    /**
     * 富文本格式
     */
    RICH_TEXT("RICH_TEXT", "富文本");

    /**
     * 类型代码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    DocumentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static DocumentType fromCode(String code) {
        for (DocumentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown document type: " + code);
    }

    /**
     * 是否为Markdown格式
     */
    public boolean isMarkdown() {
        return this == MARKDOWN;
    }

    /**
     * 是否为富文本格式
     */
    public boolean isRichText() {
        return this == RICH_TEXT;
    }
}
