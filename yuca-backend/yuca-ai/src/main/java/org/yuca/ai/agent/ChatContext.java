package org.yuca.ai.agent;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 请求上下文
 * 贯穿整个 Agent 调用链，各增强器按需读写
 */
@Data
public class ChatContext {

    private String sessionId;
    private Long userId;

    /**
     * 用户意图，由 IntentRecognitionEnhancer 写入。
     * {@code null} 表示意图识别未启用（或该请求未经过该增强器），下游当作 UNKNOWN 处理。
     */
    private Intent intent;

    private final Map<String, Object> attributes = new HashMap<>();

    public ChatContext attribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T attribute(String key) {
        return (T) attributes.get(key);
    }
}
