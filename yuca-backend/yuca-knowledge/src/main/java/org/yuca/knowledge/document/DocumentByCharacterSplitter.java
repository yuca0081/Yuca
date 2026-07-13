package org.yuca.knowledge.document;

import java.util.ArrayList;
import java.util.List;

/**
 * 按字符切片器。
 * <p>
 * 每段最多 {@code maxSize} 个字符，相邻段之间重叠 {@code overlap} 个字符。
 * 等价替换 langchain4j 的 dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter。
 */
public class DocumentByCharacterSplitter {

    private final int maxSize;
    private final int overlap;

    public DocumentByCharacterSplitter(int maxSize, int overlap) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize 必须为正数: " + maxSize);
        }
        if (overlap < 0 || overlap >= maxSize) {
            throw new IllegalArgumentException("overlap 必须在 [0, maxSize) 范围内: " + overlap);
        }
        this.maxSize = maxSize;
        this.overlap = overlap;
    }

    public List<String> split(Document document) {
        return split(document.text());
    }

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int length = text.length();
        int step = maxSize - overlap;
        int pos = 0;
        while (pos < length) {
            int end = Math.min(pos + maxSize, length);
            chunks.add(text.substring(pos, end));
            if (end >= length) {
                break;
            }
            pos += step;
        }
        return chunks;
    }
}
