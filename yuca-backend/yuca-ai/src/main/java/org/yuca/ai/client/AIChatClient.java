package org.yuca.ai.client;

import org.yuca.ai.model.*;
import java.util.function.Consumer;

/**
 * AI 聊天客户端接口
 *
 * <p>提供统一的 AI 聊天抽象，支持同步和流式调用
 * <p>
 * 使用泛型支持不同厂商的独立请求类型，避免强制转换和复杂转换逻辑
 *
 * @param <T> 聊天请求类型（如 ChatRequest、QwenChatRequest 等）
 * @author Yuca
 * @since 2025-01-27
 */
public interface AIChatClient<T> {

    /**
     * 同步聊天（非流式，完整响应）
     *
     * @param request 聊天请求
     * @return 非流式聊天响应（包含完整的message和token使用统计）
     */
    ChatResponse chat(T request);

    /**
     * 流式聊天
     *
     * @param request 请求参数
     * @param tokenHandler Token处理器（接收带类型的token：thinking/content）
     * @return 流式聊天响应（包含token使用统计，仅在最后一个chunk中）
     */
    ChatStreamResponse chatStream(T request, Consumer<StreamToken> tokenHandler);

}
