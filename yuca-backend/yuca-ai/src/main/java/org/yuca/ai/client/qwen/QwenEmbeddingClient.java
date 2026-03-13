package org.yuca.ai.client.qwen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.yuca.ai.client.qwen.dto.QwenEmbeddingRequest;
import org.yuca.ai.client.qwen.dto.QwenEmbeddingResponse;
import org.yuca.ai.model.EmbeddingRequest;
import org.yuca.ai.client.AIEmbeddingClient;
import org.yuca.ai.model.EmbeddingResponse;
import org.yuca.ai.model.ChatResponse;

import java.util.List;
import java.util.stream.Collectors;

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

            ResponseEntity<QwenEmbeddingResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + EMBEDDINGS_ENDPOINT,
                entity,
                QwenEmbeddingResponse.class
            );

            // 4. 解析响应
            String model = qwenRequest.getModel();
            EmbeddingResponse response = parseResponse(responseEntity.getBody(), model);

            // 5. 记录响应日志
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

            ResponseEntity<QwenEmbeddingResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + EMBEDDINGS_ENDPOINT,
                entity,
                QwenEmbeddingResponse.class
            );

            String batchModel = qwenRequest.getModel();
            EmbeddingResponse batchResponse = parseResponse(responseEntity.getBody(), batchModel);

            // 合并结果
            if (result == null) {
                result = batchResponse;
            } else {
                // 调整 text_index 并合并结果
                for (EmbeddingResponse.EmbeddingResult embeddingResult : batchResponse.getResults()) {
                    embeddingResult.setIndex(embeddingResult.getIndex() + batchCounter);
                    result.getResults().add(embeddingResult);
                }
                // 合并 token 使用量
                if (result.getUsage() != null && batchResponse.getUsage() != null) {
                    result.getUsage().setInputTokens(
                        result.getUsage().getInputTokens() + batchResponse.getUsage().getInputTokens()
                    );
                }
            }

            batchCounter += batch.size();
        }

        log.info("分批处理完成，总计处理 {} 条文本", result.getResults().size());
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
     * 解析响应
     */
    private EmbeddingResponse parseResponse(QwenEmbeddingResponse qwenResponse, String model) {
        if (qwenResponse == null || qwenResponse.getData() == null || qwenResponse.getData().isEmpty()) {
            throw new RuntimeException("AI 返回空结果");
        }

        // 转换嵌入结果 - List<Double> 转换为 Double[] 数组保持精度
        List<EmbeddingResponse.EmbeddingResult> results = qwenResponse.getData().stream()
            .map(item -> {
                // SDK 返回 List<Double>，直接转换为 Double[] 数组
                List<Double> embeddingList = item.getEmbedding();
                Double[] embeddingArray = embeddingList.toArray(new Double[0]);

                return EmbeddingResponse.EmbeddingResult.builder()
                    .embedding(embeddingArray)
                    .index(item.getIndex())
                    .build();
            })
            .collect(Collectors.toList());

        // 转换 usage
        ChatResponse.Usage usage = null;
        if (qwenResponse.getUsage() != null) {
            usage = ChatResponse.Usage.builder()
                .inputTokens(qwenResponse.getUsage().getTotal_tokens())
                .outputTokens(0)
                .build();
        }

        return EmbeddingResponse.builder()
            .results(results)
            .model(model)
            .usage(usage)
            .build();
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
        log.info("嵌入数量: {}", response.getResults().size());

        if (!response.getResults().isEmpty()) {
            log.info("向量维度: {}", response.getResults().get(0).getEmbedding().length);
        }

        if (response.getUsage() != null) {
            log.info("Token 使用: input={}",
                response.getUsage().getInputTokens());
        }

        log.info("========== Embedding 调用结束 ==========");
    }
}
