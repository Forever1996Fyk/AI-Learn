package com.forever1996Fyk.ai.agentx.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.w3c.dom.Text;

/**
 * @program: AI-Learn
 * @description: Agent 流式事件。
 * <p>
 * 使用 Jackson 多态序列化，每个事件自动携带 {@code "type"} 字段用于类型鉴别。
 * 例如：{@code {"type":"Text","content":"你好"}}
 *
 * <p>子 Agent 事件携带 {@link SubAgentSource} 标识，主 Agent 事件不携带（source 为 null，
 * Jackson 不序列化 null 字段，向前兼容）。
 * <p>
 * 可扩展的 sealed 接口，支持以下事件类型：
 * <ul>
 *   <li>{@link Thinking} - LLM 思考过程（think 标签内）</li>
 *   <li>{@link Text} - LLM 正常文本输出</li>
 *   <li>{@link ToolStart} - 工具即将执行</li>
 *   <li>{@link ToolEnd} - 工具执行完成</li>
 *   <li>{@link Paused} - 执行暂停，等待外部输入（含 HITL 与 USER_INTERRUPT 两种原因）</li>
 *   <li>{@link Error} - LLM 调用异常（重试时发出）</li>
 *   <li>{@link Complete} - Agent 执行完成</li>
 * </ul>
 * @author: YuKai Fan
 * @create: 2026/9/9 15:58
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AgentStreamEvent.Thinking.class, name = "Thinking"),
})
public sealed interface AgentStreamEvent permits
        AgentStreamEvent.Thinking {

    /**
     * LLM 思考过程（&lt;think/&gt; 标签内的内容）。
     *
     * @param content 思考内容
     * @param source  事件来源（null 表示主 Agent）
     */
    record Thinking(String content, SubAgentSource source) implements AgentStreamEvent {
        public Thinking(String content) {
            this(content, null);
        }
    }
}
