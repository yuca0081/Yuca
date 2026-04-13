package org.yuca.ai.controller;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.service.Assistant;
import org.yuca.ai.service.MemoryChatService;
import org.yuca.ai.tool.Calculator;
import org.yuca.common.annotation.SkipAuth;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@SkipAuth  // 跳过JWT认证，方便测试
public class AiController {

    private final ChatMemoryProvider chatMemoryProvider;

    @GetMapping("/chat")
    public String chat(String message){
        ChatModel model = QwenChatModel.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .modelName("qwen3.5-flash")
                .build();
        Assistant assistant = AiServices.create(Assistant.class, model);
        String chat = assistant.chat(message);
        return chat;
    }

    @GetMapping("/streamChat")
    public Flux<String> streamChat(String message){
        log.info("收到流式聊天请求，消息: {}", message);

        StreamingChatModel model = QwenStreamingChatModel.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .modelName("qwen3.5-flash")
                .build();
        Assistant assistant = AiServices.create(Assistant.class, model);
        Flux<String> chat = assistant.streamChat(message)
                .doOnNext(token -> {
                    log.info("流式输出: {}", token);
                    System.out.print(token);
                })
                .doOnComplete(() -> {
                    log.info("流式输出完成");
                    System.out.println("\n[完成]");
                })
                .doOnError(error -> {
                    log.error("流式输出错误", error);
                });
        return chat;
    }

    /**
     * 带Memory的聊天接口
     * 消息会自动保存到PostgreSQL数据库，AI能记住之前的对话内容
     *
     * @param message 用户消息
     * @param sessionId 会话ID，用于区分不同的对话会话（默认: "default"）
     * @return 包含AI响应和会话信息的Map
     */
    @GetMapping("/chatWithMemory")
    public Map<String, Object> chatWithMemory(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        log.info("收到带memory的聊天请求，sessionId: {}, 消息: {}", sessionId, message);

        try {
            AiMessage build = AiMessage.builder().build();
            // 创建ChatModel
            ChatModel model = QwenChatModel.builder()
                    .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                    .modelName("qwen3.5-flash")
                    .build();
            // 获取ChatMemory（会自动使用PostgreSQL存储）
            MessageWindowChatMemory memory =
                    (MessageWindowChatMemory) chatMemoryProvider.get(sessionId);
            // 创建ChatService
            MemoryChatService chatService = AiServices.builder(MemoryChatService.class)
                    .chatModel(model)
                    .chatMemory(memory)
                    .contentRetriever()
                    .tools(new Calculator())
                    .build();

            Result<String> response = chatService.chat(message);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("userMessage", message);
            result.put("response", response);

            log.info("Session {} 聊天完成", sessionId);

            return result;

        } catch (Exception e) {
            log.error("聊天处理失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("sessionId", sessionId);

            return error;
        }
    }
}
