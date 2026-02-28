package org.yuca.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.yuca.ai.client.AIEmbeddingClient;

import java.util.List;

/**
 * 向量嵌入服务
 * 负责文本向量化、批量处理和相似度计算
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AIEmbeddingClient embeddingClient;

    /**
     * 为单个文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量嵌入数组（Double类型保持精度）
     */
    public Double[] embed(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("文本内容不能为空");
            }

            Double[] embeddings = embeddingClient.embed(text);

            log.debug("文本嵌入生成成功，文本长度: {}, 向量维度: {}", text.length(), embeddings.length);
            return embeddings;

        } catch (Exception e) {
            log.error("文本嵌入生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("嵌入生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量生成向量嵌入
     *
     * @param texts 文本列表
     * @return 向量嵌入列表（Double类型保持精度）
     */
    public List<Double[]> batchEmbed(List<String> texts) {
        try {
            if (texts == null || texts.isEmpty()) {
                throw new IllegalArgumentException("文本列表不能为空");
            }

            List<Double[]> embeddings = embeddingClient.batchEmbed(texts);

            log.info("批量嵌入生成成功，文本数量: {}", texts.size());
            return embeddings;

        } catch (Exception e) {
            log.error("批量嵌入生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量嵌入生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度得分（0-1之间，1表示完全相同）
     */
    public Double cosineSimilarity(Double[] vec1, Double[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("向量不能为null");
        }

        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + vec1.length + " vs " + vec2.length);
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        double magnitude = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (magnitude == 0) {
            return 0.0;
        }

        return dotProduct / magnitude;
    }

    /**
     * 将向量数组转换为pgvector格式字符串
     *
     * @param embedding 向量数组（Double类型）
     * @return pgvector格式字符串
     */
    public String formatToPgVector(Double[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("向量数组不能为空");
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");

        return sb.toString();
    }
}
