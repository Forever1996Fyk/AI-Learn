package com.forever1996Fyk.ai.agent.advisor;

import com.forever1996Fyk.ai.agent.agent.hitl.HITLState;
import com.forever1996Fyk.ai.agent.agent.hitl.PendingToolCall;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/14 22:58
 **/
public class HITLAdvisor implements CallAdvisor {
    public static final String HITL_STATE_KEY = "hitl.state";
    public static final String HITL_REQUIRED = "hitl.required";
    public static final String HITL_PENDING_TOOLS = "hitl.pending.tools";
    public static final String HITL_NON_INTERCEPT_TOOLS = "hitl.non.intercept.tools";


    private final Set<String> interceptToolNames;

    public HITLAdvisor(Set<String> interceptToolNames) {
        this.interceptToolNames = interceptToolNames;
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        if (!response.chatResponse().hasToolCalls()) {
            return response;
        }

        // 从上下文中获取 HITLState，用于判断工具名称是否已被审批
        HITLState hitlState = (HITLState) request.context().get(HITL_STATE_KEY);

        // 需要中断的工具
        List<PendingToolCall> pending = new ArrayList<>();
        // 无需中断的工具
        List<AssistantMessage.ToolCall> nonInterrupted = new ArrayList<>();

        for (AssistantMessage.ToolCall tc : response.chatResponse().getResult().getOutput().getToolCalls()) {
            if (!interceptToolNames.contains(tc.name())) {
                nonInterrupted.add(tc);
                continue;
            }

            // 如果该工具名称在本次会话中已被人工审批过，则不再拦截，直接放行
            if (hitlState != null && hitlState.isToolNameApproved(tc.name())) {
                nonInterrupted.add(tc);
                continue;
            }

            pending.add(new PendingToolCall(tc.id(), tc.name(), tc.arguments(), null, "该工具需要用户手动确认"));
        }
        if (pending.isEmpty()) {
            return response;
        }

        response.context().put(HITL_REQUIRED, true);
        response.context().put(HITL_PENDING_TOOLS, pending);
        // 无需中断的工具，可以直接执行，所以需要传递到工具调用阶段
        if (!nonInterrupted.isEmpty()) {
            response.context().put(HITL_NON_INTERCEPT_TOOLS, nonInterrupted);
        }
        return response;
    }

    @Override
    public String getName() {
        return "HITLAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
