package org.yuca.yuca;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yuca.yuca.ai.client.AIEmbeddingClient;
import org.yuca.yuca.ai.common.EmbeddingRequest;
import org.yuca.yuca.ai.model.EmbeddingResponse;

import java.util.List;

/**
 * QwenEmbeddingClient 测试类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@SpringBootTest
class QwenEmbeddingClientTest {

    @Autowired
    private AIEmbeddingClient embeddingClient;

    /**
     * 测试 1：单个文本嵌入
     */
    @Test
    public void testSingleTextEmbedding() {
        log.info("========== 测试 1：单个文本嵌入 ==========");

        EmbeddingRequest request = EmbeddingRequest.builder()
                .dimensions(1024)
                .inputs(List.of("依据《中关村科学城劳模先进、工会主席慰问管理办法》（中科工〔2025〕8号）规定，并参照《海淀区总工会关于做好2026年劳模春节慰问金发放工作的通知》要求，拟为全国劳动模范、北京市劳动模范等荣誉称号先进个人发放慰问金，具体方案如下：\n" +
                        "1．发放范围：纳入科学城总工会管理且所在企业规范缴纳工会经费的在职先进个人，包括全国劳动模范、北京市劳动模范、全国五一劳动奖章获得者、首都劳动奖章获得者、北京市先进工作者、享受北京市劳动模范待遇的先进个人、中关村科学城创新工匠。\n" +
                        "2．发放标准：不超过当年海淀区总工会关于全国和市级劳动模范春节慰问金发放标准。今年海淀区总工会发放全国劳模、市级劳模每人1000元，因此建议本年科学城发放每人1000元。\n" +
                        "3．人员统计：大国工匠1人，全国劳动模范3人，北京市劳动模范46人（含享受北京市劳模待遇2人），全国五一劳动奖章5人，首都劳动奖章7人，科学城创新工匠6人，共计68人，总金额68,000元 。\n" +
                        "现提请办公会审议。",
                        "你好世界",
                        "仅消耗平台赠送的免费额度，免费额度用尽后平台将自动停止服务（返回403错误：AllocationQuota.FreeTierOnly.），避免产生免费额度以外的费用。若您希望在免费额度用尽后仍继续使用模型服务，并根据实际产生的 用量付费，请您保持本功能是关闭状态。"))
                .build();

        EmbeddingResponse response = embeddingClient.embed(request);

        log.info("模型: {}", response.getModel());
        log.info("嵌入数量: {}", response.getResults().size());
        log.info("向量维度: {}", response.getResults().get(0).getEmbedding().length);
        log.info("Token 使用: {}", response.getUsage().getInputTokens());
    }

    /**
     * 测试 2：多个文本嵌入
     */
    @Test
    public void testMultipleTextEmbedding() {
        log.info("========== 测试 2：多个文本嵌入 ==========");

        EmbeddingRequest request = EmbeddingRequest.builder()
            .inputs(List.of(
                "Spring Boot 是一个优秀的 Java 框架",
                "Vue 3 是一个现代化的前端框架"
            ))
            .build();

        EmbeddingResponse response = embeddingClient.embed(request);

        log.info("模型: {}", response.getModel());
        log.info("嵌入数量: {}", response.getResults().size());
        log.info("Token 使用: {}", response.getUsage().getInputTokens());

        response.getResults().forEach(result ->
            log.info("文本[{}] 向量维度: {}", result.getIndex(), result.getEmbedding().length)
        );
    }
}
