package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * 异常事件（LLM 或工具执行抛异常时）。
 *
 * <p>只读：用于错误告警、日志、清理。
 * @author: YuKai Fan
 * @create: 2026/9/18 10:49
 **/
public record ErrorEvent(AgentRuntimeContext runtimeContext,
                         Throwable error,
                         String phase,
                         int retryAttempt,
                         boolean willRetry) implements HookEvent {
}
