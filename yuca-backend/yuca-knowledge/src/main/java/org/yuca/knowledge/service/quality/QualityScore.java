package org.yuca.knowledge.service.quality;

import lombok.Builder;
import lombok.Data;

/**
 * 文档质量评分结果（#11：质量评分与智能路由）。
 *
 * <p>3 维特征 + 综合评分 + 三级 tier（Clean/Decent/Garbage）+ 评分原因（调试 / 日志用）。
 * 综合评分公式见 {@link DocumentQualityScorer}。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Data
@Builder
public class QualityScore {

    /** 综合评分 [0, 1]，越大越好 */
    private double overall;

    /** 三级分类：Clean / Decent / Garbage */
    private String tier;

    /** 非空白字符占比 [0, 1]，越高越好 */
    private double textRatio;

    /** OCR 瑕疵密度 [0, +∞)，越低越好（每字符的瑕疵数） */
    private double ocrNoiseDensity;

    /** 非空行的中位行长（字符），过长或过短都不健康 */
    private double avgLineLength;

    /** 评分原因（用于日志 / 调试），如"文本过短或为空" */
    private String reason;

    /** 常量 tier */
    public static final String TIER_CLEAN = "Clean";
    public static final String TIER_DECENT = "Decent";
    public static final String TIER_GARBAGE = "Garbage";

    /** 工厂：直接判 Garbage 的快捷构造（用于空文本 / 极短文本） */
    public static QualityScore garbage(String reason) {
        return QualityScore.builder()
                .overall(0.0)
                .tier(TIER_GARBAGE)
                .textRatio(0.0)
                .ocrNoiseDensity(1.0)
                .avgLineLength(0.0)
                .reason(reason)
                .build();
    }
}
