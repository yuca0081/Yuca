package org.yuca.ai.core.provider.qwen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.yuca.ai.core.provider.openai.dto.EmbeddingRequest;
import org.yuca.ai.core.provider.openai.dto.EmbeddingResponse;

import java.util.Comparator;
import java.util.List;

/**
 * DashScope / Qwen 嵌入模型。
 * 走 OpenAI 兼容端点 POST /compatible-mode/v1/embeddings。
 */
@Slf4j
public class QwenEmbeddingModel {

    private final RestClient restClient;
    private final String modelName;

    public QwenEmbeddingModel(String baseUrl, String apiKey, String modelName) {
        this.modelName = modelName;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 批量生成嵌入向量（按输入顺序返回）。
     */
    public List<float[]> embedBatch(List<String> texts) {
        EmbeddingRequest req = new EmbeddingRequest(modelName, texts);
        EmbeddingResponse resp = restClient.post()
                .uri("/embeddings")
                .body(req)
                .retrieve()
                .body(EmbeddingResponse.class);
        if (resp == null || resp.data() == null) {
            throw new RuntimeException("Embedding 响应为空");
        }
        return resp.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingResponse.Item::index))
                .map(item -> toFloatArray(item.embedding()))
                .toList();
    }

    private float[] toFloatArray(List<Double> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).floatValue();
        }
        return arr;
    }
}
