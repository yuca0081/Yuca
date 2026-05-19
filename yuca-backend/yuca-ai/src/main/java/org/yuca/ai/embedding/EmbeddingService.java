package org.yuca.ai.embedding;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.config.AiProperties;

import java.util.List;

/**
 * 嵌入向量服务
 * 使用 DashScope text-embedding-v3 模型生成向量
 */
@Slf4j
public class EmbeddingService {

    private final QwenEmbeddingModel embeddingModel;

    public EmbeddingService(AiProperties aiProperties) {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        AiProperties.EmbeddingConfig embeddingConfig = aiProperties.getEmbedding();

        this.embeddingModel = QwenEmbeddingModel.builder()
                .modelName(embeddingConfig.getModelName())
                .apiKey(dashscope.getApiKey())
                .build();

        log.info("EmbeddingService 初始化完成: model={}, dimension={}",
                embeddingConfig.getModelName(), embeddingConfig.getDimension());
    }

    /**
     * 生成单个文本的嵌入向量
     */
    public float[] embed(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        return response.content().vector();
    }

    /**
     * 批量生成嵌入向量
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();
        Response<List<Embedding>> response = embeddingModel.embedAll(segments);
        return response.content().stream()
                .map(Embedding::vector)
                .toList();
    }

    /**
     * 生成嵌入向量并转为 Double 数组（兼容 PGVectorTypeHandler）
     */
    public Double[] embedAsDoubleArray(String text) {
        float[] vector = embed(text);
        return floatToDoubleArray(vector);
    }

    /**
     * 批量生成嵌入向量并转为 Double 数组列表
     */
    public List<Double[]> embedBatchAsDoubleArrays(List<String> texts) {
        List<float[]> vectors = embedBatch(texts);
        return vectors.stream()
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
