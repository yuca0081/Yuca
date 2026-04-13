package org.yuca.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chat Memory 配置
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 创建使用PostgreSQL的ChatMemoryProvider
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(50)  // 保留最近50条消息
                        .chatMemoryStore(chatMemoryStore)  // 使用PostgreSQL存储
                        .build();
    }
}
