package org.yuca.assistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 角色：user/assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 使用的模型名称（仅assistant角色消息有值）
     */
    private String modelName;

    /**
     * 深度思考内容（可选）
     */
    private String thinkingContent;

    /**
     * 输入token数
     */
    private Integer inputTokens;

    /**
     * 输出token数
     */
    private Integer outputTokens;

    /**
     * 输入token详细信息（JSON格式，对应prompt_tokens_details）
     * 包含：cached_tokens, audio_tokens, text_tokens, image_tokens, video_tokens
     */
    private String promptTokensDetails;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
