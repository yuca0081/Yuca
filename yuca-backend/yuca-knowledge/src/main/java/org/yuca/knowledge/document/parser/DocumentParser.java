package org.yuca.knowledge.document.parser;

/**
 * 文档解析器接口：把原始字节流转为纯文本，供后续切片流水线使用。
 *
 * <p>不同格式（md / txt / pdf / docx）各自实现，由 {@link DocumentParserRegistry} 按扩展名路由。
 */
@FunctionalInterface
public interface DocumentParser {

    /**
     * 解析二进制内容为纯文本。
     *
     * @param bytes 文件原始字节
     * @return 解析后的纯文本（永不返回 null，空内容返回空字符串）
     * @throws Exception 解析失败（IO 异常、格式损坏、加密文档等）
     */
    String parse(byte[] bytes) throws Exception;
}
