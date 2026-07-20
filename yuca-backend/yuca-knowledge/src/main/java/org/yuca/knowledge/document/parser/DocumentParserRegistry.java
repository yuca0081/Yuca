package org.yuca.knowledge.document.parser;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档解析器注册表：按文件扩展名路由到对应 {@link DocumentParser}。
 *
 * <p>路由规则：
 * <ol>
 *   <li>已知扩展名（md / txt / pdf / docx）→ 对应解析器</li>
 *   <li>未知扩展名 → 降级到 markdownTxtParser（与重构前行为一致），记 WARN 日志</li>
 * </ol>
 */
@Slf4j
public class DocumentParserRegistry {

    private final Map<String, DocumentParser> parsers = new HashMap<>();
    private final DocumentParser fallback;

    public DocumentParserRegistry(DocumentParser fallback) {
        this.fallback = fallback;
    }

    /**
     * 注册某扩展名对应的解析器。扩展名统一转小写存储。
     */
    public void register(String extension, DocumentParser parser) {
        if (extension == null || extension.isBlank()) {
            return;
        }
        parsers.put(extension.toLowerCase(), parser);
    }

    /**
     * 按扩展名解析文档。
     *
     * @param extension 文件扩展名（不含点，大小写不敏感）
     * @param bytes     文件原始字节
     * @return 纯文本内容
     * @throws Exception 解析失败时透传
     */
    public String parse(String extension, byte[] bytes) throws Exception {
        DocumentParser parser = resolve(extension);
        return parser.parse(bytes);
    }

    private DocumentParser resolve(String extension) {
        if (extension == null || extension.isBlank()) {
            log.warn("文档扩展名为空，降级到 fallback 解析器");
            return fallback;
        }
        DocumentParser parser = parsers.get(extension.toLowerCase());
        if (parser == null) {
            log.warn("未注册的文档扩展名: {}，降级到 fallback 解析器", extension);
            return fallback;
        }
        return parser;
    }
}
