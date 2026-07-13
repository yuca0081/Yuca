package org.yuca.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yuca.knowledge.document.MarkdownChapterTreeBuilder;

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
}
