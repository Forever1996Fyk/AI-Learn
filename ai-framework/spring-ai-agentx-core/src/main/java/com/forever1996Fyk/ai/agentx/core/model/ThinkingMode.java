package com.forever1996Fyk.ai.agentx.core.model;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 15:57
 **/
public enum ThinkingMode {

    /**
     * 不处理思考内容（默认）。
     * <p>
     * 框架会自动剥离 content 中的 &lt;think/&gt; 标签，不输出 Thinking 事件。
     */
    DISABLED,

    /**
     * &lt;think/&gt; 标签格式。
     * <p>
     * 思考内容嵌入在 content 字段中，用 {@code <think...</think=>} 标签包裹。
     * 适用于：MiniMax M2.7。
     */
    THINK_TAG,

    /**
     * {@code reasoning_content} 字段格式。
     * <p>
     * 思考内容通过独立的 {@code reasoning_content} 字段返回，{@code content} 仅包含正式回答。
     * 框架通过 metadata 或消息子类的 {@code getReasoningContent()} 方法提取思考内容。
     * 适用于：DeepSeek、Qwen3.6-plus 等。
     */
    REASONING_CONTENT,

    ;
}
