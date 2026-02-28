package org.yuca.yuca.ai.client.qwen;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.yuca.yuca.ai.client.AIEmbeddingClient;
import org.yuca.yuca.ai.common.EmbeddingRequest;
import org.yuca.yuca.ai.model.EmbeddingResponse;
import org.yuca.yuca.ai.model.Usage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通义千问 Embedding Provider 实现
 *
 * <p>使用 DashScope SDK 提供文本向量化服务
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qwen.api-key")
public class QwenEmbeddingClient implements AIEmbeddingClient {

    private final TextEmbedding textEmbedding;
    private final QwenConfig config;

    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v4";
    private static final int MAX_BATCH_SIZE = 10;

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 如果输入数量超过最大批次大小，进行分批处理
            if (request.getInputs().size() > MAX_BATCH_SIZE) {
                return embedBatch(request);
            }

            // 1. 构建请求参数
            TextEmbeddingParam param = buildParam(request);

            // 2. 记录请求日志
            logRequest(request, param);

            // 3. 调用 API
            TextEmbeddingResult result = textEmbedding.call(param);

            // 4. 解析响应（传入模型名称）
            String model = param.getModel();
            EmbeddingResponse response = parseResponse(result, model);

            // 5. 记录响应日志
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("通义千问 Embedding 调用失败，耗时: {}ms", duration, e);
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
            TextEmbeddingParam param = buildParam(batchRequest);
            TextEmbeddingResult batchResult = null;
            try {
                batchResult = textEmbedding.call(param);
            } catch (NoApiKeyException e) {
                log.error("通义千问 Embedding 调用失败，批次: {}", batchCounter, e);
                throw new RuntimeException(e);
            }
            String batchModel = param.getModel();
            EmbeddingResponse batchResponse = parseResponse(batchResult, batchModel);

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
     * 构建通义千问 Embedding 请求参数
     */
    private TextEmbeddingParam buildParam(EmbeddingRequest request) {
        TextEmbeddingParam.TextEmbeddingParamBuilder builder = TextEmbeddingParam.builder()
                .apiKey(config.getApiKey())
                .model(request.getModel() != null ? request.getModel() : DEFAULT_EMBEDDING_MODEL)
                .dimension(request.getDimensions() != null ? request.getDimensions() : 1024)
                .texts(request.getInputs());

        // v3支持的维度：1024（默认）、768、512、256、128、64
        // v4支持的维度：2048、1536、1024（默认）、768、512、256、128、64
        // 注意：根据官方文档示例，使用 parameter() 方法设置维度
        if (request.getDimensions() != null) {
            builder.parameter("dimension", request.getDimensions());
        }
        return builder.build();
    }

    /**
     * 解析响应
     */
    private EmbeddingResponse parseResponse(TextEmbeddingResult result, String model) {
        if (result == null || result.getOutput() == null || result.getOutput().getEmbeddings() == null) {
            throw new RuntimeException("AI 返回空结果");
        }

        // 转换嵌入结果 - SDK 返回 List<Double>，直接转换为 Double[] 数组保持精度
        List<EmbeddingResponse.EmbeddingResult> results = result.getOutput().getEmbeddings().stream()
            .map(item -> {
                // SDK 返回 List<Double>，直接转换为 Double[] 数组
                List<Double> embeddingList = item.getEmbedding();
                Double[] embeddingArray = embeddingList.toArray(new Double[0]);

                return EmbeddingResponse.EmbeddingResult.builder()
                    .embedding(embeddingArray)
                    .index(item.getTextIndex())
                    .build();
            })
            .collect(Collectors.toList());

        // 转换 usage - 使用 getTotalTokens() 方法
        Usage usage = null;
        if (result.getUsage() != null) {
            usage = Usage.builder()
                .inputTokens(result.getUsage().getTotalTokens())
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
    private void logRequest(EmbeddingRequest request, TextEmbeddingParam param) {
        log.info("========== Embedding 调用开始 ==========");
        log.info("提供商: 通义千问");
        log.info("模型: {}", param.getModel());
        log.info("文本数: {}", request.getInputs().size());

        if (request.getDimensions() != null) {
            log.info("向量维度: {}", request.getDimensions());
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
