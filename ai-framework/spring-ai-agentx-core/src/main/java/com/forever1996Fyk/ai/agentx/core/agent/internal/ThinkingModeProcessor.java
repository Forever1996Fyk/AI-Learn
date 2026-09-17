package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.ThinkingMode;
import com.forever1996Fyk.ai.agentx.core.stage.ThinkTagParser;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description: ThinkingMode 处理器 — 封装思考模式的分支逻辑：
 * 流式 chunk 三模式（DISABLED / THINK_TAG / REASONING_CONTENT）处理、
 * reasoning_content 提取（metadata + 反射，含反射缓存）、think 标签解析与剥离。
 * @author: YuKai Fan
 * @create: 2026/9/17 11:57
 **/
public class ThinkingModeProcessor {
    private final ThinkingMode thinkingMode;

    private volatile Method cachedReasoningMethod;
    private volatile Class<?> cachedReasoningClass;

    public ThinkingModeProcessor(ThinkingMode thinkingMode) {
        this.thinkingMode = thinkingMode;
    }

    public void processStreamChunk(String text, RoundState state,
                                   Sinks.Many<AgentStreamEvent> sink) {
        if (thinkingMode == ThinkingMode.REASONING_CONTENT) {
            if (text != null && !text.isEmpty()) {
                state.textBuffer.append(text);
                sink.tryEmitNext(new AgentStreamEvent.Text(text));
            }
        } else if (thinkingMode == ThinkingMode.THINK_TAG) {
            processThinkTagChunk(text, state, sink, true);
        } else {
            processThinkTagChunk(text, state, sink, false);
        }
    }

    private void processThinkTagChunk(String text, RoundState state,
                                      Sinks.Many<AgentStreamEvent> sink,
                                      boolean emitThinkingEvents) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ThinkTagParser.ParseResult result = ThinkTagParser.parse(text, state.inThink);
        state.inThink = result.inThink();
        for (ThinkTagParser.Segment seg : result.segments()) {
            if (seg.thinking()) {
                if (emitThinkingEvents) {
                    sink.tryEmitNext(new AgentStreamEvent.Thinking(seg.content()));
                }
                state.reasoningBuffer.append(seg.content());
            } else {
                state.textBuffer.append(seg.content());
                sink.tryEmitNext(new AgentStreamEvent.Text(seg.content()));
            }
        }
    }

    public void accumulateReasoningContent(AssistantMessage msg, RoundState state) {
        if (thinkingMode != ThinkingMode.REASONING_CONTENT) {
            return;
        }
        String reasoning = extractReasoningContent(msg);
        if (reasoning != null && !reasoning.isEmpty()) {
            state.reasoningBuffer.append(reasoning);
        }
    }

    /**
     * 从 AssistantMessage 中提取 reasoning_content（metadata 优先，反射兜底）。
     */
    public String extractReasoningContent(AssistantMessage msg) {
        Map<String, Object> metadata = msg.getMetadata();
        Object rc = metadata.get("reasoningContent");
        if (rc == null) {
            rc = metadata.get("reasoning_content");
        }
        if (rc instanceof String s && !s.isEmpty()) {
            return s;
        }

        Class<?> msgClass = msg.getClass();
        try {
            if (cachedReasoningMethod == null || cachedReasoningClass != msgClass) {
                try {
                    cachedReasoningMethod = msgClass.getMethod("getReasoningContent");
                    cachedReasoningClass = msgClass;
                } catch (NoSuchMethodException e) {
                    cachedReasoningClass = null;
                    return null;
                }
            }
            if (cachedReasoningMethod != null && cachedReasoningClass != null) {
                Object result = cachedReasoningMethod.invoke(msg);
                if (result instanceof String s && !s.isEmpty()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public void processReasoningChunk(String reasoning, RoundState state, Sinks.Many<AgentStreamEvent> sink) {
        if (reasoning != null && !reasoning.isEmpty()) {
            state.reasoningBuffer.append(reasoning);
            sink.tryEmitNext(new AgentStreamEvent.Thinking(reasoning));
        }
    }

    public Map<String, Object> buildReasoningProperties(RoundState state) {
        return Map.of("reasoningContent", state.reasoningBuffer.isEmpty() ? "" : state.reasoningBuffer.toString());
    }

    public void processForceFinalChunk(String text, boolean[] inThinkHolder,
                                       StringBuilder answerBuffer,
                                       StringBuilder reasoningBuffer,
                                       Sinks.Many<AgentStreamEvent> sink) {
        if (thinkingMode == ThinkingMode.REASONING_CONTENT) {
            if (text != null && !text.isEmpty()) {
                answerBuffer.append(text);
                sink.tryEmitNext(new AgentStreamEvent.Text(text));
            }
        } else if (thinkingMode == ThinkingMode.THINK_TAG) {
            processForceFinalThinkTag(text, inThinkHolder, answerBuffer, reasoningBuffer, sink, true);
        } else {
            processForceFinalThinkTag(text, inThinkHolder, answerBuffer, reasoningBuffer, sink, false);
        }
    }

    private void processForceFinalThinkTag(String text, boolean[] inThinkHolder,
                                           StringBuilder answerBuffer,
                                           StringBuilder reasoningBuffer,
                                           Sinks.Many<AgentStreamEvent> sink,
                                           boolean emitThinkingEvents) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ThinkTagParser.ParseResult result = ThinkTagParser.parse(text, inThinkHolder[0]);
        inThinkHolder[0] = result.inThink();
        for (ThinkTagParser.Segment seg : result.segments()) {
            if (seg.thinking()) {
                if (emitThinkingEvents) {
                    sink.tryEmitNext(new AgentStreamEvent.Thinking(seg.content()));
                }
                reasoningBuffer.append(seg.content());
            } else {
                answerBuffer.append(seg.content());
                sink.tryEmitNext(new AgentStreamEvent.Text(seg.content()));
            }
        }
    }
}
