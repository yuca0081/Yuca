package org.yuca.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.function.Consumer;

/**
 * LangChain4j 统一服务接口
 * 提供基于 LangChain4j 框架的 AI 能力
 */
public interface LangChain4jService {

    /**
     * 同步聊天
     *
     * @param provider    模型提供商（如：qwen, openai）
     * @param sessionId   会话ID，用于管理记忆
     * @param userMessage 用户消息
     * @return AI响应文本
     */
    String chat(String provider, String sessionId, String userMessage);

    /**
     * 流式聊天
     *
     * @param provider     模型提供商
     * @param sessionId    会话ID
     * @param userMessage  用户消息
     * @param tokenHandler 流式Token处理器，接收每个token
     */
    void chatStream(String provider, String sessionId, String userMessage, Consumer<String> tokenHandler);

    /**
     * 带完整响应的流式聊天
     *
     * @param provider     模型提供商
     * @param sessionId    会话ID
     * @param userMessage  用户消息
     * @param tokenHandler 流式Token处理器
     * @return 完整的响应对象（包含token使用情况等）
     */
    Response<AiMessage> chatStreamWithResponse(String provider, String sessionId,
                                               String userMessage, Consumer<String> tokenHandler);

    /**
     * 带工具的聊天（同步）
     *
     * @param provider    模型提供商
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param tools       可用工具对象列表（Spring Bean）
     * @return AI响应文本（包含工具调用结果）
     */
    String chatWithTools(String provider, String sessionId, String userMessage, List<Object> tools);

    /**
     * 获取指定提供商的聊天模型
     *
     * @param provider 模型提供商
     * @return 聊天模型实例
     */
    ChatLanguageModel getChatModel(String provider);

    /**
     * 获取指定提供商的流式聊天模型
     *
     * @param provider 模型提供商
     * @return 流式聊天模型实例
     */
    StreamingChatLanguageModel getStreamingModel(String provider);

    /**
     * 清除指定会话的记忆
     *
     * @param sessionId 会话ID
     */
    void clearMemory(String sessionId);

    /**
     * 获取指定会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<dev.langchain4j.data.message.ChatMessage> getChatHistory(String sessionId);

    /**
     * 检查模型是否可用
     *
     * @param provider 模型提供商
     * @return 是否可用
     */
    boolean isModelAvailable(String provider);
}
