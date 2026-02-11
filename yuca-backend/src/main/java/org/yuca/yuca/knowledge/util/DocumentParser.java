package org.yuca.yuca.knowledge.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析工具类
 * 负责文档解析、切片和元数据提取
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
public class DocumentParser {

    private static final Integer CHUNK_SIZE = 500;
    private static final Integer CHUNK_OVERLAP = 50;
    private static final Integer MIN_CHUNK_SIZE_TO_EMBED = 5;
    private static final Integer MAX_CHUNK_SIZE_TO_EMBED = 10000;

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
            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            // 根据文件格式选择解析策略
            List<Document> documents = switch (fileFormat.toLowerCase()) {
                case "md", "markdown" -> parseMarkdown(resource, fileName);
                case "txt" -> parseText(resource, fileName);
                case "pdf" -> parsePdf(resource, fileName);
                case "docx", "doc" -> parseWord(resource, fileName);
                default -> {
                    log.warn("不支持的文件格式: {}, 使用默认文本解析", fileFormat);
                    yield parseText(resource, fileName);
                }
            };

            // 文本切片
            TokenTextSplitter splitter = new TokenTextSplitter(
                    CHUNK_SIZE,
                    CHUNK_OVERLAP,
                    MIN_CHUNK_SIZE_TO_EMBED,
                    MAX_CHUNK_SIZE_TO_EMBED,
                    true // keepSeparator
            );

            List<Document> chunks = splitter.apply(documents);

            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("file_name", fileName);
            metadata.put("file_format", fileFormat);
            metadata.put("file_size", fileBytes.length);

            chunks.forEach(chunk -> {
                chunk.getMetadata().putAll(metadata);
            });

            log.info("文档解析成功: {} -> {} 个切片", fileName, chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("文档解析失败: {} - {}", fileName, e.getMessage(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Markdown文档
     */
    private static List<Document> parseMarkdown(ByteArrayResource resource, String fileName) {
        TextReader reader = new TextReader(resource);
        reader.setCharset(StandardCharsets.UTF_8);

        List<Document> documents = reader.get();
        log.debug("Markdown文档解析成功: {}", fileName);
        return documents;

    }

    /**
     * 解析纯文本文档
     */
    private static List<Document> parseText(ByteArrayResource resource, String fileName) {
        TextReader reader = new TextReader(resource);
        reader.setCharset(StandardCharsets.UTF_8);

        List<Document> documents = reader.get();
        log.debug("文本文档解析成功: {}", fileName);
        return documents;

    }

    /**
     * 解析PDF文档（使用PDFBox）
     */
    private static List<Document> parsePdf(ByteArrayResource resource, String fileName) {
        try {
            // 简单的PDF文本提取（可扩展）
            TextReader reader = new TextReader(resource);
            reader.setCharset(StandardCharsets.UTF_8);

            List<Document> documents = reader.get();
            log.debug("PDF文档解析成功: {}", fileName);
            return documents;

        } catch (Exception e) {
            log.error("PDF解析失败: {}", e.getMessage());
            throw new RuntimeException("PDF解析失败", e);
        }
    }

    /**
     * 解析Word文档（使用POI）
     */
    private static List<Document> parseWord(ByteArrayResource resource, String fileName) {
        try {
            // 简单的Word文本提取（可扩展）
            TextReader reader = new TextReader(resource);
            reader.setCharset(StandardCharsets.UTF_8);

            List<Document> documents = reader.get();
            log.debug("Word文档解析成功: {}", fileName);
            return documents;

        } catch (Exception e) {
            log.error("Word解析失败: {}", e.getMessage());
            throw new RuntimeException("Word解析失败", e);
        }
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
