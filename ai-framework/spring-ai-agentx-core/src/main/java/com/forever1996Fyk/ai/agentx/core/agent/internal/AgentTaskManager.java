package com.forever1996Fyk.ai.agentx.core.agent.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 15:29
 **/
public class AgentTaskManager {
    private static final Logger log = LoggerFactory.getLogger(AgentTaskManager.class);

    private final Map<String, TaskInfo> taskMap = new ConcurrentHashMap<>();

    public static class TaskInfo {
        // 通配类型
        private final Sinks.Many<?> sink;
        private final long createTime;

        private volatile Disposable disposable;

        /**
         * 中断请求（仅 interrupt 路径设置，stopTask 不设置）
         */
        private volatile InterruptRequest interruptRequest;
        /**
         * 中断快照回调（由 AgentLoopExecutor 在每轮 scheduleRound 注册）
         */
        private volatile Consumer<String> interruptHandler;

        TaskInfo(Sinks.Many<?> sink) {
            this.sink = sink;
            this.createTime = System.currentTimeMillis();
        }


        public Sinks.Many<?> getSink() {
            return sink;
        }

        public Disposable getDisposable() {
            return disposable;
        }

        public void setDisposable(Disposable disposable) {
            this.disposable = disposable;
        }

        public long getCreateTime() {
            return createTime;
        }

        public InterruptRequest getInterruptRequest() {
            return interruptRequest;
        }

        /**
         * 注册中断快照回调。AgentLoopExecutor 在每轮开始时注册，
         * 捕获当前 messages / sink / execCtx 等上下文用于构建 PauseState。
         */
        public void setInterruptHandler(Consumer<String> handler) {
            this.interruptHandler = handler;
        }

    }

    /**
     * 中断请求记录。
     */
    public static class InterruptRequest {
        private final String message;
        private final long requestedAt;

        public InterruptRequest(String message, long requestedAt) {
            this.message = message;
            this.requestedAt = requestedAt;
        }

        public String getMessage() {
            return message;
        }

        public long getRequestedAt() {
            return requestedAt;
        }
    }

    public TaskInfo registerTask(String conversationId, Sinks.Many<?> sink) {
        TaskInfo newTask = new TaskInfo(sink);
        TaskInfo existing = taskMap.putIfAbsent(conversationId, newTask);
        if (existing != null) {
            log.warn("Task already exists for conversation: {}", conversationId);
            return null;
        }
        log.debug("Registered task for conversation: {}", conversationId);
        return newTask;
    }

}
