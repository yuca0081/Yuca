package org.yuca.ai.agent;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * 聊天拦截器接口
 * 所有上下文工程（SystemPrompt、Memory、RAG、Skill）统一实现此接口
 */
public interface ChatInterceptor {

    /**
     * 在 Agent 调用 ChatModel 前执行，用于注入上下文
     *
     * @param request 当前请求
     * @param context 请求上下文
     * @return 增强后的请求（可以返回同一个 request）
     */
    ChatRequest before(ChatRequest request, ChatContext context);

    /**
     * 在 Agent 完成后（含工具循环）执行，用于副作用（保存记忆等）
     *
     * @param response 最终响应
     * @param context  请求上下文
     */
    void after(ChatResponse response, ChatContext context);
}
