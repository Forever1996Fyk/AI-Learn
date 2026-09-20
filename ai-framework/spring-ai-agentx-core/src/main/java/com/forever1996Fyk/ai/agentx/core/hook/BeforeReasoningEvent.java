package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/18 10:43
 **/
public record BeforeReasoningEvent(AgentRuntimeContext runtimeContext, long round) implements HookEvent {
}
