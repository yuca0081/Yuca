package org.yuca.knowledge.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF 解析器（基于 Apache PDFBox 2.0.29）。
 *
 * <p>扫描件 / 图片型 PDF 无法提取文本，本解析器返回空字符串；
 * OCR 兜底属于后续 P2 待办，不在本类职责内。
 *
 * <p>加密 PDF：PDFBox 默认会抛 {@code IOException}，本类不主动解密，
 * 让上层 Service 翻译成 {@code BusinessException} 给出明确提示。
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    @Override
    public String parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        try (PDDocument document = PDDocument.load(bytes)) {
            if (document.isEncrypted()) {
                throw new IOException("PDF 文档已加密，拒绝解析");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text;
        }
    }
}
