package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应Token
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamToken {

    /**
     * Token类型：thinking-思考内容，content-正式回答
     */
    private String type;

    /**
     * Token内容
     */
    private String content;

    /**
     * 创建思考内容Token
     */
    public static StreamToken thinking(String content) {
        return StreamToken.builder()
            .type("thinking")
            .content(content)
            .build();
    }

    /**
     * 创建推理内容Token（reasoning的别名，与千问API保持一致）
     */
    public static StreamToken reasoning(String content) {
        return thinking(content);
    }

    /**
     * 创建正式回答Token
     */
    public static StreamToken content(String content) {
        return StreamToken.builder()
            .type("content")
            .content(content)
            .build();
    }
}
