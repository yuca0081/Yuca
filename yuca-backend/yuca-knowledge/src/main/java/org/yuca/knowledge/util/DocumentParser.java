package org.yuca.knowledge.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.parser.Parser;
import dev.langchain4j.parser.TextParser;
import dev.langchain4j.document.parser.apache.pdf.ApachePdfBoxDocumentParser;
import dev.langchain4j.document.parser.apache.poi.ApachePoiDocumentParser;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析工具类
 * 基于 LangChain4j 框架，负责文档解析、切片和元数据提取
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
public class DocumentParser {

    // 默认切分参数
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int MIN_CHUNK_SIZE = 5;
    private static final int MAX_CHUNK_SIZE = 10000;

    // 文档解析器实例
    private static final ApachePdfBoxDocumentParser pdfParser = new ApachePdfBoxDocumentParser();
    private static final ApachePoiDocumentParser poiParser = new ApachePoiDocumentParser();
    private static final TextParser textParser = new TextParser();

    /**
     * 解析文档并切片
     *
     * @param fileBytes  文档字节数组
     * @param fileName   文件名
     * @param fileFormat 文件格式
     * @return 切片后的文档列表
     */
    public static List<Document> parseAndSplit(byte[] fileBytes, String fileName, String fileFormat) {
        try {
            // 根据文件格式选择解析策略
            List<Document> documents = switch (fileFormat.toLowerCase()) {
                case "md", "markdown" -> parseText(fileBytes, fileName);
                case "txt" -> parseText(fileBytes, fileName);
                case "pdf" -> parsePdf(fileBytes, fileName);
                case "docx", "doc" -> parseWord(fileBytes, fileName);
                default -> {
                    log.warn("不支持的文件格式: {}, 使用默认文本解析", fileFormat);
                    yield parseText(fileBytes, fileName);
                }
            };

            // 文本切片
            List<TextSegment> segments = splitDocuments(documents);

            // 将 TextSegment 转换为 Document 并添加元数据
            List<Document> chunks = new ArrayList<>();
            Map<String, Object> metadata = createMetadata(fileName, fileFormat, fileBytes.length);

            for (TextSegment segment : segments) {
                Document chunk = Document.from(segment);
                chunk.metadata().putAll(metadata);
                chunks.add(chunk);
            }

            log.info("文档解析成功: {} -> {} 个切片", fileName, chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("文档解析失败: {} - {}", fileName, e.getMessage(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析纯文本文档（包括 Markdown）
     */
    private static List<Document> parseText(byte[] fileBytes, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            Document document = textParser.parse(inputStream);
            log.debug("文本文档解析成功: {}", fileName);
            return List.of(document);
        } catch (IOException e) {
            log.error("文本解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本解析失败", e);
        }
    }

    /**
     * 解析PDF文档（使用 Apache PDFBox）
     */
    private static List<Document> parsePdf(byte[] fileBytes, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            Document document = pdfParser.parse(inputStream);
            log.debug("PDF文档解析成功: {}", fileName);
            return List.of(document);
        } catch (IOException e) {
            log.error("PDF解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF解析失败", e);
        }
    }

    /**
     * 解析Word文档（使用 Apache POI）
     */
    private static List<Document> parseWord(byte[] fileBytes, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            Document document = poiParser.parse(inputStream);
            log.debug("Word文档解析成功: {}", fileName);
            return List.of(document);
        } catch (IOException e) {
            log.error("Word解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("Word解析失败", e);
        }
    }

    /**
     * 切分文档
     * 使用 LangChain4j 的文档切分器
     */
    private static List<TextSegment> splitDocuments(List<Document> documents) {
        // 创建基于字符的文档切分器
        DocumentSplitter splitter = new DocumentSplitter() {
            @Override
            protected List<TextSegment> split(String text) {
                List<TextSegment> segments = new ArrayList<>();
                int length = text.length();

                for (int start = 0; start < length; start += (CHUNK_SIZE - CHUNK_OVERLAP)) {
                    int end = Math.min(start + CHUNK_SIZE, length);

                    // 确保最小分块大小
                    if (end - start < MIN_CHUNK_SIZE && start > 0) {
                        continue;
                    }

                    String segmentText = text.substring(start, end);
                    TextSegment segment = TextSegment.from(segmentText);
                    segments.add(segment);

                    if (end >= length) {
                        break;
                    }
                }

                return segments;
            }
        };

        // 切分所有文档
        List<TextSegment> allSegments = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = splitter.split(document);
            allSegments.addAll(segments);
        }

        return allSegments;
    }

    /**
     * 创建文档元数据
     */
    private static Map<String, Object> createMetadata(String fileName, String fileFormat, long fileSize) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file_name", fileName);
        metadata.put("file_format", fileFormat);
        metadata.put("file_size", fileSize);
        return metadata;
    }

    /**
     * 验证文件格式是否支持
     *
     * @param fileFormat 文件格式
     * @return 是否支持
     */
    public static boolean isFormatSupported(String fileFormat) {
        return switch (fileFormat.toLowerCase()) {
            case "md", "markdown", "txt", "pdf", "docx", "doc" -> true;
            default -> false;
        };
    }

    /**
     * 获取支持的文件格式列表
     *
     * @return 支持的格式列表
     */
    public static List<String> getSupportedFormats() {
        return List.of("md", "markdown", "txt", "pdf", "docx", "doc");
    }
}
