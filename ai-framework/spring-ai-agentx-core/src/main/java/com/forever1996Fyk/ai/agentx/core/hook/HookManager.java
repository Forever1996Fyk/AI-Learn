package com.forever1996Fyk.ai.agentx.core.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/18 10:50
 **/
public class HookManager {
    private static final Logger log = LoggerFactory.getLogger(HookManager.class);

    private final List<AgentHook> hooks;
    private final boolean empty;

    public static final HookManager EMPTY = new HookManager(List.of());

    public HookManager(List<AgentHook> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            this.hooks = List.of();
            this.empty = true;
        } else {
            List<AgentHook> sorted = new ArrayList<>(hooks);
            sorted.sort(Comparator.comparingInt(AgentHook::priority).reversed());
            this.hooks = List.copyOf(sorted);
            this.empty = false;
        }
    }

    public boolean isEmpty() {
        return empty;
    }


    /**
     * 依次调用所有 Hook 处理事件，传播可修改事件的变更。
     *
     * @param event 原始事件
     * @return 处理后的事件（可能被 Hook 修改）
     */
    @SuppressWarnings("unchecked")
    public <T extends HookEvent> T fireEvent(T event) {
        if (empty) {
            return event;
        }
        HookEvent current = event;
        for (AgentHook hook : hooks) {
            try {
                HookEvent result = hook.onEvent(current);
                if (result != null) {
                    current = result;
                }
            } catch (Exception e) {
                log.error("Hook [{}] 执行失败: {}", hook.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        return (T) current;
    }
}
