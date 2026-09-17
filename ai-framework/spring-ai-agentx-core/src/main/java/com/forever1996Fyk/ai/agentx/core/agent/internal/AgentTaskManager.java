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

    /**
     * 注册中断快照回调。委托给当前 TaskInfo。
     */
    public void setInterruptHandler(String conversationId, Consumer<String> handler) {
        TaskInfo taskInfo = taskMap.get(conversationId);
        if (taskInfo != null) {
            taskInfo.setInterruptHandler(handler);
        }
    }

    public void setDisposable(String conversationId, Disposable disposable) {
        TaskInfo taskInfo = taskMap.get(conversationId);
        if (taskInfo != null) {
            taskInfo.setDisposable(disposable);
        } else {
            // task 已被 stopTask 移除（如客户端断连），直接 dispose 防止泄漏
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                log.debug("Task already removed, disposed orphaned subscription: {}", conversationId);
            }
        }
    }

    /**
     * 强制停止任务，丢弃所有运行时状态。
     */
    public boolean stopTask(String conversationId) {
        // 原子操作：remove 同时拿回并删除，防止 get 和 remove 之间新任务被误删
        TaskInfo taskInfo = taskMap.remove(conversationId);
        if (taskInfo == null) {
            log.warn("No running task for conversation: {}", conversationId);
            return false;
        }
        try {
            Disposable disposable = taskInfo.getDisposable();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                log.debug("Disposed underlying call for conversation: {}", conversationId);
            }

            var sink = taskInfo.getSink();
            if (sink != null) {
                sink.tryEmitComplete();
                log.debug("Completed stream output for conversation: {}", conversationId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to stop task for conversation: {}", conversationId, e);
            return false;
        }
    }

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

}
