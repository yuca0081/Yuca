package org.yuca.ai.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 带Memory的聊天服务接口
 * 使用LangChain4j的AiServices手动创建
 *
 * 根据LangChain4j官方文档：
 * https://docs.langchain4j.dev/tutorials/chat-memory#with-aiservices
 */
public interface MemoryChatService {

    @SystemMessage("""
        你是一个友好的AI助手，能够记住之前的对话内容。
        请用简洁、准确的方式回答用户的问题。
        """)
    Result<String> chat(@UserMessage String userMessage);
}
