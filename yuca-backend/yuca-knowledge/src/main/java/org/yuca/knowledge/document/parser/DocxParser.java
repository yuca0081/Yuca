package org.yuca.knowledge.document.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * DOCX 解析器（基于 Apache POI 5.2.5）。
 *
 * <p>按文档顺序遍历 {@code IBodyElement}（段落 + 表格），保留顺序信息便于后续章节树识别。
 * 表格转纯文本时单元格用 {@code " | "} 分隔，行用换行分隔。
 *
 * <p>仅支持 .docx（OOXML 格式）；老版 .doc（HWPF）不在本类范围。
 */
@Slf4j
@Component
public class DocxParser implements DocumentParser {

    private static final String CELL_SEPARATOR = " | ";
    private static final String LINE_SEPARATOR = "\n";

    @Override
    public String parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        try (org.apache.poi.xwpf.usermodel.XWPFDocument document =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(bytes))) {

            StringBuilder sb = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> appendParagraph((XWPFParagraph) element, sb);
                    case TABLE -> appendTable((XWPFTable) element, sb);
                    default -> { /* 跳过页眉页脚等非正文元素 */ }
                }
            }
            return sb.toString();
        }
    }

    private void appendParagraph(XWPFParagraph paragraph, StringBuilder sb) {
        String text = paragraph.getText();
        if (text != null && !text.isEmpty()) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append(LINE_SEPARATOR);
            }
            sb.append(text).append(LINE_SEPARATOR);
        }
    }

    private void appendTable(XWPFTable table, StringBuilder sb) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
            sb.append(LINE_SEPARATOR);
        }
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            List<XWPFTableCell> cells = row.getTableCells();
            for (int c = 0; c < cells.size(); c++) {
                if (c > 0) {
                    sb.append(CELL_SEPARATOR);
                }
                String text = cells.get(c).getText();
                if (text != null) {
                    sb.append(text.replace(LINE_SEPARATOR, " "));
                }
            }
            sb.append(LINE_SEPARATOR);
        }
    }
}
