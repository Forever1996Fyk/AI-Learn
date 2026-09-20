package com.forever1996Fyk.ai.agentx.core.hook;

import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/18 10:44
 **/
public final class BeforeToolExecutionEvent implements HookEvent {
    private final AgentRuntimeContext runtimeContext;
    private final String toolName;
    private final String toolCallId;
    private String arguments;
    private ToolContext toolContext;

    public BeforeToolExecutionEvent(AgentRuntimeContext runtimeContext, String toolName, String toolCallId,
                                    String arguments, ToolContext toolContext) {
        this.runtimeContext = runtimeContext;
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.arguments = arguments;
        this.toolContext = toolContext;
    }

    public AgentRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getArguments() {
        return arguments;
    }

    public ToolContext getToolContext() {
        return toolContext;
    }
}
