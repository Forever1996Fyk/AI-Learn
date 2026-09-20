package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * 单个工具执行后事件（callback.call 返回后、ToolEnd 发射后）。
 *
 * <p>emitter 可通过 {@link #runtimeContext()} 获取，用于注入下游事件（如阶段输出）。
 *
 * @author: YuKai Fan
 * @create: 2026/9/18 10:52
 **/
public record AfterToolExecutionEvent(
        AgentRuntimeContext runtimeContext,
        String toolName,
        String toolCallId,
        String arguments,
        String result,
        boolean success,
        long durationMs
) implements HookEvent {
}
