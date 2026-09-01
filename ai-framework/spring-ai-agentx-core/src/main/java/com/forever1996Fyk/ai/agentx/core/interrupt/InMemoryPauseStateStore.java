package com.forever1996Fyk.ai.agentx.core.interrupt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: AI-Learn
 * @description:
 * 基于内存的 {@link PauseStateStore} 实现。
 *
 * <p>用于单节点部署、单元测试和开发期验证。不具备跨进程持久化能力，
 * 进程重启后状态丢失。生产环境跨进程恢复请使用 {@link JdbcPauseStateStore}。
 *
 * <p>过期清理策略：以 {@link PauseState#getInterruptedAt()} 为基准，
 * 超过 {@link #defaultTtlMillis} 的状态视为过期。{@link PauseState#getInterruptedAt()} 为 0 时永不过期。
 *
 * @author: YuKai Fan
 * @create: 2026/9/1 08:39
 **/
public class InMemoryPauseStateStore implements PauseStateStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPauseStateStore.class);

}
