package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * 单轮推理结束事件（LLM stream 完成后）。
 *
 * <p>只读：携带本轮 LLM 输出（文本、工具调用决策、token 用量）。
 *
 * @author: YuKai Fan
 * @create: 2026/9/18 10:53
 **/
public record AfterReasoningEvent(
        AgentRuntimeContext runtimeContext,
        String text,
        List<AssistantMessage.ToolCall> toolCalls,
        long round,
        long promptTokens,
        long completionTokens,
        long durationMs
) implements HookEvent {
}
