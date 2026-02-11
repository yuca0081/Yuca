package org.yuca.yuca.note.enums;

import lombok.Getter;

/**
 * 节点类型枚举
 */
@Getter
public enum ItemType {

    /**
     * 文件夹
     */
    FOLDER("FOLDER", "文件夹"),

    /**
     * 文档
     */
    DOCUMENT("DOCUMENT", "文档");

    /**
     * 类型代码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    ItemType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static ItemType fromCode(String code) {
        for (ItemType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown item type: " + code);
    }

    /**
     * 是否为文件夹
     */
    public boolean isFolder() {
        return this == FOLDER;
    }

    /**
     * 是否为文档
     */
    public boolean isDocument() {
        return this == DOCUMENT;
    }
}
