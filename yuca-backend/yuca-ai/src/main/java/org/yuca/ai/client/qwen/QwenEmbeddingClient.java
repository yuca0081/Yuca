package org.yuca.ai.client.qwen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.yuca.ai.client.AIEmbeddingClient;
import org.yuca.ai.model.EmbeddingRequest;
import org.yuca.ai.model.EmbeddingResponse;

import java.util.List;
import java.util.Map;

/**
 * 通义千问 Embedding Provider 实现（HTTP 方式）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qwen.api-key")
public class QwenEmbeddingClient implements AIEmbeddingClient {

    private final RestTemplate restTemplate;
    private final QwenConfig config;

    private static final String EMBEDDINGS_ENDPOINT = "/embeddings";
    private static final int MAX_BATCH_SIZE = 25;

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 如果输入数量超过最大批次大小，进行分批处理
            if (request.getInputs().size() > MAX_BATCH_SIZE) {
                return embedBatch(request);
            }

            // 1. 构建请求参数
            QwenEmbeddingRequest qwenRequest = buildRequest(request);

            // 2. 记录请求日志
            logRequest(request, qwenRequest);

            // 3. 调用 API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<QwenEmbeddingRequest> entity = new HttpEntity<>(qwenRequest, headers);

            ResponseEntity<EmbeddingResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + EMBEDDINGS_ENDPOINT,
                entity,
                EmbeddingResponse.class
            );

            EmbeddingResponse response = responseEntity.getBody();

            // 4. 记录响应日志
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("千问 Embedding 调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分批处理大量文本
     */
    private EmbeddingResponse embedBatch(EmbeddingRequest request) {
        log.info("输入文本数量({})超过最大批次大小({})，启用分批处理",
            request.getInputs().size(), MAX_BATCH_SIZE);

        List<String> inputs = request.getInputs();
        EmbeddingResponse result = null;
        int batchCounter = 0;

        for (int i = 0; i < inputs.size(); i += MAX_BATCH_SIZE) {
            // 构建当前批次
            int end = Math.min(i + MAX_BATCH_SIZE, inputs.size());
            List<String> batch = inputs.subList(i, end);

            log.info("处理批次 [{}/{}]，文本数: {}",
                (i / MAX_BATCH_SIZE) + 1, (inputs.size() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE, batch.size());

            // 构建批次请求
            EmbeddingRequest batchRequest = EmbeddingRequest.builder()
                .inputs(batch)
                .model(request.getModel())
                .dimensions(request.getDimensions())
                .build();

            // 调用 API
            QwenEmbeddingRequest qwenRequest = buildRequest(batchRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<QwenEmbeddingRequest> entity = new HttpEntity<>(qwenRequest, headers);

            ResponseEntity<EmbeddingResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + EMBEDDINGS_ENDPOINT,
                entity,
                EmbeddingResponse.class
            );

            EmbeddingResponse batchResponse = responseEntity.getBody();

            // 合并结果
            if (result == null) {
                result = batchResponse;
            } else {
                // 调整 index 并合并结果
                for (EmbeddingResponse.EmbeddingData embeddingData : batchResponse.getData()) {
                    embeddingData.setIndex(embeddingData.getIndex() + batchCounter);
                    result.getData().add(embeddingData);
                }
                // 合并 token 使用量
                if (result.getUsage() != null && batchResponse.getUsage() != null) {
                    result.getUsage().setPromptTokens(
                        result.getUsage().getPromptTokens() + batchResponse.getUsage().getPromptTokens()
                    );
                    result.getUsage().setTotalTokens(
                        result.getUsage().getTotalTokens() + batchResponse.getUsage().getTotalTokens()
                    );
                }
            }

            batchCounter += batch.size();
        }

        log.info("分批处理完成，总计处理 {} 条文本", result.getData().size());
        return result;
    }

    /**
     * 构建千问 Embedding 请求参数
     */
    private QwenEmbeddingRequest buildRequest(EmbeddingRequest request) {
        QwenEmbeddingRequest.QwenEmbeddingRequestBuilder builder = QwenEmbeddingRequest.builder()
            .input(request.getInputs())
            .model(request.getModel() != null ? request.getModel() : config.getEmbeddingModel());

        // 设置向量维度
        if (request.getDimensions() != null) {
            builder.dimensions(request.getDimensions());
        } else {
            builder.dimensions(config.getEmbeddingDimensions());
        }

        return builder.build();
    }

    /**
     * 记录请求日志
     */
    private void logRequest(EmbeddingRequest request, QwenEmbeddingRequest qwenRequest) {
        log.info("========== Embedding 调用开始 ==========");
        log.info("提供商: 通义千问");
        log.info("模型: {}", qwenRequest.getModel());
        log.info("文本数: {}", request.getInputs().size());

        if (qwenRequest.getDimensions() != null) {
            log.info("向量维度: {}", qwenRequest.getDimensions());
        }

        if (log.isDebugEnabled()) {
            for (int i = 0; i < request.getInputs().size(); i++) {
                String text = request.getInputs().get(i);
                String preview = text.length() > 50
                    ? text.substring(0, 50) + "..."
                    : text;
                log.debug("文本[{}]: {}", i, preview);
            }
        }
    }

    /**
     * 记录响应日志
     */
    private void logResponse(EmbeddingResponse response, long duration) {
        log.info("Embedding 调用成功，耗时: {}ms", duration);
        log.info("嵌入数量: {}", response.getData().size());

        if (!response.getData().isEmpty()) {
            log.info("向量维度: {}", response.getData().get(0).getEmbedding().size());
        }

        if (response.getUsage() != null) {
            log.info("Token 使用: input={}", response.getUsage().getPromptTokens());
        }

        log.info("========== Embedding 调用结束 ==========");
    }

    /**
     * 千问 Embedding 请求（内部类，用于字段映射）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class QwenEmbeddingRequest {
        /**
         * 输入文本（千问格式用input，不是inputs）
         */
        @JsonProperty("input")
        private List<String> input;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 向量维度
         */
        private Integer dimensions;
    }
}
