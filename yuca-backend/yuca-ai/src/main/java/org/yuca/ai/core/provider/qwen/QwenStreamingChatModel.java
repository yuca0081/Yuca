package org.yuca.ai.core.provider.qwen;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.core.message.AiMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.core.model.StreamingChatModel;
import org.yuca.ai.core.model.StreamingChatResponseHandler;
import org.yuca.ai.core.provider.openai.dto.ChatCompletionChunk;
import org.yuca.ai.core.provider.openai.dto.ChatMessageDto;
import org.yuca.ai.core.provider.openai.dto.FunctionCall;
import org.yuca.ai.core.provider.openai.dto.ToolCall;
import org.yuca.ai.core.provider.openai.dto.Usage;
import org.yuca.ai.core.tool.ToolExecutionRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope / Qwen 流式聊天模型。
 * 走 OpenAI 兼容端点 POST /compatible-mode/v1/chat/completions，stream=true，SSE 协议。
 * <p>
 * 使用 JDK HttpClient（零新增依赖）。
 */
@Slf4j
public class QwenStreamingChatModel implements StreamingChatModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;

    public QwenStreamingChatModel(String baseUrl, String apiKey, String modelName) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        try {
            Object body = QwenConverters.toRequestBody(request, modelName, true);
            String json = MAPPER.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errBody = response.body().collect(java.util.stream.Collectors.joining("\n"));
                handler.onError(new RuntimeException("流式响应失败 HTTP " + response.statusCode() + ": " + errBody));
                return;
            }

            StreamAccumulator acc = new StreamAccumulator();
            response.body()
                    .filter(line -> line.startsWith("data:"))
                    .forEach(line -> {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            return;
                        }
                        try {
                            ChatCompletionChunk chunk = MAPPER.readValue(data, ChatCompletionChunk.class);
                            acc.consume(chunk, handler);
                        } catch (Exception e) {
                            log.warn("解析流式 chunk 失败: {}", data, e);
                        }
                    });

            ChatResponse finalResponse = acc.buildResponse();
            handler.onCompleteResponse(finalResponse);
        } catch (Exception e) {
            log.error("流式聊天失败", e);
            handler.onError(e);
        }
    }

    /**
     * 流式响应累积器：累积 content、reasoning_content、tool_calls，最后构造 ChatResponse。
     */
    private static class StreamAccumulator {
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final Map<Integer, ToolCallBuf> toolCalls = new LinkedHashMap<>();
        private Usage usage;
        private String finishReason;

        void consume(ChatCompletionChunk chunk, StreamingChatResponseHandler handler) {
            if (chunk.usage() != null) {
                usage = chunk.usage();
            }
            if (chunk.choices() == null || chunk.choices().isEmpty()) {
                return;
            }
            for (var choice : chunk.choices()) {
                if (choice.finishReason() != null) {
                    finishReason = choice.finishReason();
                }
                ChatMessageDto delta = choice.delta();
                if (delta == null) continue;

                if (delta.content() != null && !delta.content().isEmpty()) {
                    content.append(delta.content());
                    handler.onPartialResponse(delta.content());
                }
                if (delta.reasoningContent() != null && !delta.reasoningContent().isEmpty()) {
                    reasoning.append(delta.reasoningContent());
                }
                if (delta.toolCalls() != null) {
                    for (ToolCall tc : delta.toolCalls()) {
                        ToolCallBuf buf = toolCalls.computeIfAbsent(
                                tc.index() != null ? tc.index() : toolCalls.size(),
                                k -> new ToolCallBuf());
                        buf.merge(tc);
                    }
                }
            }
        }

        ChatResponse buildResponse() {
            List<ToolExecutionRequest> requests = new ArrayList<>();
            for (ToolCallBuf buf : toolCalls.values()) {
                requests.add(ToolExecutionRequest.builder()
                        .id(buf.id)
                        .name(buf.name)
                        .arguments(buf.arguments.toString())
                        .build());
            }

            AiMessage ai = requests.isEmpty()
                    ? AiMessage.from(content.toString())
                    : AiMessage.from(content.toString(), requests);

            org.yuca.ai.core.model.TokenUsage tu = usage == null ? null
                    : org.yuca.ai.core.model.TokenUsage.of(
                            usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
            return new ChatResponse(ai, tu);
        }
    }

    /** 单个工具调用的累积缓冲（流式 tool_calls 按 index 增量到达）。 */
    private static class ToolCallBuf {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();

        void merge(ToolCall tc) {
            if (tc.id() != null) id = tc.id();
            if (tc.function() != null) {
                FunctionCall fn = tc.function();
                if (fn.name() != null) name = fn.name();
                if (fn.arguments() != null) arguments.append(fn.arguments());
            }
        }
    }
}
