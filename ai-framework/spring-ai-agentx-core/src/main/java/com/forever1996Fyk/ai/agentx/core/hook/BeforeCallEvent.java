package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * Agent 调用开始事件（首次推理前）。
 *
 * <p>query/params/messages/emitter 均可通过 {@link #runtimeContext()} } 访问。
 *
 * @author: YuKai Fan
 * @create: 2026/9/18 10:29
 **/
public record BeforeCallEvent(AgentRuntimeContext runtimeContext) implements HookEvent {
}
