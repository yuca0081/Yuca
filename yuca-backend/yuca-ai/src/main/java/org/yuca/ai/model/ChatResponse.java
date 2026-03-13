package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 回复内容
     */
    private String content;

    /**
     * Token 使用情况
     */
    private Usage usage;

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * 模型名称
     */
    private String model;

    /**
     * Token 使用情况统计
     *
     * @author Yuca
     * @since 2025-01-27
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {

        /**
         * 输入 token 数
         */
        private Integer inputTokens;

        /**
         * 输出 token 数
         */
        private Integer outputTokens;

        /**
         * 获取总 token 数
         *
         * @return 总 token 数，如果任一值为 null 则返回 null
         */
        public Integer getTotalTokens() {
            if (inputTokens == null || outputTokens == null) {
                return null;
            }
            return inputTokens + outputTokens;
        }
    }

}
