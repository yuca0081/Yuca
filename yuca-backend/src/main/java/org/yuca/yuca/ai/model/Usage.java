package org.yuca.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class Usage {

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
