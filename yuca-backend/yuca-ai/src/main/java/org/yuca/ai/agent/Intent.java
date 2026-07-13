package org.yuca.ai.agent;

/**
 * 用户意图分类。
 *
 * <p>由 {@link org.yuca.ai.agent.enhancer.IntentRecognitionEnhancer} 通过小模型 LLM 调用判定，
 * 写入 {@link ChatContext#getIntent()} 供下游增强器（目前仅 {@link org.yuca.ai.agent.enhancer.RagEnhancer}）
 * 做路由决策。
 *
 * <p>路由规则：RagEnhancer 仅在 {@link #QA} 或 {@link #UNKNOWN} 时执行；其他意图跳过 RAG 省 API 成本。
 * {@code null}（意图识别被禁用）等价于 UNKNOWN——走原路径。
 */
public enum Intent {
    /** 闲聊、问候、感谢 */
    CHITCHAT,
    /** 知识问答、概念解释、事实查询——触发 RAG */
    QA,
    /** 需要工具执行的任务（计算、查询等） */
    TASK,
    /** 内容创作、写作、翻译 */
    CREATION,
    /** LLM 调用失败或无法明确分类——保守默认，下游当作"无意图信号"处理 */
    UNKNOWN
}
