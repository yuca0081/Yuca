package org.yuca.knowledge.service.quality;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 文档质量评分器（#11：质量评分与智能路由）。
 *
 * <p>3 维特征 → 综合评分 → 三级 tier：
 * <ul>
 *   <li><b>textRatio</b>：非空白字符 / 总字符。空文本 / 乱码 PDF 该值低</li>
 *   <li><b>ocrNoiseDensity</b>：OCR 瑕疵数 / 总字符。瑕疵含：
 *     <ul>
 *       <li>U+FFFD（替换字符，编码错误标志）</li>
 *       <li>连续 5+ 同字符（OCR 重复输出）</li>
 *       <li>孤立控制字符（除 \t\r\n 外的 &lt; 0x20）</li>
 *     </ul>
 *   </li>
 *   <li><b>avgLineLength</b>：非空行长度的中位数。扫描件 PDF 往往行长极不规律</li>
 * </ul>
 *
 * <p>综合评分：{@code overall = textRatio*0.5 + (1-min(noise,1))*0.3 + clamp(lineLen/80)*0.2}
 *
 * <p>tier 阈值（硬编码，无训练数据）：
 * <ul>
 *   <li><b>Clean</b>：{@code overall>=0.85 AND textRatio>=0.7}</li>
 *   <li><b>Decent</b>：{@code overall>=0.5}</li>
 *   <li><b>Garbage</b>：其他</li>
 * </ul>
 *
 * <p>无状态、线程安全，可作为单例 Bean 注入。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Slf4j
@Component
public class DocumentQualityScorer {

    /** 极短文本阈值（字符）。低于此值直接判 Garbage，避免评分公式对小样本失真 */
    private static final int MIN_CONTENT_LEN = 10;
    /** OCR 重复字符判定阈值：连续相同字符数 ≥ 此值视为瑕疵 */
    private static final int REPEAT_THRESHOLD = 5;
    /** 行长归一化基准：80 字符为常见文档行长 */
    private static final double NOMINAL_LINE_LEN = 80.0;

    /** Clean tier：overall 下限 */
    private static final double CLEAN_OVERALL_THRESHOLD = 0.85;
    /** Clean tier：textRatio 下限 */
    private static final double CLEAN_TEXT_RATIO_THRESHOLD = 0.7;
    /** Decent tier：overall 下限 */
    private static final double DECENT_OVERALL_THRESHOLD = 0.5;

    /** 连续 5+ 同字符（含中文、英文、数字、标点）*/
    private static final Pattern REPEAT_PATTERN = Pattern.compile("(.)\\1{" + (REPEAT_THRESHOLD - 1) + ",}");

    /**
     * 评分。
     *
     * @param content    解析后的纯文本
     * @param fileFormat 文件扩展名（md/pdf/docx/txt），仅用于日志，不影响算法
     * @return 评分结果，永不返回 null
     */
    public QualityScore score(String content, String fileFormat) {
        if (content == null || content.strip().length() < MIN_CONTENT_LEN) {
            log.debug("[quality] 文本过短或为空: fileFormat={}, len={}",
                    fileFormat, content == null ? 0 : content.length());
            return QualityScore.garbage("文本过短或为空");
        }

        int total = content.length();
        long nonWhitespace = content.chars().filter(c -> !Character.isWhitespace(c)).count();
        double textRatio = (double) nonWhitespace / total;

        long noise = countOcrNoise(content);
        double noiseDensity = (double) noise / total;

        double medianLineLen = medianNonEmptyLineLength(content);
        double lineLenScore = clamp01(medianLineLen / NOMINAL_LINE_LEN);

        double overall = textRatio * 0.5
                + (1.0 - Math.min(noiseDensity, 1.0)) * 0.3
                + lineLenScore * 0.2;

        String tier;
        String reason;
        if (overall >= CLEAN_OVERALL_THRESHOLD && textRatio >= CLEAN_TEXT_RATIO_THRESHOLD) {
            tier = QualityScore.TIER_CLEAN;
            reason = "高质量文本";
        } else if (overall >= DECENT_OVERALL_THRESHOLD) {
            tier = QualityScore.TIER_DECENT;
            reason = "中等质量文本";
        } else {
            tier = QualityScore.TIER_GARBAGE;
            reason = "低质量文本（疑似扫描件或解析失败）";
        }

        log.debug("[quality] 评分完成: fileFormat={}, tier={}, overall={}, textRatio={}, noise={}, lineLen={}",
                fileFormat, tier, overall, textRatio, noiseDensity, medianLineLen);

        return QualityScore.builder()
                .overall(overall)
                .tier(tier)
                .textRatio(textRatio)
                .ocrNoiseDensity(noiseDensity)
                .avgLineLength(medianLineLen)
                .reason(reason)
                .build();
    }

    /**
     * 统计 OCR 瑕疵数：
     * <ul>
     *   <li>U+FFFD 替换字符数</li>
     *   <li>连续 {@link #REPEAT_THRESHOLD}+ 同字符段数（每段算 1 个瑕疵）</li>
     *   <li>孤立控制字符数（&lt; 0x20 且非 \t \r \n）</li>
     * </ul>
     */
    private long countOcrNoise(String content) {
        long replacement = content.chars().filter(c -> c == 0xFFFD).count();
        long controls = content.chars()
                .filter(c -> c < 0x20 && c != '\t' && c != '\r' && c != '\n')
                .count();
        long repeats = REPEAT_PATTERN.matcher(content).results().count();
        return replacement + controls + repeats;
    }

    /** 计算非空行长度的中位数 */
    private double medianNonEmptyLineLength(String content) {
        String[] lines = content.split("\\r?\\n");
        int[] lengths = Arrays.stream(lines)
                .mapToInt(String::length)
                .filter(len -> len > 0)
                .toArray();
        if (lengths.length == 0) {
            return 0.0;
        }
        Arrays.sort(lengths);
        int mid = lengths.length / 2;
        if (lengths.length % 2 == 0) {
            return (lengths[mid - 1] + lengths[mid]) / 2.0;
        }
        return lengths[mid];
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
