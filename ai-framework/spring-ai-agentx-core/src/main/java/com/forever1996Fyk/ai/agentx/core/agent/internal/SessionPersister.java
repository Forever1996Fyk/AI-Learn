package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forever1996Fyk.ai.agentx.core.interrupt.PauseStateStore;
import com.forever1996Fyk.ai.agentx.core.memory.store.ConversationStore;
import com.forever1996Fyk.ai.agentx.core.memory.store.SessionMessageStore;
import com.forever1996Fyk.ai.agentx.core.memory.util.MemoryPersistor;
import com.forever1996Fyk.ai.agentx.core.trace.TraceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: AI-Learn
 * @description: 会话/Trace/暂停状态的持久化入口，集中处理所有落库副作用。
 * AgentLoopExecutor 在 init、resume、终态分支中委托本类完成 DB 写入。
 * @author: YuKai Fan
 * @create: 2026/9/9 17:02
 **/
public class SessionPersister {

    private static final Logger log = LoggerFactory.getLogger(SessionPersister.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final boolean enableSession;
    private final boolean enableTrace;
    private final ConversationStore conversationStore;
    private final SessionMessageStore sessionMessageStore;
    private final TraceStore traceStore;
    private final PauseStateStore stateStore;
    private final MemoryPersistor memoryPersistor;


    public SessionPersister(boolean enableSession, boolean enableTrace, ConversationStore conversationStore, SessionMessageStore sessionMessageStore, TraceStore traceStore, PauseStateStore stateStore, MemoryPersistor memoryPersistor) {
        this.enableSession = enableSession;
        this.enableTrace = enableTrace;
        this.conversationStore = conversationStore;
        this.sessionMessageStore = sessionMessageStore;
        this.traceStore = traceStore;
        this.stateStore = stateStore;
        this.memoryPersistor = memoryPersistor;
    }
}
