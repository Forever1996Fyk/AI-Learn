package com.forever1996Fyk.ai.agentx.core.interrupt;

import com.forever1996Fyk.ai.agentx.core.model.PauseState;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 16:12
 **/
public interface PauseStateStore {

    /**
     * 保存（覆盖）指定 conversationId 的暂停状态。
     */
    void save(PauseState state);


    /**
     * 按 conversationId 查找暂停状态。
     *
     * @return 状态存在则返回，不存在返回 null
     */
    PauseState findByConversationId(String conversationId);

    /**
     * 检查是否存在未恢复的暂停状态。
     */
    boolean exists(String conversationId);

    /**
     * 删除指定 conversationId 的暂停状态。
     *
     * @return 实际删除返回 true，状态不存在返回 false（用于并发恢复 CAS）
     */
    boolean delete(String conversationId);

    /**
     * 清理已过期的暂停状态。
     *
     * <p>由应用层定时任务调度（如 Spring {@code @Scheduled}），框架自身不启动后台线程。
     * 实现可以选择不实现过期清理（直接空方法）。
     */
    int deleteExpired();
}
