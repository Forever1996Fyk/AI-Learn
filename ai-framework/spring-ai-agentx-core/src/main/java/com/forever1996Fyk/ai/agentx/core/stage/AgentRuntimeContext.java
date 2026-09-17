package com.forever1996Fyk.ai.agentx.core.stage;

import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import com.forever1996Fyk.ai.agentx.core.trace.TraceManager;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/9 16:58
 **/
public class AgentRuntimeContext {

    private final String query;
    private final RunnableParams params;

    private TraceManager traceManager;
    private long sessionId;

    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);
    private final AtomicInteger totalRounds = new AtomicInteger(0);

    private volatile int newMsgStartIndex;
    private volatile List<Message> originalMessagesSnapshot;
    private final AtomicReference<String> terminalStatus = new AtomicReference<>(null);

    /**
     * 终态落库幂等标志。CAS true 一次后，后续 persistOnTerminal 调用直接返回，
     * 避免流式 doFinally 与显式调用双写。
     */
    private final AtomicBoolean persistedFlag = new AtomicBoolean(false);


    /**
     * ReAct 循环的工作消息列表（可变，供 Hook 读取/修改）。
     */
    private List<Message> messages;

    /**
     * 下游事件发射器，Hook 可通过它注入 AgentStreamEvent。
     */
    private Consumer<AgentStreamEvent> emitter;

    public AgentRuntimeContext(String query, RunnableParams params) {
        this.query = query;
        this.params = params;
    }

    public void appendOriginalMessage(Message message) {
        if (message == null || originalMessagesSnapshot == null) {
            return;
        }
        originalMessagesSnapshot.add(message);
    }

    public String getQuery() {
        return query;
    }

    public RunnableParams getParams() {
        return params;
    }

    public void setNewMsgStartIndex(int newMsgStartIndex) {
        this.newMsgStartIndex = newMsgStartIndex;
    }

    public void setOriginalMessagesSnapshot(List<Message> originalMessagesSnapshot) {
        this.originalMessagesSnapshot = originalMessagesSnapshot;
    }

    public int getNewMsgStartIndex() {
        return newMsgStartIndex;
    }

    public List<Message> getOriginalMessagesSnapshot() {
        return originalMessagesSnapshot;
    }


    public String getTerminalStatus() {
        return terminalStatus.get();
    }


    public AtomicBoolean getPersistedFlag() {
        return persistedFlag;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Consumer<AgentStreamEvent> getEmitter() {
        return emitter;
    }

    public void setEmitter(Consumer<AgentStreamEvent> emitter) {
        this.emitter = emitter;
    }

    public TraceManager getTraceManager() {
        return traceManager;
    }

    public void setTraceManager(TraceManager traceManager) {
        this.traceManager = traceManager;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getTotalPromptTokens() {
        return totalPromptTokens.get();
    }

    public long getTotalCompletionTokens() {
        return totalCompletionTokens.get();
    }

    /**
     * 从暂停恢复时还原轮次。
     */
    public void setTotalRounds(int rounds) {
        totalRounds.set(rounds);
    }

    public int getTotalRounds() {
        return totalRounds.get();
    }

    /**
     * CAS 标记终态：completed / interrupted / error。只允许设置一次。
     */
    public boolean markTerminal(String status) {
        return terminalStatus.compareAndSet(null, status);
    }

    /**
     * 便捷方法：从 params 派生 conversationId。
     */
    public String getConversationId() {
        return params != null ? params.getConversationId() : null;
    }

    /**
     * CAS 标记已落库。返回 true 表示本次调用抢到落库权，false 表示已被其他路径落库。
     * 流式路径在 sink.tryEmitComplete() 前显式调用，避免 doFinally 时序竞态。
     */
    public boolean tryMarkPersisted() {
        return persistedFlag.compareAndSet(false, true);
    }

    /**
     * 累加本轮 token 用量。
     */
    public void accumulateTokens(long promptTokens, long completionTokens) {
        if (promptTokens > 0) {
            this.totalPromptTokens.addAndGet(promptTokens);
        }
        if (completionTokens > 0) {
            this.totalCompletionTokens.addAndGet(completionTokens);
        }
    }

    public void appendOriginalMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty() || originalMessagesSnapshot == null) {
            return;
        }
        originalMessagesSnapshot.addAll(messages);
    }

}
