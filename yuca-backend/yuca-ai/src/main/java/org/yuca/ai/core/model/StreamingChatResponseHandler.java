package org.yuca.ai.core.model;

/**
 * 流式聊天响应处理器。
 * 等价于 langchain4j 的 dev.langchain4j.model.chat.response.StreamingChatResponseHandler。
 */
public interface StreamingChatResponseHandler {

    /** 收到一段增量响应（token 或文字片段） */
    void onPartialResponse(String partialResponse);

    /** 流式响应完成 */
    void onCompleteResponse(ChatResponse completeResponse);

    /** 发生错误 */
    void onError(Throwable error);
}
