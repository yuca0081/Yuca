package org.yuca.yuca.note.enums;

import lombok.Getter;

/**
 * 文档状态枚举
 */
@Getter
public enum DocumentStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已发布
     */
    PUBLISHED("PUBLISHED", "已发布"),

    /**
     * 已归档
     */
    ARCHIVED("ARCHIVED", "已归档");

    /**
     * 状态代码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    DocumentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static DocumentStatus fromCode(String code) {
        for (DocumentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown document status: " + code);
    }

    /**
     * 是否为草稿
     */
    public boolean isDraft() {
        return this == DRAFT;
    }

    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return this == PUBLISHED;
    }

    /**
     * 是否已归档
     */
    public boolean isArchived() {
        return this == ARCHIVED;
    }
}
