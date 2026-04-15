package org.yuca.ai.agent;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天请求上下文
 * 贯穿整个 Agent 调用链，各拦截器按需读写
 */
@Data
public class ChatContext {

    private String sessionId;
    private Long userId;
    private String skillName;

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
