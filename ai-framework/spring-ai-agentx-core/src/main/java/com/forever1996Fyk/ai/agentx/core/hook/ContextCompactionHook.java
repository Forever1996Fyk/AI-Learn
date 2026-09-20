package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.context.ContextCompactor;
import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/18 10:48
 **/
public record ContextCompactionHook(ContextCompactor compactor) implements AgentHook {

    @Override
    public HookEvent onEvent(HookEvent event) {
        if (event instanceof BeforeReasoningEvent e) {
            AgentRuntimeContext ctx = e.runtimeContext();
            compactor.compact(
                    ctx.getMessages(),
                    ctx.getQuery(),
                    ctx.getConversationId(),
                    ctx.getSessionId());
        }
        return event;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
