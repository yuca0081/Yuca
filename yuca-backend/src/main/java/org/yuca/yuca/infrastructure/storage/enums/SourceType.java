package org.yuca.yuca.infrastructure.storage.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 来源类型枚举
 */
public enum SourceType {

    /**
     * 用户上传
     */
    USER("user"),

    /**
     * 系统生成
     */
    SYSTEM("system"),

    /**
     * 第三方同步
     */
    THIRD_PARTY("third_party");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
