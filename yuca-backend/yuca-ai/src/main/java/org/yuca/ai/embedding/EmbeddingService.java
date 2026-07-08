package org.yuca.ai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.provider.qwen.QwenEmbeddingModel;

import java.util.List;

/**
 * 嵌入向量服务
 * 使用 DashScope text-embedding-v3 模型生成向量（走 OpenAI 兼容端点）
 */
@Slf4j
public class EmbeddingService {

    private final QwenEmbeddingModel embeddingModel;
    private final int dimension;

    public EmbeddingService(AiProperties aiProperties) {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        AiProperties.EmbeddingConfig embeddingConfig = aiProperties.getEmbedding();

        this.embeddingModel = new QwenEmbeddingModel(
                dashscope.getBaseUrl(),
                dashscope.getApiKey(),
                embeddingConfig.getModelName());
        this.dimension = embeddingConfig.getDimension();

        log.info("EmbeddingService 初始化完成: model={}, dimension={}",
                embeddingConfig.getModelName(), dimension);
    }

    /**
     * 生成单个文本的嵌入向量
     */
    public float[] embed(String text) {
        return embeddingModel.embedBatch(List.of(text)).get(0);
    }

    /**
     * 批量生成嵌入向量
     */
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingModel.embedBatch(texts);
    }

    /**
     * 生成嵌入向量并转为 Double 数组（兼容 PGVectorTypeHandler）
     */
    public Double[] embedAsDoubleArray(String text) {
        return floatToDoubleArray(embed(text));
    }

    /**
     * 批量生成嵌入向量并转为 Double 数组列表
     */
    public List<Double[]> embedBatchAsDoubleArrays(List<String> texts) {
        return embedBatch(texts).stream()
                .map(this::floatToDoubleArray)
                .toList();
    }

    private Double[] floatToDoubleArray(float[] vector) {
        Double[] result = new Double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (double) vector[i];
        }
        return result;
    }
}
