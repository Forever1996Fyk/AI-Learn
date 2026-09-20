package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * Agent 调用结束事件（终态标记后、持久化前）。
 *
 * <p>只读：用于持久化、长期记忆、清理等后处理。
 * messages/totalTokens 均可通过 {@link #runtimeContext()} 访问。
 *
 * @author: YuKai Fan
 * @create: 2026/9/18 10:54
 **/
public record AfterCallEvent(
        AgentRuntimeContext runtimeContext,
        String finalAnswer,
        long durationMs
) implements HookEvent {
}
