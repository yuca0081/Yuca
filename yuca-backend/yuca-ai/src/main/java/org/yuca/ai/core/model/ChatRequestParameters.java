package org.yuca.ai.core.model;

/**
 * 聊天请求参数的标记接口。
 * 具体提供商通过子接口/子 record 扩展（如 {@code org.yuca.ai.core.provider.qwen.QwenRequestParameters}
 * 携带 Qwen3 思考模式的 enableThinking 开关）。
 */
public interface ChatRequestParameters {
}
