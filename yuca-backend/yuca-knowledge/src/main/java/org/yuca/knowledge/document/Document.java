package org.yuca.knowledge.document;

/**
 * 文档包装类型，承载待切片的原始文本。
 * 等价替换 langchain4j 的 dev.langchain4j.data.document.Document。
 */
public record Document(String text) {

    public static Document from(String text) {
        return new Document(text);
    }
}
