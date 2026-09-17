package com.forever1996Fyk.ai.agentx.core.agent.internal;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * 流式轮次执行状态。
 * 从 AgentLoopExecutor 中提取为独立类，供 ThinkingModeProcessor 等协作类共享。
 * @author: YuKai Fan
 * @create: 2026/9/17 11:03
 **/
public class RoundState {
    RoundMode mode = RoundMode.TEXT;
    final List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
    final StringBuilder textBuffer = new StringBuilder();
    final StringBuilder reasoningBuffer = new StringBuilder();
    boolean inThink = false;
    long promptTokens = -1;
    long completionTokens = -1;
    String finishReason;
    Map<String, Object> advisorContext;
}
