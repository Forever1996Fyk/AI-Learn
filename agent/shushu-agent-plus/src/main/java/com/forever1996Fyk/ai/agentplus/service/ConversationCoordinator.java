package com.forever1996Fyk.ai.agentplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/23 17:56
 **/
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "deploy.mode", havingValue = "cluster")
public class ConversationCoordinator implements MessageListener {

    @Override
    public void onMessage(Message message, @Nullable byte[] pattern) {

    }

    /**
     * 抢会话执行锁：抢到才允许发起执行，抢不到说明该会话在其他实例（或本实例）执行中。
     */
    public boolean tryLock(String conversationId) {
        return false;
    }

    /**
     * 释放会话执行锁（挂在执行流的 doFinally 上，执行终止才触发，HTTP 断开不影响）。
     */
    public void unlock(String conversationId) {
    }

    /**
     * 广播中断：各实例收到后各自尝试本地打断，持有该会话任务的那台真正执行，其余忽略。
     */
    public void broadcastInterrupt(String conversationId) {

    }
}
