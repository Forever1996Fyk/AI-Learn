package com.forever1996Fyk.ai.agent.agent.hitl;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/14 22:57
 **/
public record AgentInterrupted(List<PendingToolCall> pendingToolCalls,
                               List<Message> checkpointMessages,
                               Map<String, Object> context) implements AgentResult{
}
