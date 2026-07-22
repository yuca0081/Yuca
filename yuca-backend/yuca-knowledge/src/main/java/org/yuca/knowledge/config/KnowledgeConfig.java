package org.yuca.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yuca.knowledge.document.MarkdownChapterTreeBuilder;
import org.yuca.knowledge.document.parser.DocumentParserRegistry;
import org.yuca.knowledge.document.parser.MarkdownTxtParser;
import org.yuca.knowledge.document.splitter.DocumentByParagraphSplitter;

/**
 * 知识库模块配置。
 *
 * <p>注册知识库专用组件。章节树构造器是知识库切片算法，跟知识库业务强内聚，
 * 故 bean 注册放在本模块而非通用 AI 配置。
 */
@Configuration
public class KnowledgeConfig {

    /**
     * Markdown 章节树构造器。无状态、线程安全，单例即可。
     */
    @Bean
    public MarkdownChapterTreeBuilder markdownChapterTreeBuilder() {
        return new MarkdownChapterTreeBuilder();
    }

    /**
     * 段落切片器（#11：Clean-非md / Decent 路径用）。无状态，单例。
     */
    @Bean
    public DocumentByParagraphSplitter documentByParagraphSplitter() {
        return new DocumentByParagraphSplitter();
    }

    /**
     * 文档解析器注册表：按扩展名路由到 md / txt / pdf / docx 等解析器。
     *
     * <p>MarkdownTxtParser / PdfParser / DocxParser 通过 {@code @Component} 自动注册，
     * 这里把它们按扩展名装配到 registry，未知扩展名降级到 MarkdownTxtParser。
     */
    @Bean
    public DocumentParserRegistry documentParserRegistry(
            MarkdownTxtParser markdownTxtParser,
            org.yuca.knowledge.document.parser.PdfParser pdfParser,
            org.yuca.knowledge.document.parser.DocxParser docxParser) {
        DocumentParserRegistry registry = new DocumentParserRegistry(markdownTxtParser);
        registry.register("md", markdownTxtParser);
        registry.register("txt", markdownTxtParser);
        registry.register("pdf", pdfParser);
        registry.register("docx", docxParser);
        return registry;
    }
}
