package org.yuca.knowledge.document.parser;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Markdown / 纯文本解析器。
 *
 * <p>文本类格式本来就是 UTF-8 可读字符，直接解码即可。
 * 也作为未知扩展名的降级解析器，保持与重构前的行为一致。
 */
@Component
public class MarkdownTxtParser implements DocumentParser {

    @Override
    public String parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
