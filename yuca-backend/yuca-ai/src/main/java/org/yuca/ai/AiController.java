package org.yuca.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yuca.ai.service.Assistant;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

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

}
