package com.forever1996Fyk.ai.agentx.core.trace;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/16 14:55
 **/
public class TraceManager {
    private final TraceStore traceStore;
    private final long sessionId;
    private final String conversationId;

    public TraceManager(TraceStore traceStore, long sessionId, String conversationId) {
        this.traceStore = traceStore;
        this.sessionId = sessionId;
        this.conversationId = conversationId;
    }

    public long getSessionId() {
        return sessionId;
    }

    /**
     * 记录一次 LLM 调用 trace。
     */
    public void trace(int round, String inputData, String outputData, String think,
                      int promptTokens, int completionTokens, long durationMs) {
        traceStore.save(sessionId, conversationId, round,
                inputData, outputData, think,
                promptTokens, completionTokens, durationMs, true, null);
    }
}
