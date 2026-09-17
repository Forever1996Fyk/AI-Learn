package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.forever1996Fyk.ai.agentx.core.interrupt.PauseReason;
import com.forever1996Fyk.ai.agentx.core.interrupt.SafePoint;
import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.PauseState;
import com.forever1996Fyk.ai.agentx.core.model.PendingToolCall;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/17 09:18
 **/
public class InterruptContext {

    private static final Logger log = LoggerFactory.getLogger(InterruptContext.class);

    private final List<Message> messagesRef;
    private final Sinks.Many<AgentStreamEvent> sink;
    private final RunnableParams params;
    private final String query;

    private volatile SafePoint phase = SafePoint.INIT;

    /**
     * TOOL_EXECUTION 安全点处缓存的待执行工具调用
     */
    private final AtomicReference<List<AssistantMessage.ToolCall>> pendingToolCalls =
            new AtomicReference<>(List.of());

    /**
     * 当前轮文本缓冲区引用（中断时取部分输出并持久化到 agentx_session）
     */
    private volatile StringBuilder textBuffer;

    /**
     * 当前轮推理缓冲区引用（中断时取部分思考并持久化到 agentx_session）
     */
    private volatile StringBuilder reasoningBuffer;

    public InterruptContext(List<Message> messagesRef, Sinks.Many<AgentStreamEvent> sink, RunnableParams params, String query) {
        this.messagesRef = messagesRef;
        this.sink = sink;
        this.params = params;
        this.query = query;
    }


    /**
     * 用户中断时把当前轮已产出的部分正文/思考刷进消息列表，保证纯文本回复也能落库。
     * 仅 LLM_STREAMING 安全点需要：TOOL_EXECUTION 的 assistant(tool_calls) 已在 finishRound 写入。
     */
    public void flushPartialOutput(AgentRuntimeContext execContext) {
        if (phase != SafePoint.LLM_STREAMING) {
            return;
        }
        String text = getPartialText();
        String reasoning = getPartialReasoning();
        if (text.isEmpty() && reasoning.isEmpty()) {
            return;
        }
        AssistantMessage partial = AssistantMessage.builder()
                .content(text)
                .properties(Map.of("reasoningContent", reasoning))
                .build();
        messagesRef.add(partial);
        if (execContext != null) {
            execContext.appendOriginalMessage(partial);
        }
    }

    public String getPartialText() {
        return textBuffer != null ? textBuffer.toString() : "";
    }

    public String getPartialReasoning() {
        return reasoningBuffer != null ? reasoningBuffer.toString() : "";
    }

    /**
     * 构建暂停快照。messages 取当前 messages 列表快照（浅拷贝避免后续修改污染）。
     *
     * @param interruptMessage 中断说明消息
     * @param runtimeCtx       运行时上下文（提供 sessionId、token 累计）
     * @param currentRound     当前轮次（来自 AgentLoopExecutor 的 roundCounter）
     * @return PauseState 实例（已填充 reason、interruptPhase、pendingToolCalls 等）
     */
    public PauseState buildSnapshot(String interruptMessage, AgentRuntimeContext runtimeCtx, long currentRound) {
        List<Message> messagesSnapshot = new ArrayList<>(messagesRef);

        List<PendingToolCall> pending = new ArrayList<>();
        for (AssistantMessage.ToolCall tc : pendingToolCalls.get()) {
            pending.add(new PendingToolCall(tc.id(), tc.name(), tc.arguments()));
        }

        long sessionId = runtimeCtx != null ? runtimeCtx.getSessionId() : 0L;
        long promptTokens = runtimeCtx != null ? runtimeCtx.getTotalPromptTokens() : 0L;
        long completionTokens = runtimeCtx != null ? runtimeCtx.getTotalCompletionTokens() : 0L;

        return PauseState.builder()
                .messages(messagesSnapshot)
                .params(params)
                .query(query)
                .reason(PauseReason.USER_INTERRUPT)
                .safePoint(phase)
                .interruptMessage(interruptMessage)
                .interruptedAt(System.currentTimeMillis())
                .pendingToolCalls(pending)
                .currentRound((int) currentRound)
                .sessionId(sessionId)
                .totalPromptTokens(promptTokens)
                .totalCompletionTokens(completionTokens)
                .build();
    }


    /**
     * 发射 Paused 事件并完成 sink。被 {@link AgentTaskManager#interrupt} 注册的回调调用。
     */
    public void emitPausedAndComplete(PauseState state) {
        try {
            sink.tryEmitNext(new AgentStreamEvent.Paused(state));
        } catch (Exception e) {
            log.warn("[InterruptContext] Failed to emit Paused: {}", e.getMessage());
        }
        sink.tryEmitComplete();
    }

    /**
     * 进入 LLM_STREAMING 安全点（在调用 LLM 之前）。
     */
    public void enterLlmStreaming() {
        this.phase = SafePoint.LLM_STREAMING;
        this.pendingToolCalls.set(List.of());
    }

    /**
     * 设置当前轮的文本和推理缓冲区引用（每轮开始时由 scheduleRound 调用）。
     * 中断时从这些缓冲区读取部分输出并持久化到 agentx_session。
     */
    public void setRoundBuffers(StringBuilder text, StringBuilder reasoning) {
        this.textBuffer = text;
        this.reasoningBuffer = reasoning;
    }

    /**
     * 进入 TOOL_EXECUTION 安全点（在调用工具之前）。
     */
    public void enterToolExecution(List<AssistantMessage.ToolCall> toolCalls) {
        this.phase = SafePoint.TOOL_EXECUTION;
        this.pendingToolCalls.set(toolCalls == null ? List.of() : List.copyOf(toolCalls));
    }
}
